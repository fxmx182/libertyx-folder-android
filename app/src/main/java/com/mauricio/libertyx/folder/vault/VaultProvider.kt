package com.mauricio.libertyx.folder.vault

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import android.system.ErrnoException
import android.system.OsConstants
import com.mauricio.libertyx.folder.BuildConfig
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.ui.Mime
import java.io.FileNotFoundException

/**
 * Entrega um arquivo da pasta segura a outro app (visualizador, player)
 * decifrando sob demanda — nada decifrado vai para o disco.
 */
class VaultProvider : ContentProvider() {
    override fun onCreate() = true

    private fun loc(uri: Uri) = Loc.Vault(uri.pathSegments.joinToString("/"))

    override fun getType(uri: Uri): String = Mime.of(uri.lastPathSegment ?: "")

    override fun query(uri: Uri, projection: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor {
        val l = loc(uri)
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val item = Vault.get(l.path)
        return MatrixCursor(cols).apply {
            addRow(cols.map { when (it) { OpenableColumns.DISPLAY_NAME -> l.name; OpenableColumns.SIZE -> item?.size; else -> null } })
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Somente leitura")
        val leitor = try { VaultFs.reader(loc(uri)) } catch (e: Exception) { throw FileNotFoundException(e.message) }
        val thread = HandlerThread("cofre-fd").apply { start() }
        return context!!.getSystemService(StorageManager::class.java).openProxyFileDescriptor(
            ParcelFileDescriptor.MODE_READ_ONLY,
            object : ProxyFileDescriptorCallback() {
                override fun onGetSize() = leitor.size
                override fun onRead(offset: Long, size: Int, data: ByteArray): Int = try {
                    maxOf(0, leitor.read(offset, data, 0, size))
                } catch (e: Exception) { throw ErrnoException("cofre", OsConstants.EIO) }
                override fun onRelease() { runCatching { leitor.close() }; thread.quitSafely() }
            },
            Handler(thread.looper),
        )
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

    companion object {
        fun uri(l: Loc.Vault): Uri = Uri.Builder().scheme("content").authority("${BuildConfig.APPLICATION_ID}.cofre")
            .apply { l.path.split('/').filter { it.isNotEmpty() }.forEach { appendPath(it) } }.build()
    }
}
