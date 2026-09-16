package com.mauricio.libertyx.folder.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mauricio.libertyx.folder.fs.FileEntry
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.ops.Ops
import com.mauricio.libertyx.folder.smb.SmbProvider

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) { Column(Modifier.padding(vertical = 8.dp), content = content) }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier.padding(start = 24.dp, top = 20.dp, bottom = 8.dp),
        style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

/** Ícone colorido do tipo, ou miniatura real para imagem e vídeo. */
@Composable
fun Thumb(e: FileEntry, size: androidx.compose.ui.unit.Dp, corner: androidx.compose.ui.unit.Dp = 10.dp, fill: Boolean = false) {
    val kind = Mime.kind(e)
    val context = LocalContext.current
    val podeMiniatura = when (e.loc) {
        is Loc.Local -> kind == Kind.Image || kind == Kind.Video
        // No servidor, só imagem pequena: vídeo exigiria ler pedaços espalhados de um arquivo enorme.
        is Loc.Smb -> kind == Kind.Image && e.size in 1..25_000_000
        is Loc.Vault -> kind == Kind.Image || kind == Kind.Video
    }
    val mod = (if (fill) Modifier.fillMaxSize() else Modifier.size(size)).clip(RoundedCornerShape(corner))
    val icone = @Composable {
        Box(mod.background(Mime.color(e).copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Mime.icon(e), null, tint = Mime.color(e), modifier = Modifier.size(if (fill) 48.dp else size * 0.58f))
        }
    }
    if (!podeMiniatura) { icone(); return }
    val model: Any = when (val l = e.loc) {
        is Loc.Local -> l.file; is Loc.Smb -> SmbProvider.uri(l); is Loc.Vault -> com.mauricio.libertyx.folder.vault.VaultProvider.uri(l)
    }
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(model).crossfade(true)
            // Miniatura decifrada nunca vai para o cache em disco.
            .apply { if (e.loc is Loc.Vault) diskCachePolicy(coil3.request.CachePolicy.DISABLED) }
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = mod,
        loading = { icone() },
        error = { icone() },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    e: FileEntry, selecionando: Boolean, selecionado: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit, subtitulo: String? = null,
) {
    Row(
        Modifier.fillMaxWidth()
            .tvFocus(radius = 0.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selecionado) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f) else Color.Transparent)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecionando) {
            Icon(
                if (selecionado) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                tint = if (selecionado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 14.dp).size(24.dp),
            )
        }
        Thumb(e, 44.dp)
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(e.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge)
            val sub = subtitulo ?: listOfNotNull(
                Fmt.date(e.modified).ifEmpty { null },
                if (!e.isDir) Fmt.size(e.size) else null,
            ).joinToString("  ·  ")
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCell(e: FileEntry, selecionando: Boolean, selecionado: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier.tvFocus(grow = 1.05f).clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .background(if (selecionado) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Thumb(e, 96.dp, corner = 14.dp, fill = true)
            if (selecionando) Icon(
                if (selecionado) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                tint = if (selecionado) MaterialTheme.colorScheme.primary else Color.White,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(22.dp)
                    .background(Color.Black.copy(alpha = 0.25f), CircleShape),
            )
        }
        Text(e.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, lineHeight = 15.sp,
            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp))
    }
}

/** Painel de progresso da cópia/movimentação, por cima de qualquer tela. */
@Composable
fun OpPanel(modifier: Modifier = Modifier) {
    val s by Ops.state.collectAsState()
    val op = s ?: return
    Surface(
        modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh, shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
            val titulo = when {
                !op.terminou -> op.titulo + "…"
                op.erro != null -> "Falhou"
                else -> "Concluído"
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(titulo, fontWeight = FontWeight.SemiBold)
                    val det = op.erro ?: buildString {
                        if (op.itensTotal > 0) append("${minOf(op.itensFeitos + if (op.terminou) 0 else 1, op.itensTotal)} de ${op.itensTotal}")
                        if (op.total > 0) append("  ·  ${Fmt.size(op.feitos)} de ${Fmt.size(op.total)}")
                        if (!op.terminou && op.atual.isNotEmpty()) append("\n${op.atual}")
                    }
                    Text(det, style = MaterialTheme.typography.bodySmall,
                        color = if (op.erro != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (op.terminou) TextButton(onClick = { Ops.dismiss() }, Modifier.tvFocus(24.dp)) { Text("OK") }
                else TextButton(onClick = { Ops.cancel() }, Modifier.tvFocus(24.dp)) { Text("Cancelar") }
            }
            if (!op.terminou) {
                Spacer(Modifier.height(10.dp))
                if (op.total > 0) LinearProgressIndicator(progress = { op.fracao }, Modifier.fillMaxWidth())
                else LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun UsageBar(usado: Long, total: Long) {
    val f = if (total > 0) usado.toFloat() / total else 0f
    Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest)) {
        Box(Modifier.fillMaxWidth(f).height(6.dp).clip(CircleShape).background(
            if (f > 0.9f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary))
    }
}

@Composable
fun EmptyState(texto: String, sub: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (icon != null) Icon(icon, null, Modifier.size(56.dp).padding(bottom = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(texto, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun RowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, cor: Color) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(cor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = cor, modifier = Modifier.size(22.dp))
    }
}

