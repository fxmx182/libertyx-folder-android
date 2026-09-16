package com.mauricio.arquivos.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.mauricio.arquivos.fs.FileEntry
import com.mauricio.arquivos.fs.Loc
import com.mauricio.arquivos.smb.SmbServerStore

@Composable
fun NameDialog(titulo: String, inicial: String, confirmar: String, onDismiss: () -> Unit, onOk: (String) -> Unit) {
    // No renomear, seleciona só o nome, sem a extensão — como no One UI.
    val fim = inicial.lastIndexOf('.').takeIf { it > 0 } ?: inicial.length
    var v by remember { mutableStateOf(TextFieldValue(inicial, TextRange(0, fim))) }
    val foco = remember { FocusRequester() }
    val invalido = v.text.isBlank() || v.text.contains('/') || v.text.contains('\\')
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            OutlinedTextField(
                v, { v = it }, singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(foco),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (!invalido) onOk(v.text.trim()) }),
            )
        },
        confirmButton = { TextButton(onClick = { onOk(v.text.trim()) }, enabled = !invalido) { Text(confirmar) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
    LaunchedEffect(Unit) { foco.requestFocus() }
}

@Composable
fun ConfirmDialog(titulo: String, texto: String, confirmar: String, onDismiss: () -> Unit, onOk: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss, title = { Text(titulo) }, text = { Text(texto) },
        confirmButton = { TextButton(onClick = onOk) { Text(confirmar, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
fun SortDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ordenar por") },
        text = {
            Column {
                Sort.entries.forEach { s ->
                    Row(Modifier.fillMaxWidth().clickable { Prefs.sort = s }, verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(Prefs.sort == s, { Prefs.sort = s }); Text(s.titulo)
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Ordem crescente", Modifier.weight(1f)); Switch(Prefs.asc, { Prefs.asc = it })
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Mostrar arquivos ocultos", Modifier.weight(1f)); Switch(Prefs.hidden, { Prefs.hidden = it })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

@Composable
fun DetailsDialog(itens: List<FileEntry>, onDismiss: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var tamanho by remember { mutableStateOf<Long?>(null) }
    var contagem by remember { mutableStateOf(0 to 0) }
    LaunchedEffect(itens) {
        var arquivos = 0; var pastas = 0
        suspend fun somar(e: FileEntry): Long {
            if (!e.isDir) { arquivos++; return e.size }
            pastas++
            return runCatching { com.mauricio.arquivos.fs.Fss.of(e.loc).list(e.loc) }.getOrDefault(emptyList()).sumOf { somar(it) }
        }
        val total = itens.sumOf { somar(it) }
        tamanho = total; contagem = arquivos to pastas
    }
    val um = itens.singleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(um?.name ?: "${itens.size} itens") },
        text = {
            Column {
                @Composable fun linha(k: String, v: String) {
                    Text(k, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp))
                    Text(v, style = MaterialTheme.typography.bodyMedium)
                }
                linha("Tamanho", tamanho?.let { Fmt.size(it) + if (it >= 1024) " (%,d bytes)".format(it) else "" } ?: "Calculando…")
                if (itens.any { it.isDir } && tamanho != null) linha("Contém", "${contagem.first} arquivos, ${maxOf(0, contagem.second - itens.count { it.isDir })} pastas")
                if (um != null) {
                    if (um.modified > 0) linha("Modificado", Fmt.date(um.modified))
                    if (!um.isDir) linha("Tipo", Mime.of(um.name))
                    linha("Local", descrever(ctx, um.loc))
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
}

fun descrever(ctx: android.content.Context, loc: Loc): String = when (loc) {
    is Loc.Local -> loc.path
    is Loc.Vault -> "Pasta segura" + if (loc.path.isEmpty()) "" else "/" + loc.path
    is Loc.Smb -> {
        val s = SmbServerStore.get(ctx).byId(loc.serverId)
        "smb://${s?.displayHost ?: "?"}/${loc.share}${if (loc.path.isEmpty()) "" else "/" + loc.path}"
    }
}
