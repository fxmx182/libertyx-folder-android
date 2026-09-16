package com.mauricio.arquivos.smb

import android.util.Log
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.mserref.NtStatus
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2CreateOptions
import com.hierynomus.mssmb2.SMB2Dialect
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.mssmb2.SMBApiException
import com.hierynomus.security.bc.BCSecurityProvider
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.SmbConfig
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.auth.NtlmAuthenticator
import com.hierynomus.smbj.session.Session
import com.hierynomus.smbj.share.DiskShare
import com.hierynomus.smbj.share.File
import com.mauricio.arquivos.App
import com.mauricio.arquivos.fs.FileEntry
import com.mauricio.arquivos.fs.Fs
import com.mauricio.arquivos.fs.Loc
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible
import java.io.FilterInputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.EnumSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * SMB 2/3 pelo smbj, com a sessão de cada servidor reaproveitada.
 *
 * Abrir conexão e autenticar custa centenas de milissegundos; refazer isso a
 * cada pasta aberta deixaria a navegação arrastada. A sessão fica guardada e,
 * se o servidor a derrubar (tempo ocioso, Wi-Fi que caiu), a primeira operação
 * que falhar por isso descarta e tenta de novo uma vez.
 */
object SmbFs : Fs {

    private const val TAG = "Arquivos"

    private fun config() = SmbConfig.builder()
        // MD4 do NTLM não existe na criptografia do Android.
        .withSecurityProvider(BCSecurityProvider())
        // Kerberos puxaria `javax` que o Android não tem.
        .withAuthenticators(NtlmAuthenticator.Factory())
        .withDfsEnabled(false)
        .withTimeout(20, TimeUnit.SECONDS)
        .withSoTimeout(30, TimeUnit.SECONDS)
        .withReadBufferSize(1 shl 20)
        .withWriteBufferSize(1 shl 20)
        .withTransactBufferSize(1 shl 20)

    private val cliente by lazy { SMBClient(config().build()) }

    /**
     * Convidado sem SMB 3: o smbj deriva chaves da senha no SMB 3 e sessão de
     * convidado não tem, então cai num NullPointerException (visto no LibertyX).
     */
    private val clienteConvidado by lazy {
        SMBClient(
            config().withDialects(SMB2Dialect.SMB_2_1, SMB2Dialect.SMB_2_0_2)
                // Sessão de convidado não tem chave: o smbj assina mesmo assim, o
                // Samba recusa ("Bad SMB2 signature") e a conexão morre por tempo.
                // Visto no teste com dperson/samba.
                .withSigningEnabled(false)
                .withTimeout(8, TimeUnit.SECONDS)
                .build()
        )
    }

    private val sessoes = ConcurrentHashMap<String, Session>()
    private val shares = ConcurrentHashMap<String, DiskShare>()

    fun forget(serverId: String) {
        shares.keys.filter { it.startsWith("$serverId/") }.forEach { k -> shares.remove(k)?.let { runCatching { it.close() } } }
        sessoes.remove(serverId)?.let { s -> runCatching { s.connection.close(true) } }
    }

    private fun server(id: String): SmbServer =
        SmbServerStore.get(App.instance).byId(id) ?: throw IOException("Servidor removido")

    /** Conecta e autentica — usado também pelo "testar" do cadastro. */
    fun openSession(server: SmbServer, password: String): Session {
        val usuario = server.username.trim()
        if (!server.isGuest && usuario.isNotEmpty()) {
            val (dominio, nome) = if ('\\' in usuario) usuario.substringBefore('\\') to usuario.substringAfter('\\')
                else server.domain to usuario
            val conn = cliente.connect(server.host, server.port)
            return try {
                conn.authenticate(AuthenticationContext(nome, password.toCharArray(), dominio))
            } catch (e: Exception) {
                runCatching { conn.close(true) }; throw e
            }
        }
        // Há Samba que só aceita "guest" e há o que só aceita anônimo.
        var erro: Exception? = null
        for ((cli, ctx) in listOf(
            clienteConvidado to AuthenticationContext.guest(),
            clienteConvidado to AuthenticationContext.anonymous(),
            cliente to AuthenticationContext.guest(),
        )) {
            val conn = try { cli.connect(server.host, server.port) } catch (e: Exception) { throw e }
            try {
                return conn.authenticate(ctx)
            } catch (e: Exception) {
                erro = e; runCatching { conn.close(true) }
            }
        }
        throw erro ?: IOException("Falha de autenticação")
    }

