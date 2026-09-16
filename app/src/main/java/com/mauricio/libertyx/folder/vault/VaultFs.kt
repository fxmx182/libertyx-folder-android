package com.mauricio.libertyx.folder.vault

import com.mauricio.libertyx.folder.fs.FileEntry
import com.mauricio.libertyx.folder.fs.Fs
import com.mauricio.libertyx.folder.fs.Loc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.FilterOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/** A pasta segura vista como mais um lugar: copiar, mover, renomear e apagar funcionam igual. */
object VaultFs : Fs {

    private fun Loc.p() = (this as Loc.Vault).path

    private fun entry(i: Vault.Item) = FileEntry(
        loc = Loc.Vault(i.path), name = i.path.substringAfterLast('/'), isDir = i.isDir,
        size = i.size, modified = i.modified, hidden = false,
    )

    override suspend fun list(dir: Loc): List<FileEntry> = withContext(Dispatchers.IO) {
        Vault.key()
        val base = dir.p()
        Vault.snapshot().filter { it.path.substringBeforeLast('/', "") == base && it.path.isNotEmpty() }.map(::entry)
    }

    override suspend fun stat(loc: Loc): FileEntry? =
        if (loc.p().isEmpty()) FileEntry(loc, "Pasta segura", true) else Vault.get(loc.p())?.let(::entry)

    override suspend fun mkdir(parent: Loc, name: String): Loc = withContext(Dispatchers.IO) {
        val c = parent.child(name)
        if (Vault.get(c.p()) == null) Vault.put(Vault.Item(c.p(), null, 0, System.currentTimeMillis(), null))
        c
    }

    override suspend fun delete(loc: Loc) = withContext(Dispatchers.IO) { Vault.remove(loc.p()) }

    override suspend fun rename(loc: Loc, newName: String): Loc = withContext(Dispatchers.IO) {
        val dest = loc.parent()!!.child(newName)
        Vault.move(loc.p(), dest.p()); dest
    }

    override suspend fun fastMove(src: Loc, dest: Loc): Boolean {
        if (src !is Loc.Vault || dest !is Loc.Vault) return false
        withContext(Dispatchers.IO) { Vault.move(src.path, dest.path) }
        return true
    }

    fun reader(loc: Loc.Vault): VaultCrypto.Reader {
        val item = Vault.get(loc.path) ?: throw IOException("Arquivo não encontrado")
        val id = item.id ?: throw IOException("É uma pasta")
        return VaultCrypto.Reader(Vault.blob(id), VaultCrypto.fileKey(Vault.key(), id))
    }

    override fun openRead(loc: Loc): InputStream = reader(loc as Loc.Vault).stream()

    /** Grava num blob novo e só registra no índice quando termina: cópia pela metade não aparece. */
    override fun openWrite(loc: Loc): OutputStream {
        val path = loc.p()
        val id = UUID.randomUUID().toString()
        val arquivo = Vault.blob(id)
        var total = 0L
        val cifrado = VaultCrypto.encryptingStream(FileOutputStream(arquivo), VaultCrypto.fileKey(Vault.key(), id))
        return object : FilterOutputStream(cifrado) {
            override fun write(b: ByteArray, off: Int, len: Int) { out.write(b, off, len); total += len }
            override fun write(b: Int) { out.write(b); total++ }
            override fun close() {
                super.close()
                val antigo = Vault.get(path)
                Vault.put(Vault.Item(path, id, total, System.currentTimeMillis(), antigo?.origem))
                antigo?.id?.let { Vault.blob(it).delete() }
            }
        }
    }

    fun setMeta(loc: Loc.Vault, modified: Long, origem: String?) {
        val i = Vault.get(loc.path) ?: return
        Vault.put(i.copy(modified = if (modified > 0) modified else i.modified, origem = origem ?: i.origem))
    }

    fun origem(loc: Loc.Vault): String? = Vault.get(loc.path)?.origem
}
