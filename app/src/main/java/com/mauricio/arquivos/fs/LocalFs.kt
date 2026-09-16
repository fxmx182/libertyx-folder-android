package com.mauricio.arquivos.fs

import android.media.MediaScannerConnection
import com.mauricio.arquivos.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

object LocalFs : Fs {

    private fun Loc.f(): File = (this as Loc.Local).file

    fun entry(loc: Loc.Local, f: File = loc.file) = FileEntry(
        loc = loc,
        name = f.name,
        isDir = f.isDirectory,
        size = if (f.isDirectory) 0 else f.length(),
        modified = f.lastModified(),
        hidden = f.name.startsWith("."),
    )

    override suspend fun list(dir: Loc): List<FileEntry> = withContext(Dispatchers.IO) {
        val d = dir as Loc.Local
        val files = d.file.listFiles() ?: throw IOException("Sem acesso a ${d.path}")
        files.map { entry(d.child(it.name) as Loc.Local, it) }
    }

    override suspend fun stat(loc: Loc): FileEntry? = withContext(Dispatchers.IO) {
        val l = loc as Loc.Local
        if (l.file.exists()) entry(l) else null
    }

    override suspend fun mkdir(parent: Loc, name: String): Loc = withContext(Dispatchers.IO) {
        val c = parent.child(name)
        if (!c.f().mkdirs() && !c.f().isDirectory) throw IOException("Não foi possível criar a pasta")
        c
    }

    override suspend fun delete(loc: Loc) = withContext(Dispatchers.IO) {
        val f = loc.f()
        if (!f.deleteRecursively()) throw IOException("Não foi possível apagar ${f.name}")
        scan(f.path)
    }

    override suspend fun rename(loc: Loc, newName: String): Loc = withContext(Dispatchers.IO) {
        val dest = loc.parent()!!.child(newName)
        if (dest.f().exists()) throw IOException("Já existe um item com esse nome")
        if (!loc.f().renameTo(dest.f())) throw IOException("Não foi possível renomear")
        scan(loc.f().path, dest.f().path)
        dest
    }

    override suspend fun fastMove(src: Loc, dest: Loc): Boolean = withContext(Dispatchers.IO) {
        if (src !is Loc.Local || dest !is Loc.Local || src.root != dest.root) return@withContext false
        src.file.renameTo(dest.file).also { if (it) scan(src.path, dest.path) }
    }

    override fun openRead(loc: Loc) = FileInputStream(loc.f())

    override fun openWrite(loc: Loc): FileOutputStream {
        loc.f().parentFile?.mkdirs()
        return FileOutputStream(loc.f())
    }

    /** Avisa o MediaStore, senão as categorias e a galeria mostram o que já não existe. */
    fun scan(vararg paths: String) {
        runCatching { MediaScannerConnection.scanFile(App.instance, paths, null, null) }
    }
}