    private fun session(serverId: String): Session {
        sessoes[serverId]?.let { if (it.connection.isConnected) return it }
        val s = server(serverId)
        val nova = openSession(s, SmbServerStore.get(App.instance).password(s))
        sessoes[serverId] = nova
        return nova
    }

    private fun disk(loc: Loc.Smb): DiskShare {
        require(loc.share.isNotEmpty())
        val key = "${loc.serverId}/${loc.share.lowercase()}"
        shares[key]?.let { if (it.isConnected) return it }
        val d = session(loc.serverId).connectShare(loc.share) as? DiskShare
            ?: throw IOException("${loc.share} não é uma pasta compartilhada")
        shares[key] = d
        return d
    }

    /** Tenta; se a conexão tinha caído, refaz a sessão e tenta mais uma vez. */
    private fun <T> comRetentativa(serverId: String, bloco: () -> T): T = try {
        bloco()
    } catch (e: SMBApiException) {
        throw traduzir(e)
    } catch (e: Exception) {
        if (e is InterruptedException) throw e
        Log.w(TAG, "SMB falhou, reconectando: $e")
        forget(serverId)
        try { bloco() } catch (e2: SMBApiException) { throw traduzir(e2) }
    }

    private fun traduzir(e: SMBApiException): IOException = IOException(
        when (e.status) {
            NtStatus.STATUS_LOGON_FAILURE -> "Usuário ou senha incorretos"
            NtStatus.STATUS_ACCESS_DENIED -> "Acesso negado"
            NtStatus.STATUS_BAD_NETWORK_NAME -> "Compartilhamento não encontrado"
            NtStatus.STATUS_OBJECT_NAME_NOT_FOUND, NtStatus.STATUS_OBJECT_PATH_NOT_FOUND -> "Arquivo não encontrado"
            NtStatus.STATUS_OBJECT_NAME_COLLISION -> "Já existe um item com esse nome"
            NtStatus.STATUS_SHARING_VIOLATION -> "O arquivo está em uso no servidor"
            NtStatus.STATUS_DIRECTORY_NOT_EMPTY -> "A pasta não está vazia"
            NtStatus.STATUS_DISK_FULL -> "Disco do servidor cheio"
            else -> "Erro do servidor: ${e.status}"
        }, e
    )

    // ---------------------------------------------------------------- listagem

    private suspend fun <T> io(serverId: String, bloco: () -> T): T =
        runInterruptible(Dispatchers.IO) { comRetentativa(serverId, bloco) }

    override suspend fun list(dir: Loc): List<FileEntry> {
        val d = dir as Loc.Smb
        return io(d.serverId) {
            if (d.isServerRoot) listarShares(d) else
                disk(d).list(d.smbPath).filter { it.fileName != "." && it.fileName != ".." }.map { i ->
                    val attrs = i.fileAttributes
                    FileEntry(
                        loc = d.child(i.fileName),
                        name = i.fileName,
                        isDir = attrs and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value != 0L,
                        size = i.endOfFile,
                        modified = i.lastWriteTime.toEpochMillis(),
                        hidden = i.fileName.startsWith(".") || attrs and FileAttributes.FILE_ATTRIBUTE_HIDDEN.value != 0L,
                    )
                }
        }
    }

    /** Compartilhamentos de disco, sem os administrativos (C$, IPC$…). */
    private fun listarShares(d: Loc.Smb): List<FileEntry> {
        val transporte = SMBTransportFactories.SRVSVC.getTransport(session(d.serverId))
        return ServerService(transporte).shares1
            .filter { (it.type and 0xFF) == 0 && (it.type and 0x80000000.toInt()) == 0 && !it.netName.endsWith("$") }
            .map { FileEntry(loc = d.child(it.netName), name = it.netName, isDir = true, isShare = true) }
    }

