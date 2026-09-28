package com.mauricio.libertyx.folder.ui

import android.text.format.DateFormat
import android.webkit.MimeTypeMap
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderShared
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.mauricio.libertyx.folder.App
import com.mauricio.libertyx.folder.fs.FileEntry
import java.util.Date

enum class Kind { Folder, Image, Video, Audio, Pdf, Doc, Sheet, Slides, Text, Archive, Apk, Code, Other }

object Mime {
    private val extra = mapOf(
        "mkv" to "video/x-matroska", "ts" to "video/mp2t", "m2ts" to "video/mp2t", "flac" to "audio/flac",
        "opus" to "audio/ogg", "apk" to "application/vnd.android.package-archive", "heic" to "image/heic",
        "heif" to "image/heif", "webp" to "image/webp", "rmvb" to "application/vnd.rn-realmedia-vbr",
        "7z" to "application/x-7z-compressed", "rar" to "application/vnd.rar", "srt" to "application/x-subrip",
        "md" to "text/markdown", "json" to "application/json", "log" to "text/plain", "epub" to "application/epub+zip",
    )

    fun of(name: String): String {
        val ext = name.substringAfterLast('.', "").lowercase()
        return extra[ext] ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
    }

    fun kind(e: FileEntry): Kind {
        if (e.isDir) return Kind.Folder
        val ext = e.ext
        val m = of(e.name)
        return when {
            ext == "apk" || ext == "xapk" || ext == "apks" -> Kind.Apk
            ext == "pdf" -> Kind.Pdf
            m.startsWith("image/") -> Kind.Image
            m.startsWith("video/") || ext in setOf("rmvb", "flv", "wmv", "vob") -> Kind.Video
            m.startsWith("audio/") -> Kind.Audio
            ext in setOf("doc", "docx", "odt", "rtf", "hwp") -> Kind.Doc
            ext in setOf("xls", "xlsx", "ods", "csv") -> Kind.Sheet
            ext in setOf("ppt", "pptx", "odp") -> Kind.Slides
            ext in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz") -> Kind.Archive
            ext in setOf("kt", "java", "py", "js", "ts", "html", "css", "json", "xml", "sh", "c", "cpp", "yml", "yaml") -> Kind.Code
            m.startsWith("text/") || ext in setOf("txt", "md", "log", "srt", "ass") -> Kind.Text
            else -> Kind.Other
        }
    }

    fun icon(e: FileEntry): ImageVector = when (kind(e)) {
        Kind.Folder -> if (e.isShare) Icons.Filled.FolderShared else Icons.Filled.Folder
        Kind.Image -> Icons.Filled.Image
        Kind.Video -> Icons.Filled.VideoFile
        Kind.Audio -> Icons.Filled.AudioFile
        Kind.Pdf -> Icons.Filled.PictureAsPdf
        Kind.Doc -> Icons.Filled.Description
        Kind.Sheet -> Icons.Filled.TableChart
        Kind.Slides -> Icons.Filled.Slideshow
        Kind.Text -> Icons.Filled.Article
        Kind.Archive -> Icons.Filled.FolderZip
        Kind.Apk -> Icons.Filled.Android
        Kind.Code -> Icons.Filled.Code
        Kind.Other -> Icons.Filled.InsertDriveFile
    }

    /**
     * A cor de cada tipo. Escolhidas para o fundo escuro: no claro elas eram
     * cores cheias, que sobre preto ficam pesadas — aqui são as mesmas
     * famílias, um tom acima e menos saturadas. A pasta é a cor da marca, que
     * é também a do ícone do app.
     */
    fun color(e: FileEntry): Color = when (kind(e)) {
        Kind.Folder -> Lx.ouro
        Kind.Image -> Color(0xFFC08BFF)
        Kind.Video -> Color(0xFFFF6E8A)
        Kind.Audio -> Color(0xFF35D0BE)
        Kind.Pdf -> Color(0xFFFF6B5E)
        Kind.Doc -> Color(0xFF5B9CFF)
        Kind.Sheet -> Color(0xFF46C97E)
        Kind.Slides -> Color(0xFFFFA24D)
        Kind.Text -> Color(0xFFA8B3C2)
        Kind.Archive -> Color(0xFFD9A15C)
        Kind.Apk -> Color(0xFF8BD962)
        Kind.Code -> Color(0xFF8A96FF)
        Kind.Other -> Color(0xFF9EA3AD)
    }
}

object Fmt {
    fun size(b: Long): String {
        if (b < 1024) return "$b B"
        val u = listOf("KB", "MB", "GB", "TB")
        var v = b / 1024.0; var i = 0
        while (v >= 1024 && i < u.size - 1) { v /= 1024; i++ }
        return if (v >= 100) "%.0f %s".format(v, u[i]) else "%.1f %s".format(v, u[i]).replace('.', ',')
    }

    fun date(ms: Long): String {
        if (ms <= 0) return ""
        val c = App.instance
        return DateFormat.getMediumDateFormat(c).format(Date(ms)) + " " + DateFormat.getTimeFormat(c).format(Date(ms))
    }
}
