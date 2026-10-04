package com.mauricio.libertyx.folder.media

import android.content.Context
import com.mauricio.libertyx.folder.Textos
import com.mauricio.libertyx.folder.R
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import com.mauricio.libertyx.folder.fs.Loc
import java.io.File

data class Volume(val name: String, val path: String, val removable: Boolean, val total: Long, val free: Long) {
    val loc get() = Loc.Local(path, path)
}

object Storage {
    fun volumes(context: Context): List<Volume> {
        val sm = context.getSystemService(StorageManager::class.java)
        val lista = sm.storageVolumes.mapNotNull { v ->
            val dir: File? = if (Build.VERSION.SDK_INT >= 30) v.directory else runCatching {
                v.javaClass.getMethod("getPathFile").invoke(v) as File
            }.getOrNull()
            if (dir == null || !dir.canRead()) return@mapNotNull null
            val st = runCatching { StatFs(dir.path) }.getOrNull()
            Volume(
                name = if (v.isPrimary) Textos.get(R.string.vol_interno) else v.getDescription(context) ?: dir.name,
                path = dir.path,
                removable = v.isRemovable,
                total = st?.totalBytes ?: 0,
                free = st?.availableBytes ?: 0,
            )
        }
        return lista.ifEmpty {
            val d = Environment.getExternalStorageDirectory()
            val st = StatFs(d.path)
            listOf(Volume(Textos.get(R.string.vol_interno), d.path, false, st.totalBytes, st.availableBytes))
        }
    }

    /** O [Loc.Local] certo para um caminho qualquer: a raiz é o volume que o contém. */
    fun locFor(context: Context, path: String): Loc.Local {
        val raiz = volumes(context).map { it.path }.filter { path == it || path.startsWith("$it/") }
            .maxByOrNull { it.length } ?: File(path).parent ?: "/"
        return Loc.Local(path, raiz)
    }
}
