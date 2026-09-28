package com.mauricio.libertyx.folder.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mauricio.libertyx.folder.fs.FileEntry
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.ops.Ops
import com.mauricio.libertyx.folder.smb.SmbProvider

/** O cartão de vidro que agrupa uma seção da tela inicial. */
@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().vidro().padding(vertical = 6.dp), content = content)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(), modifier = modifier.padding(start = 26.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
        style = Lx.tituloSecao,
    )
}

/**
 * O selo de um tipo de arquivo: a cor do tipo em degradê dentro de um quadrado
 * de cantos vivos. Quadrado, e não círculo: ao lado de miniaturas de foto — que
 * são retângulos — o círculo quebrava o alinhamento da coluna.
 */
@Composable
fun Selo(cor: Color, tamanho: Dp, raio: Dp, forte: Boolean = true, conteudo: @Composable () -> Unit) {
    val fim = if (forte) 0.30f else 0.15f
    Box(
        Modifier.size(tamanho).clip(RoundedCornerShape(raio))
            .background(Brush.linearGradient(listOf(cor.copy(alpha = fim), cor.copy(alpha = fim / 3f))))
            .border(0.5.dp, cor.copy(alpha = if (forte) 0.22f else 0.13f), RoundedCornerShape(raio)),
        contentAlignment = Alignment.Center,
        content = { conteudo() },
    )
}

/** Ícone colorido do tipo, ou miniatura real para imagem e vídeo. */
@Composable
fun Thumb(e: FileEntry, size: Dp, corner: Dp = 13.dp, fill: Boolean = false) {
    val kind = Mime.kind(e)
    val context = LocalContext.current
    val podeMiniatura = when (e.loc) {
        is Loc.Local -> kind == Kind.Image || kind == Kind.Video
        // No servidor, só imagem pequena: vídeo exigiria ler pedaços espalhados de um arquivo enorme.
        is Loc.Smb -> kind == Kind.Image && e.size in 1..25_000_000
        is Loc.Vault -> kind == Kind.Image || kind == Kind.Video
    }
    val cor = Mime.color(e)
    val icone = @Composable {
        if (fill) {
            // Na grade a placa é grande: em ouro, uma pasta atrás da outra vira
            // uma parede. A da pasta é de vidro, e só o desenho fica dourado.
            val pasta = kind == Kind.Folder
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(corner))
                    .then(
                        if (pasta) Modifier.vidro(corner)
                        else Modifier
                            .background(Brush.linearGradient(listOf(cor.copy(alpha = 0.26f), cor.copy(alpha = 0.09f))))
                            .border(0.5.dp, cor.copy(alpha = 0.20f), RoundedCornerShape(corner))
                    ),
                contentAlignment = Alignment.Center,
            ) { Icon(Mime.icon(e), null, tint = cor, modifier = Modifier.size(if (pasta) 52.dp else 44.dp)) }
        }
        else Selo(cor, size, corner, forte = kind != Kind.Folder) {
            Icon(Mime.icon(e), null, tint = cor, modifier = Modifier.size(size * 0.54f))
        }
    }
    if (!podeMiniatura) { icone(); return }
    val mod = (if (fill) Modifier.fillMaxSize() else Modifier.size(size)).clip(RoundedCornerShape(corner))
    val model: Any = when (val l = e.loc) {
        is Loc.Local -> l.file; is Loc.Smb -> SmbProvider.uri(l); is Loc.Vault -> com.mauricio.libertyx.folder.vault.VaultProvider.uri(l)
    }
    Box(mod.border(0.5.dp, Lx.vidroBorda, RoundedCornerShape(corner))) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context).data(model).crossfade(true)
                // Miniatura decifrada nunca vai para o cache em disco.
                .apply { if (e.loc is Loc.Vault) diskCachePolicy(coil3.request.CachePolicy.DISABLED) }
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(corner)),
            loading = { icone() },
            error = { icone() },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    e: FileEntry, selecionando: Boolean, selecionado: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit, subtitulo: String? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(Lx.raioPequeno))
            .then(if (selecionado) Modifier.vidro(Lx.raioPequeno, Lx.ouro.copy(alpha = 0.12f), Lx.ouro.copy(alpha = 0.35f)) else Modifier)
            .tvFocus(radius = Lx.raioPequeno)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecionando) {
            Icon(
                if (selecionado) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                tint = if (selecionado) Lx.ouro else Lx.tenue,
                modifier = Modifier.padding(end = 12.dp).size(24.dp),
            )
        }
        Thumb(e, 44.dp)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(e.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium)
            val sub = subtitulo ?: listOfNotNull(
                Fmt.date(e.modified).ifEmpty { null },
                if (!e.isDir) Fmt.size(e.size) else null,
            ).joinToString("  ·  ")
            if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = Lx.tenue,
                lineHeight = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileCell(e: FileEntry, selecionando: Boolean, selecionado: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        Modifier.padding(3.dp).clip(RoundedCornerShape(Lx.raioPequeno))
            .then(if (selecionado) Modifier.vidro(Lx.raioPequeno, Lx.ouro.copy(alpha = 0.12f), Lx.ouro.copy(alpha = 0.35f)) else Modifier)
            .tvFocus(Lx.raioPequeno, grow = 1.05f)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(6.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            Thumb(e, 96.dp, corner = 14.dp, fill = true)
            if (selecionando) Icon(
                if (selecionado) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
                tint = if (selecionado) Lx.ouro else Color.White,
                modifier = Modifier.align(Alignment.TopStart).padding(6.dp).size(22.dp)
                    .background(Color.Black.copy(alpha = 0.35f), CircleShape),
            )
        }
        Text(e.name, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 12.sp, lineHeight = 15.sp,
            color = Lx.texto, modifier = Modifier.padding(top = 7.dp, start = 2.dp, end = 2.dp))
    }
}

