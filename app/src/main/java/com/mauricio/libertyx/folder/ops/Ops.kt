package com.mauricio.libertyx.folder.ops

import android.content.Context
import android.content.Intent
import android.os.Build
import com.mauricio.libertyx.folder.fs.FileEntry
import com.mauricio.libertyx.folder.fs.Fss
import com.mauricio.libertyx.folder.fs.LocalFs
import com.mauricio.libertyx.folder.fs.Loc
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.coroutineContext

data class OpState(
    val titulo: String,
    val atual: String = "",
    val feitos: Long = 0,
    val total: Long = 0,
    val itensFeitos: Int = 0,
    val itensTotal: Int = 0,
    val erro: String? = null,
    val terminou: Boolean = false,
) {
    val fracao: Float get() = if (total > 0) (feitos.toFloat() / total).coerceIn(0f, 1f) else 0f
}

/**
 * Copiar, mover e apagar — entre quaisquer dois lugares (aparelho ↔ servidor,
 * servidor ↔ outro servidor). Uma operação por vez, fora da tela, com um
 * serviço em primeiro plano para o Android não matar uma cópia de 10 GB.
 */
object Ops {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _state = MutableStateFlow<OpState?>(null)
    val state: StateFlow<OpState?> = _state
    private var job: Job? = null

    /** Incrementa a cada operação concluída: as telas recarregam quando muda. */
    val versao = MutableStateFlow(0)

    val ocupado get() = job?.isActive == true

    fun cancel() { job?.cancel() }
    fun dismiss() { if (!ocupado) _state.value = null }

    private fun iniciar(context: Context, titulo: String, bloco: suspend () -> Unit) {
        if (ocupado) return
        _state.value = OpState(titulo)
        val app = context.applicationContext
        runCatching {
            val i = Intent(app, OpsService::class.java)
            if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(i) else app.startService(i)
        }
        job = scope.launch {
            try {
                bloco()
                _state.value = _state.value?.copy(terminou = true)
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.value = _state.value?.copy(terminou = true, erro = "Cancelado")
            } catch (e: Throwable) {
                _state.value = _state.value?.copy(terminou = true, erro = e.message ?: e.toString())
            } finally {
                versao.value++
            }
        }
    }

    fun delete(context: Context, itens: List<FileEntry>) = iniciar(context, "Excluindo") {
        _state.value = _state.value!!.copy(itensTotal = itens.size)
        itens.forEachIndexed { i, e ->
            coroutineContext.ensureActive()
            _state.value = _state.value!!.copy(atual = e.name, itensFeitos = i)
            Fss.of(e.loc).delete(e.loc)
        }
        _state.value = _state.value!!.copy(itensFeitos = itens.size)
    }

    fun transfer(context: Context, itens: List<FileEntry>, destino: Loc, mover: Boolean) =
        iniciar(context, if (mover) "Movendo" else "Copiando") {
            for (e in itens) {
                if (destino == e.loc || isInside(destino, e.loc)) throw IOException("Não dá para ${if (mover) "mover" else "copiar"} uma pasta para dentro dela mesma")
            }
            _state.value = _state.value!!.copy(atual = "Calculando…", itensTotal = itens.size)
            val total = itens.sumOf { tamanho(it) }
            _state.value = _state.value!!.copy(total = total)

            itens.forEachIndexed { i, e ->
                coroutineContext.ensureActive()
                _state.value = _state.value!!.copy(itensFeitos = i)
                val existentes = Fss.of(destino).list(destino).map { it.name.lowercase() }.toSet()
                val nome = nomeLivre(e.name, e.isDir, existentes)
                val alvo = destino.child(nome)
                if (mover && e.loc.parent() == destino) return@forEachIndexed
                if (mover && Fss.of(e.loc).fastMove(e.loc, alvo)) {
                    _state.value = _state.value!!.let { it.copy(feitos = it.feitos + tamanho(e, jaMovido = true)) }
                } else {
                    copiar(e, alvo)
                    if (mover) Fss.of(e.loc).delete(e.loc)
                }
            }
            _state.value = _state.value!!.copy(itensFeitos = itens.size, feitos = total)
        }