    override suspend fun stat(loc: Loc): FileEntry? {
        val l = loc as Loc.Smb
        if (l.path.isEmpty()) return FileEntry(l, l.share.ifEmpty { server(l.serverId).name }, true)
        return io(l.serverId) {
            val share = disk(l)
            if (!share.fileExists(l.smbPath) && !share.folderExists(l.smbPath)) return@io null
            val info = share.getFileInformation(l.smbPath)
            FileEntry(
                loc = l, name = l.name,
                isDir = info.standardInformation.isDirectory,
                size = info.standardInformation.endOfFile,
                modified = info.basicInformation.lastWriteTime.toEpochMillis(),
            )
        }
    }

    fun statBlocking(loc: Loc.Smb): FileEntry = comRetentativa(loc.serverId) {
        val info = disk(loc).getFileInformation(loc.smbPath)
        FileEntry(loc, loc.name, info.standardInformation.isDirectory, info.standardInformation.endOfFile,
            info.basicInformation.lastWriteTime.toEpochMillis())
    }

    // ---------------------------------------------------------------- mudanças

    override suspend fun mkdir(parent: Loc, name: String): Loc {
        val c = (parent as Loc.Smb).child(name)
        if (parent.isServerRoot) throw IOException("Não dá para criar pasta na raiz do servidor")
        io(c.serverId) { disk(c).mkdir(c.smbPath) }
        return c
    }

    override suspend fun delete(loc: Loc) {
        val l = loc as Loc.Smb
        if (l.path.isEmpty()) throw IOException("Compartilhamentos não podem ser apagados")
        io(l.serverId) {
            val share = disk(l)
            if (share.folderExists(l.smbPath)) share.rmdir(l.smbPath, true) else share.rm(l.smbPath)
        }
    }

    override suspend fun rename(loc: Loc, newName: String): Loc {
        val l = loc as Loc.Smb
        val dest = (l.parent() as Loc.Smb).child(newName)
        moveTo(l, dest)
        return dest
    }

    override suspend fun fastMove(src: Loc, dest: Loc): Boolean {
        if (src !is Loc.Smb || dest !is Loc.Smb) return false
        if (src.serverId != dest.serverId || !src.share.equals(dest.share, ignoreCase = true)) return false
        moveTo(src, dest)
        return true
    }

    private suspend fun moveTo(src: Loc.Smb, dest: Loc.Smb) = io(src.serverId) {
        disk(src).open(
            src.smbPath,
            EnumSet.of(AccessMask.DELETE, AccessMask.GENERIC_READ),
            null, SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN, null,
        ).use { it.rename(dest.smbPath, false) }
    }

    // ---------------------------------------------------------------- bytes

    fun openFile(loc: Loc.Smb, write: Boolean = false): File = comRetentativa(loc.serverId) {
        if (write) disk(loc).openFile(
            loc.smbPath, EnumSet.of(AccessMask.GENERIC_WRITE, AccessMask.GENERIC_READ), null,
            SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OVERWRITE_IF,
            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
        ) else disk(loc).openFile(
            loc.smbPath, EnumSet.of(AccessMask.GENERIC_READ), null,
            SMB2ShareAccess.ALL, SMB2CreateDisposition.FILE_OPEN,
            EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE),
        )
    }

    override fun openRead(loc: Loc): InputStream {
        val f = openFile(loc as Loc.Smb)
        return object : FilterInputStream(f.inputStream) {
            override fun close() { try { super.close() } finally { runCatching { f.close() } } }
        }
    }

    override fun openWrite(loc: Loc): OutputStream {
        val f = openFile(loc as Loc.Smb, write = true)
        return object : FilterOutputStream(f.outputStream) {
            override fun write(b: ByteArray, off: Int, len: Int) = out.write(b, off, len)
            override fun close() { try { super.close() } finally { runCatching { f.close() } } }
        }
    }
}
