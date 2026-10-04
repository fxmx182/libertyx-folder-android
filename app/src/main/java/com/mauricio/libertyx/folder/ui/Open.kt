package com.mauricio.libertyx.folder.ui

import android.content.ClipData
import com.mauricio.libertyx.folder.R
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.mauricio.libertyx.folder.BuildConfig
import com.mauricio.libertyx.folder.fs.FileEntry
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.smb.SmbProvider

object Open {
    fun uri(context: Context, loc: Loc): Uri = when (loc) {
        is Loc.Local -> FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.files", loc.file)
        is Loc.Smb -> SmbProvider.uri(loc)
        is Loc.Vault -> com.mauricio.libertyx.folder.vault.VaultProvider.uri(loc)
    }

    fun open(context: Context, e: FileEntry, escolher: Boolean = false) {
        // O app não instala apps (não pede REQUEST_INSTALL_PACKAGES): sem a
        // permissão, o instalador do sistema recusaria com um erro confuso.
        // E foi justamente "pode instalar outros apps" o motivo do Play
        // Protect para bloquear o pacote antigo.
        if (Mime.kind(e) == Kind.Apk) {
            Toast.makeText(context, context.getString(R.string.apk_nao_instala), Toast.LENGTH_LONG).show()
            return
        }
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri(context, e.loc), Mime.of(e.name))
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(if (escolher) Intent.createChooser(i, context.getString(R.string.abrir_com)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else i)
        } catch (_: Exception) {
            // Sem app para o tipo exato: oferece todos.
            try {
                context.startActivity(
                    Intent.createChooser(Intent(i).setDataAndType(i.data, "*/*"), context.getString(R.string.abrir_com))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
                Toast.makeText(context, context.getString(R.string.abrir_nenhum_app), Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun share(context: Context, itens: List<FileEntry>) {
        val arquivos = itens.filter { !it.isDir }
        if (arquivos.isEmpty()) {
            Toast.makeText(context, context.getString(R.string.compartilhar_pastas), Toast.LENGTH_SHORT).show(); return
        }
        val uris = arquivos.map { uri(context, it.loc) }
        val tipos = arquivos.map { Mime.of(it.name) }.distinct()
        val tipo = if (tipos.size == 1) tipos[0] else tipos.map { it.substringBefore('/') }.distinct().singleOrNull()?.let { "$it/*" } ?: "*/*"
        val i = if (uris.size == 1) Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris[0])
            else Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        i.type = tipo
        i.clipData = ClipData.newRawUri("", uris[0]).apply { uris.drop(1).forEach { addItem(ClipData.Item(it)) } }
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(i, context.getString(R.string.compartilhar)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