    private suspend fun tamanho(e: FileEntry, jaMovido: Boolean = false): Long {
        if (jaMovido) return if (e.isDir) 0 else e.size
        if (!e.isDir) return e.size
        return Fss.of(e.loc).list(e.loc).sumOf { tamanho(it) }
    }

    private suspend fun copiar(e: FileEntry, alvo: Loc) {
        coroutineContext.ensureActive()
        _state.value = _state.value!!.copy(atual = e.name)
        val fs = Fss.of(alvo)
        if (e.isDir) {
            fs.mkdir(alvo.parent()!!, alvo.name)
            for (filho in Fss.of(e.loc).list(e.loc)) copiar(filho, alvo.child(filho.name))
            return
        }
        withContext(Dispatchers.IO) {
            val buf = ByteArray(1 shl 20)
            try {
                Fss.of(e.loc).openRead(e.loc).use { input ->
                    fs.openWrite(alvo).use { out ->
                        while (true) {
                            coroutineContext.ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            out.write(buf, 0, n)
                            _state.value = _state.value!!.let { it.copy(feitos = it.feitos + n) }
                        }
                    }
                }
            } catch (t: Throwable) {
                // Arquivo pela metade no destino é pior que nenhum.
                runCatching { kotlinx.coroutines.runBlocking { fs.delete(alvo) } }
                throw t
            }
            if (alvo is Loc.Vault) com.mauricio.libertyx.folder.vault.VaultFs.setMeta(alvo, e.modified, origemDe(e.loc))
            if (alvo is Loc.Local) {
                runCatching { alvo.file.setLastModified(e.modified) }
                LocalFs.scan(alvo.path)
            }
        }
    }

    private fun origemDe(l: Loc): String? = (l as? Loc.Local)?.path

    /**
     * "Retirar da pasta segura": cada item volta para a pasta de onde veio;
     * se ela não existe mais (ou veio de servidor), vai para Download.
     */
    fun restore(context: Context, itens: List<FileEntry>) = iniciar(context, "Retirando da pasta segura") {
        val download = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS).path
        val grupos = itens.groupBy { e ->
            val origem = (e.loc as? Loc.Vault)?.let { com.mauricio.libertyx.folder.vault.VaultFs.origem(it) }
            val pai = origem?.let { java.io.File(it).parentFile }?.takeIf { it.isDirectory }?.path ?: download
            com.mauricio.libertyx.folder.media.Storage.locFor(context, pai)
        }
        _state.value = _state.value!!.copy(itensTotal = itens.size, atual = "Calculando…")
        _state.value = _state.value!!.copy(total = itens.sumOf { tamanho(it) })
        var feitos = 0
        for ((destino, grupo) in grupos) for (e in grupo) {
            coroutineContext.ensureActive()
            _state.value = _state.value!!.copy(itensFeitos = feitos++)
            val existentes = Fss.of(destino).list(destino).map { it.name.lowercase() }.toSet()
            copiar(e, destino.child(nomeLivre(e.name, e.isDir, existentes)))
            Fss.of(e.loc).delete(e.loc)
        }
        _state.value = _state.value!!.copy(itensFeitos = itens.size)
    }

    private fun isInside(dest: Loc, src: Loc): Boolean {
        var p: Loc? = dest.parent()
        while (p != null) { if (p == src) return true; p = p.parent() }
        return false
    }

    /** "foto.jpg" → "foto (1).jpg" se já existir: nunca sobrescreve em silêncio. */
    fun nomeLivre(nome: String, isDir: Boolean, existentes: Set<String>): String {
        if (nome.lowercase() !in existentes) return nome
        val (base, ext) = if (isDir || !nome.contains('.') || nome.startsWith(".")) nome to ""
            else nome.substringBeforeLast('.') to "." + nome.substringAfterLast('.')
        var i = 1
        while ("$base ($i)$ext".lowercase() in existentes) i++
        return "$base ($i)$ext"
    }
}
