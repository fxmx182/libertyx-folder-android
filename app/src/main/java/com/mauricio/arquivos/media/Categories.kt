package com.mauricio.arquivos.media

import android.content.Context
import android.provider.MediaStore
import com.mauricio.arquivos.fs.FileEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

enum class Category(val titulo: String) {
    Images("Imagens"), Videos("Vídeos"), Audio("Áudio"), Documents("Documentos"),
    Downloads("Downloads"), Apks("Arquivos de instalação"), Compressed("Compactados"),
}

/** As categorias vêm do índice do sistema (MediaStore) — varrer o disco a cada abertura seria lento. */
object Categories {

    private val DOCS = listOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "odt", "ods", "odp", "rtf", "csv", "epub", "md", "hwp")
    private val ZIPS = listOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

    suspend fun list(context: Context, cat: Category): List<FileEntry> = withContext(Dispatchers.IO) {
        when (cat) {
            Category.Images -> query(context, "${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}")
            Category.Videos -> query(context, "${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}")
            Category.Audio -> query(context, "${MediaStore.Files.FileColumns.MEDIA_TYPE}=${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO}")
            Category.Documents -> porExtensao(context, DOCS)
            Category.Compressed -> porExtensao(context, ZIPS)
            Category.Apks -> porExtensao(context, listOf("apk", "apks", "xapk"))
            Category.Downloads -> emptyList()
        }
    }

    private fun porExtensao(context: Context, exts: List<String>): List<FileEntry> {
        val where = exts.joinToString(" OR ") { "${MediaStore.Files.FileColumns.DATA} LIKE ?" }
        return query(context, "($where)", exts.map { "%.$it" }.toTypedArray())
    }

    fun recent(context: Context, limit: Int = 30): List<FileEntry> = query(
        context,
        "${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL AND ${MediaStore.Files.FileColumns.DATA} NOT LIKE '%/.%'" +
            " AND ${MediaStore.Files.FileColumns.DATA} NOT LIKE '%/Android/%'",
        null, limit,
    )

    fun search(context: Context, termo: String, limit: Int = 500): List<FileEntry> = query(
        context, "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?", arrayOf("%$termo%"), limit, incluirPastas = true,
    )

    private fun query(
        context: Context, where: String, args: Array<String>? = null, limit: Int = 0, incluirPastas: Boolean = false,
    ): List<FileEntry> {
        val uri = MediaStore.Files.getContentUri("external")
        val cols = arrayOf(
            MediaStore.Files.FileColumns.DATA, MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED, MediaStore.Files.FileColumns.MIME_TYPE,
        )
        val ordem = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC" + if (limit > 0) " LIMIT $limit" else ""
        val out = mutableListOf<FileEntry>()
        val vols = Storage.volumes(context).map { it.path }
        runCatching {
            context.contentResolver.query(uri, cols, where, args, ordem)?.use { c ->
                while (c.moveToNext()) {
                    val path = c.getString(0) ?: continue
                    val f = File(path)
                    val isDir = c.isNull(3) && f.isDirectory
                    if (isDir && !incluirPastas) continue
                    if (!f.exists()) continue
                    val raiz = vols.filter { path.startsWith("$it/") }.maxByOrNull { it.length } ?: f.parent ?: "/"
                    out += FileEntry(
                        loc = com.mauricio.arquivos.fs.Loc.Local(path, raiz),
                        name = f.name, isDir = isDir,
                        size = c.getLong(1), modified = c.getLong(2) * 1000,
                    )
                }
            }
        }
        return out
    }
}
