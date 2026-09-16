package com.mauricio.arquivos.smb

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
import com.hierynomus.smbj.share.File
import com.mauricio.arquivos.BuildConfig
import com.mauricio.arquivos.fs.Loc
import com.mauricio.arquivos.ui.Mime
import java.io.FileNotFoundException

/**
 * Entrega arquivos do servidor a outros apps sem baixar antes.
 *
 * O outro app (player, leitor de PDF) recebe um descritor "de mentira" do
 * sistema; cada leitura dele vira um pedaço lido do servidor, no ponto pedido.
 * Por isso um vídeo de 20 GB abre na hora e dá para pular para o meio.
 */
class SmbProvider : ContentProvider() {

    override fun onCreate() = true

    private fun loc(uri: Uri): Loc.Smb {
        val p = uri.pathSegments
        if (p.size < 3) throw FileNotFoundException(uri.toString())
        return Loc.Smb(p[0], p[1], p.drop(2).joinToString("/"))
    }

    override fun getType(uri: Uri): String = Mime.of(uri.lastPathSegment ?: "")

    override fun query(uri: Uri, projection: Array<out String>?, s: String?, a: Array<out String>?, o: String?): Cursor {
        val l = loc(uri)
        val cols = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val tamanho = runCatching { SmbFs.statBlocking(l).size }.getOrNull()
        return MatrixCursor(cols).apply {
            addRow(cols.map {
                when (it) {
                    OpenableColumns.DISPLAY_NAME -> l.name
                    OpenableColumns.SIZE -> tamanho
                    else -> null
                }
            })
        }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Somente leitura")
        val l = loc(uri)
        val (arquivo, tamanho) = try {
            val f = SmbFs.openFile(l)
            f to f.fileInformation.standardInformation.endOfFile
        } catch (e: Exception) {
            throw FileNotFoundException(e.message)
        }
        val thread = HandlerThread("smb-fd").apply { start() }
        val sm = context!!.getSystemService(StorageManager::class.java)
        return sm.openProxyFileDescriptor(
            ParcelFileDescriptor.MODE_READ_ONLY,
            Leitor(arquivo, tamanho, thread),
            Handler(thread.looper),
        )
    }

    private class Leitor(val f: File, val tamanho: Long, val thread: HandlerThread) : ProxyFileDescriptorCallback() {
        override fun onGetSize() = tamanho
        override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
            try {
                var lidos = 0
                while (lidos < size && offset + lidos < tamanho) {
                    val n = f.read(data, offset + lidos, lidos, size - lidos)
                    if (n <= 0) break
                    lidos += n
                }
                return lidos
            } catch (e: Exception) {
                throw ErrnoException("smb read", OsConstants.EIO)
            }
        }
        override fun onRelease() {
            runCatching { f.close() }
            thread.quitSafely()
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, s: String?, a: Array<out String>?) = 0
    override fun update(uri: Uri, v: ContentValues?, s: String?, a: Array<out String>?) = 0

    companion object {
        fun uri(l: Loc.Smb): Uri = Uri.Builder().scheme("content")
            .authority("${BuildConfig.APPLICATION_ID}.smb")
            .appendPath(l.serverId).appendPath(l.share)
            .apply { l.path.split('/').filter { it.isNotEmpty() }.forEach { appendPath(it) } }
            .build()
    }
}