/** Painel de progresso da cópia/movimentação, por cima de qualquer tela. */
@Composable
fun OpPanel(modifier: Modifier = Modifier) {
    val s by Ops.state.collectAsState()
    val op = s ?: return
    Box(
        modifier.fillMaxWidth().padding(12.dp)
            .background(Lx.superficie, RoundedCornerShape(Lx.raioCartao))
            .vidro(Lx.raioCartao, Lx.vidroForte)
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
                        color = if (op.erro != null) MaterialTheme.colorScheme.error else Lx.apagado,
                        maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
                if (op.terminou) TextButton(onClick = { Ops.dismiss() }, Modifier.tvFocus(24.dp)) { Text("OK") }
                else TextButton(onClick = { Ops.cancel() }, Modifier.tvFocus(24.dp)) { Text("Cancelar") }
            }
            if (!op.terminou) {
                Spacer(Modifier.height(10.dp))
                if (op.total > 0) LinearProgressIndicator(
                    progress = { op.fracao }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                    color = Lx.ouro, trackColor = Lx.vidroForte, gapSize = 0.dp, drawStopIndicator = {},
                )
                else LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(4.dp).clip(CircleShape), color = Lx.ouro, trackColor = Lx.vidroForte,
                )
            }
        }
    }
}

/** A barra de uso do armazenamento — em ouro, e vermelha quando aperta. */
@Composable
fun UsageBar(usado: Long, total: Long) {
    val f = if (total > 0) usado.toFloat() / total else 0f
    val cores = if (f > 0.9f) listOf(Color(0xFFFF8A5A), Lx.vermelho) else listOf(Lx.ouroClaro, Lx.ouroEscuro)
    Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Lx.vidroForte)) {
        Box(Modifier.fillMaxWidth(f.coerceIn(0f, 1f)).height(5.dp).clip(CircleShape)
            .background(Brush.horizontalGradient(cores)))
    }
}

@Composable
fun EmptyState(texto: String, sub: String? = null, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Column(Modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        if (icon != null) Box(
            Modifier.size(72.dp).clip(CircleShape).vidro(36.dp).padding(bottom = 0.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(32.dp), tint = Lx.tenue) }
        Text(texto, style = MaterialTheme.typography.titleMedium, color = Lx.apagado, modifier = Modifier.padding(top = 16.dp))
        if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = Lx.tenue,
            modifier = Modifier.padding(top = 6.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}

@Composable
fun RowIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, cor: Color) {
    Selo(cor, 42.dp, Lx.raioPequeno) { Icon(icon, null, tint = cor, modifier = Modifier.size(21.dp)) }
}
