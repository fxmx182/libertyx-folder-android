package com.mauricio.libertyx.folder.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.libertyx.folder.R
import com.mauricio.libertyx.folder.pro.Loja
import com.mauricio.libertyx.folder.pro.Pro
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * O guia de uso, feito para o controle remoto.
 *
 * No LibertyX Player o guia ensina gestos, e o dedo aprende fazendo. Aqui não
 * há gesto a ensinar: há ferramentas a mostrar, e a pessoa está no sofá a três
 * metros. Então cada página é uma cena do próprio app, encolhida e animada — o
 * destaque dourado anda sozinho, a seleção acontece, a barra sobe, o painel
 * some —, ao lado de um texto curto. A pessoa só assiste e aperta OK.
 *
 * Navegação: OK em "Próximo", ou as setas ◀ ▶ em qualquer lugar; Voltar
 * volta uma página (na primeira, pula o guia). Nunca prende.
 *
 * Aparece uma vez, antes do pedido de permissão (a última página avisa dele),
 * e pode ser revisto em Configurações › Guia de uso.
 */
@Composable
fun GuiaScreen(primeiraVez: Boolean = true, onFim: () -> Unit) {
    val paginas = remember { paginasDoGuia(primeiraVez) }
    var atual by remember { mutableIntStateOf(0) }
    val foco = remember { FocusRequester() }
    val ultima = atual == paginas.lastIndex

    fun ir(delta: Int) {
        val novo = atual + delta
        when {
            novo < 0 -> onFim()
            novo > paginas.lastIndex -> onFim()
            else -> atual = novo
        }
    }

    BackHandler { ir(-1) }
    LaunchedEffect(atual) { delay(60); runCatching { foco.requestFocus() } }

    Box(
        Modifier.fillMaxSize().statusBarsPadding()
            .onPreviewKeyEvent { e ->
                // ◀ ▶ mudam de página de qualquer lugar: os botões ficam em
                // coluna justamente para as setas laterais não serem deles.
                when (e.key) {
                    Key.DirectionRight -> { if (e.type == KeyEventType.KeyDown && !ultima) ir(1); true }
                    Key.DirectionLeft -> { if (e.type == KeyEventType.KeyDown && atual > 0) ir(-1); true }
                    else -> false
                }
            },
    ) {
        AnimatedContent(
            atual, label = "pagina",
            transitionSpec = {
                val frente = targetState > initialState
                (slideInHorizontally(tween(320)) { if (frente) it / 6 else -it / 6 } + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(tween(260)) { if (frente) -it / 6 else it / 6 } + fadeOut(tween(180)))
            },
        ) { i ->
            val p = paginas[i]
            Row(
                Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 40.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A cena: uma "tela" de vidro com o app encolhido dentro.
                Box(
                    Modifier.weight(0.56f).fillMaxHeight().padding(end = 28.dp)
                        .vidro(26.dp).clip(RoundedCornerShape(26.dp)),
                    contentAlignment = Alignment.Center,
                ) { p.cena() }

                Column(Modifier.weight(0.44f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1} / ${paginas.size}", style = Lx.tituloSecao)
                        if (p.pro) Text(
                            "PRO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.6.sp,
                            color = Color(0xFF1A1200),
                            modifier = Modifier.padding(start = 10.dp).clip(RoundedCornerShape(5.dp))
                                .background(Lx.ouro).padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                    Text(p.tituloComposto?.invoke() ?: stringResource(p.titulo), fontSize = 27.sp, lineHeight = 31.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.6).sp)
                    Text(p.texto(), fontSize = 15.sp, lineHeight = 21.sp, color = Lx.apagado)
                    if (p.teclas.isNotEmpty()) Column(Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        p.teclas.forEach { (tecla, oQue) -> LinhaDeTecla(stringResource(tecla), stringResource(oQue)) }
                    }
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { ir(1) },
                        colors = ButtonDefaults.buttonColors(containerColor = Lx.ouro, contentColor = Color(0xFF1A1200)),
                        modifier = Modifier.fillMaxWidth().height(48.dp).focusRequester(foco).tvFocus(24.dp),
                    ) { Text(stringResource(if (ultima) R.string.guia_comecar else R.string.guia_proximo), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp) }
                    if (!ultima) TextButton(onClick = onFim, modifier = Modifier.tvFocus(24.dp)) {
                        Text(stringResource(R.string.guia_pular), color = Lx.apagado)
                    }
                }
            }
        }

        // Os pontinhos do progresso, embaixo e no centro.
        Row(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            paginas.indices.forEach { i ->
                val largura by animateDpAsState(if (i == atual) 22.dp else 7.dp, label = "ponto")
                Box(Modifier.width(largura).height(7.dp).clip(CircleShape)
                    .background(if (i == atual) Lx.ouro else Lx.vidroForte))
            }
        }
    }
}

private class Pagina(
    @StringRes val titulo: Int,
    val texto: @Composable () -> String,
    val teclas: List<Pair<Int, Int>> = emptyList(),
    val pro: Boolean = false,
    /** Título com o número de dias (plural), em vez de [titulo]. */
    val tituloComposto: (@Composable () -> String)? = null,
    val cena: @Composable () -> Unit,
)

private fun paginasDoGuia(primeiraVez: Boolean): List<Pagina> = listOf(
    Pagina(
        R.string.guia1_t, { stringResource(R.string.guia1_x) },
        listOf(R.string.tecla_setas_lado to R.string.guia_t_muda_pagina,
            R.string.tecla_voltar to if (primeiraVez) R.string.guia_t_pula else R.string.guia_t_fecha),
    ) { CenaBoasVindas() },
    Pagina(
        R.string.guia2_t, { stringResource(R.string.guia2_x) },
        listOf(R.string.tecla_setas to R.string.guia_t_movem, R.string.tecla_ok to R.string.guia_t_abre,
            R.string.tecla_segurar_ok to R.string.guia_t_seleciona, R.string.tecla_voltar to R.string.guia_t_volta),
    ) { CenaControle() },
    Pagina(R.string.guia3_t, { stringResource(R.string.guia3_x) }) { CenaInicio() },
    Pagina(R.string.guia4_t, { stringResource(R.string.guia4_x) }) { CenaPastas() },
    Pagina(
        R.string.guia5_t, { stringResource(R.string.guia5_x) },
        listOf(R.string.tecla_segurar_ok to R.string.guia_t_comeca_sel, R.string.tecla_voltar to R.string.guia_t_desfaz_sel),
    ) { CenaSelecao() },
    Pagina(R.string.guia6_t, { stringResource(R.string.guia6_x) }) { CenaColar() },
    Pagina(R.string.guia7_t, { stringResource(R.string.guia7_x) }, pro = true) { CenaRede() },
    Pagina(R.string.guia8_t, { stringResource(R.string.guia8_x) }, pro = true) { CenaAssistir() },
    Pagina(R.string.guia9_t, { stringResource(R.string.guia9_x) }) { CenaPesquisa() },
    Pagina(
        0,
        {
            val s by Loja.situacao.collectAsState()
            val preco = (s as? Loja.Situacao.Pronta)?.preco
            val estado by Pro.estado.collectAsState()
            val permissao = if (primeiraVez) stringResource(R.string.guia10_permissao) else ""
            when {
                estado == Pro.Estado.Comprado -> stringResource(R.string.guia10_ativo) + permissao
                preco != null -> stringResource(R.string.guia10_x_preco, Pro.DIAS_DE_TESTE, preco) + permissao
                else -> stringResource(R.string.guia10_x, Pro.DIAS_DE_TESTE) + permissao
            }
        },
        pro = true,
        tituloComposto = { pluralStringResource(R.plurals.guia10_t, Pro.DIAS_DE_TESTE, Pro.DIAS_DE_TESTE) },
    ) { CenaPro() },
)

// ───────────────────────── peças das cenas ─────────────────────────

/** Avança por um roteiro de passos, cada um com a sua duração, em laço. */
@Composable
private fun rememberRoteiro(vararg duracoes: Long): Int {
    var passo by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) { delay(duracoes[passo]); passo = (passo + 1) % duracoes.size }
    }
    return passo
}

/** Uma tecla do controle desenhada, e o que ela faz. */
@Composable
private fun LinhaDeTecla(tecla: String, oQue: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            tecla, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Lx.texto,
            modifier = Modifier.width(116.dp).padding(end = 10.dp)
                .border(1.dp, Lx.vidroBorda, RoundedCornerShape(8.dp))
                .background(Lx.vidroForte, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
        )
        Text(oQue, fontSize = 13.sp, color = Lx.apagado)
    }
}

/** O realce de foco das cenas — o mesmo dourado do app, sem precisar de foco de verdade. */
private fun Modifier.focoDeMentira(ativo: Boolean, raio: Dp = 10.dp): Modifier =
    if (!ativo) this
    else this.border(2.dp, Lx.ouro, RoundedCornerShape(raio)).background(Lx.ouro.copy(alpha = 0.13f), RoundedCornerShape(raio))

/** Uma linha de arquivo de mentira, no tamanho das cenas. */
@Composable
private fun MiniLinha(
    icone: ImageVector, cor: Color, nome: String, sub: String,
    focada: Boolean = false, selecionando: Boolean = false, marcada: Boolean = false,
) {
    val fundo by animateColorAsState(if (marcada) Lx.ouro.copy(alpha = 0.10f) else Color.Transparent, label = "marca")
    Row(
        Modifier.fillMaxWidth().background(fundo, RoundedCornerShape(10.dp)).focoDeMentira(focada)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selecionando) Icon(
            if (marcada) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null,
            tint = if (marcada) Lx.ouro else Lx.tenue, modifier = Modifier.padding(end = 8.dp).size(16.dp),
        )
        Selo(cor, 26.dp, 8.dp, forte = icone != Icons.Filled.Folder) {
            Icon(icone, null, tint = cor, modifier = Modifier.size(15.dp))
        }
        Column(Modifier.padding(start = 10.dp)) {
            Text(nome, fontSize = 12.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 9.sp, lineHeight = 11.sp, color = Lx.tenue, maxLines = 1)
        }
    }
}

/** Uma moldura de "tela" para a cena, com um título no alto. */
@Composable
private fun TelaDeMentira(titulo: String?, modifier: Modifier = Modifier, conteudo: @Composable BoxScope.() -> Unit) {
    Column(modifier.fillMaxSize().padding(18.dp)) {
        if (titulo != null) Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(16.dp), tint = Lx.apagado)
            Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
        }
        Box(Modifier.fillMaxSize(), content = conteudo)
    }
}

/** A legenda que aparece embaixo da cena dizendo o que acabou de acontecer. */
@Composable
private fun BoxScope.Legenda(texto: String) {
    AnimatedContent(
        texto, label = "legenda", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
        transitionSpec = { (fadeIn(tween(200)) + slideInVertically { it / 2 }) togetherWith fadeOut(tween(150)) },
    ) { t ->
        Text(
            t, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Lx.ouroClaro,
            modifier = Modifier.background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(20.dp))
                .border(0.5.dp, Lx.ouro.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 5.dp),
        )
    }
}

// ───────────────────────── as cenas ─────────────────────────

@Composable
private fun CenaBoasVindas() {
    val t = rememberInfiniteTransition(label = "orbita")
    val angulo by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "ang")
    val brilho by t.animateFloat(0.14f, 0.28f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "brilho")
    val satelites = listOf(
        Icons.Filled.VideoFile to Color(0xFFFF6E8A), Icons.Filled.Image to Color(0xFFC08BFF),
        Icons.Filled.Computer to Lx.verde, Icons.Filled.Audiotrack to Color(0xFF35D0BE),
        Icons.Filled.Description to Color(0xFF5B9CFF), Icons.Filled.Dns to Lx.ouro,
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(Modifier.size(230.dp).background(Brush.radialGradient(listOf(Lx.ouro.copy(alpha = brilho), Color.Transparent)), CircleShape))
        // A órbita: uma elipse fina, como as fitas da marca.
        Canvas(Modifier.size(300.dp, 170.dp)) {
            drawOval(Lx.ouro.copy(alpha = 0.18f), style = Stroke(1.5.dp.toPx()))
        }
        satelites.forEachIndexed { i, (icone, cor) ->
            val a = (angulo + i * 60f) * (PI / 180f).toFloat()
            val x = cos(a) * 150f; val y = sin(a) * 85f
            // Atrás da marca na metade de cima da órbita: parece girar em volta.
            val frente = sin(a) > 0
            Box(
                Modifier.offset(x.dp, y.dp).scale(if (frente) 1f else 0.8f)
                    .graphicsLayer { alpha = if (frente) 1f else 0.55f },
            ) { Selo(cor, 36.dp, 11.dp) { Icon(icone, null, tint = cor, modifier = Modifier.size(19.dp)) } }
        }
        Image(painterResource(R.drawable.ic_marca), null, Modifier.size(104.dp))
    }
}

@Composable
private fun CenaControle() {
    // Roteiro: desce, desce, OK (abre), Voltar (volta), sobe.
    val passo = rememberRoteiro(1100, 1100, 1300, 1300, 1100)
    val tecla = when (passo) { 0, 1 -> "baixo"; 2 -> "ok"; 3 -> "voltar"; else -> "cima" }
    val focoNaLinha = when (passo) { 0 -> 1; 1, 2 -> 2; 3 -> 2; else -> 1 }
    val aberta = passo == 2
    Row(Modifier.fillMaxSize().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
        Controle(tecla, Modifier.padding(end = 22.dp))
        Box(Modifier.weight(1f).fillMaxHeight().vidro(16.dp).padding(12.dp)) {
            AnimatedContent(aberta, label = "abre") { abriu ->
                if (!abriu) Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.vol_interno), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    listOf(stringResource(R.string.g_download), stringResource(R.string.g_filmes), stringResource(R.string.g_fotos), stringResource(R.string.g_musicas)).forEachIndexed { i, n ->
                        MiniLinha(Icons.Filled.Folder, Lx.ouro, n, stringResource(R.string.g_pasta), focada = i == focoNaLinha)
                    }
                } else Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.g_fotos), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 4.dp))
                    MiniLinha(Icons.Filled.Image, Color(0xFFC08BFF), stringResource(R.string.g_praia), "2,4 MB", focada = true)
                    MiniLinha(Icons.Filled.Image, Color(0xFFC08BFF), stringResource(R.string.g_aniversario), "3,1 MB")
                    MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_festa), "184 MB")
                }
            }
            Legenda(when (passo) { 0, 1 -> stringResource(R.string.g_desce); 2 -> stringResource(R.string.g_ok_abre); 3 -> stringResource(R.string.g_voltar_volta); else -> stringResource(R.string.g_sobe) })
        }
    }
}

/** O controle remoto desenhado: anel de setas, OK no meio, Voltar e Início. */
@Composable
private fun Controle(acesa: String, modifier: Modifier = Modifier) {
    @Composable
    fun Tecla(nome: String, conteudo: @Composable () -> Unit, tamanho: Dp, mod: Modifier = Modifier) {
        val ligada = acesa == nome
        val cor by animateColorAsState(if (ligada) Lx.ouro else Color.White.copy(alpha = 0.08f), label = "tecla")
        val escala by animateFloatAsState(if (ligada) 0.9f else 1f, label = "aperto")
        Box(mod.size(tamanho).scale(escala).clip(CircleShape).background(cor), contentAlignment = Alignment.Center) { conteudo() }
    }
    val tinta = @Composable { nome: String -> if (acesa == nome) Color(0xFF1A1200) else Lx.apagado }
    Column(
        modifier.width(118.dp).clip(RoundedCornerShape(50.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF2A2A30), Color(0xFF151518))))
            .border(0.5.dp, Lx.vidroBorda, RoundedCornerShape(50.dp)).padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.KeyboardArrowUp, null, Modifier.align(Alignment.TopCenter).padding(top = 4.dp).size(22.dp),
                tint = if (acesa == "cima") Lx.ouro else Lx.apagado)
            Icon(Icons.Filled.KeyboardArrowDown, null, Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(22.dp),
                tint = if (acesa == "baixo") Lx.ouro else Lx.apagado)
            Icon(Icons.Filled.KeyboardArrowLeft, null, Modifier.align(Alignment.CenterStart).padding(start = 4.dp).size(22.dp), tint = Lx.apagado)
            Icon(Icons.Filled.KeyboardArrowRight, null, Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).size(22.dp), tint = Lx.apagado)
            Tecla("ok", { Text("OK", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = tinta("ok")) }, 38.dp)
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Tecla("voltar", { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(16.dp), tint = tinta("voltar")) }, 32.dp)
            Tecla("inicio", { Icon(Icons.Filled.Home, null, Modifier.size(16.dp), tint = tinta("inicio")) }, 32.dp)
        }
        Box(Modifier.padding(top = 14.dp).size(36.dp, 4.dp).clip(CircleShape).background(Lx.vidroForte))
        Box(Modifier.padding(top = 8.dp).size(36.dp, 4.dp).clip(CircleShape).background(Lx.vidroForte))
    }
}

@Composable
private fun CenaInicio() {
    val passo = rememberRoteiro(1500, 1700, 1500, 1700)
    TelaDeMentira(null) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_marca), null, Modifier.size(22.dp))
                Text(buildAnnotatedString { append("LibertyX"); withStyle(SpanStyle(color = Lx.ouro)) { append(" Folder") } },
                    fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(start = 8.dp))
            }
            Row(
                Modifier.fillMaxWidth().vidro(18.dp).focoDeMentira(passo == 0, 18.dp).padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, null, Modifier.size(14.dp), tint = Lx.apagado)
                Text(stringResource(R.string.pesquisar_aparelho), fontSize = 11.sp, color = Lx.tenue, modifier = Modifier.padding(start = 8.dp))
            }
            Text(stringResource(R.string.sec_categorias).uppercase(), style = Lx.tituloSecao.copy(fontSize = 8.sp))
            Row(
                Modifier.fillMaxWidth().vidro(14.dp).focoDeMentira(passo == 1, 14.dp).padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf(
                    Icons.Filled.Image to Color(0xFFC08BFF), Icons.Filled.VideoLibrary to Color(0xFFFF6E8A),
                    Icons.Filled.Audiotrack to Color(0xFF35D0BE), Icons.Filled.Description to Color(0xFF5B9CFF),
                    Icons.Filled.Download to Lx.ouro, Icons.Filled.Android to Color(0xFF8BD962), Icons.Filled.FolderZip to Color(0xFFD9A15C),
                ).forEach { (i, c) -> Selo(c, 24.dp, 7.dp) { Icon(i, null, tint = c, modifier = Modifier.size(13.dp)) } }
            }
            Text(stringResource(R.string.sec_armazenamento).uppercase(), style = Lx.tituloSecao.copy(fontSize = 8.sp))
            Row(
                Modifier.fillMaxWidth().vidro(14.dp).focoDeMentira(passo == 2, 14.dp).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Selo(Lx.ouro, 24.dp, 7.dp) { Icon(Icons.Filled.Tv, null, tint = Lx.ouro, modifier = Modifier.size(13.dp)) }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(stringResource(R.string.vol_interno), fontSize = 11.sp)
                    Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(3.dp).clip(CircleShape).background(Lx.vidroForte)) {
                        Box(Modifier.fillMaxWidth(0.62f).height(3.dp).background(Brush.horizontalGradient(listOf(Lx.ouroClaro, Lx.ouroEscuro))))
                    }
                }
            }
            Text(stringResource(R.string.sec_rede).uppercase(), style = Lx.tituloSecao.copy(fontSize = 8.sp))
            Row(
                Modifier.fillMaxWidth().vidro(14.dp).focoDeMentira(passo == 3, 14.dp).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Selo(Lx.verde, 24.dp, 7.dp) { Icon(Icons.Filled.Computer, null, tint = Lx.verde, modifier = Modifier.size(13.dp)) }
                Text(stringResource(R.string.rede_achado_em, stringResource(R.string.g_pc)), fontSize = 11.sp, modifier = Modifier.padding(start = 10.dp))
            }
        }
        Legenda(when (passo) { 0 -> stringResource(R.string.g_l_pesquisa); 1 -> stringResource(R.string.g_l_categorias); 2 -> stringResource(R.string.g_l_tv_pendrive); else -> stringResource(R.string.g_l_computadores) })
    }
}

@Composable
private fun CenaPastas() {
    // Lista → foco na grade → vira grade → foco no ⋮ → menu → volta.
    val passo = rememberRoteiro(1300, 900, 1600, 900, 2000)
    val grade = passo in 2..4
    val focoIcone = when (passo) { 1 -> "grade"; 3, 4 -> "mais"; else -> null }
    val itens = listOf(
        Triple(Icons.Filled.Folder, Lx.ouro, stringResource(R.string.g_series)), Triple(Icons.Filled.Folder, Lx.ouro, stringResource(R.string.g_shows)),
        Triple(Icons.Filled.VideoFile, Color(0xFFFF6E8A), "trailer.mkv"), Triple(Icons.Filled.Image, Color(0xFFC08BFF), stringResource(R.string.g_capa)),
        Triple(Icons.Filled.Audiotrack, Color(0xFF35D0BE), stringResource(R.string.g_tema)), Triple(Icons.Filled.Description, Color(0xFF5B9CFF), stringResource(R.string.g_notas)),
    )
    Box(Modifier.fillMaxSize().padding(18.dp)) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(16.dp), tint = Lx.apagado)
                Text(stringResource(R.string.g_filmes), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp).weight(1f))
                Icon(Icons.Filled.Search, null, Modifier.size(16.dp), tint = Lx.apagado)
                Box(Modifier.padding(start = 10.dp).focoDeMentira(focoIcone == "grade", 8.dp).padding(3.dp)) {
                    Icon(if (grade) Icons.Filled.ViewList else Icons.Filled.GridView, null, Modifier.size(16.dp), tint = Lx.apagado)
                }
                Box(Modifier.padding(start = 6.dp).focoDeMentira(focoIcone == "mais", 8.dp).padding(3.dp)) {
                    Icon(Icons.Filled.MoreVert, null, Modifier.size(16.dp), tint = Lx.apagado)
                }
            }
            // A trilha: o caminho em pastilhas.
            Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf(stringResource(R.string.sec_armazenamento), stringResource(R.string.g_videos_pasta), stringResource(R.string.g_filmes)).forEachIndexed { i, n ->
                    val ultima = i == 2
                    Text(n, fontSize = 9.sp, color = if (ultima) Lx.ouro else Lx.apagado,
                        modifier = Modifier.then(if (ultima) Modifier.vidro(8.dp, Lx.ouro.copy(alpha = 0.10f), Lx.ouro.copy(alpha = 0.3f)) else Modifier.vidro(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp))
                    if (!ultima) Icon(Icons.Filled.ChevronRight, null, Modifier.size(11.dp), tint = Lx.tenue)
                }
            }
            AnimatedContent(grade, label = "grade") { emGrade ->
                if (!emGrade) Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    itens.take(5).forEach { (i, c, n) -> MiniLinha(i, c, n, stringResource(R.string.g_data_tam)) }
                } else Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itens.chunked(3).forEach { linha ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            linha.forEach { (i, c, n) ->
                                Column(Modifier.weight(1f)) {
                                    Box(
                                        Modifier.fillMaxWidth().height(62.dp).then(
                                            if (i == Icons.Filled.Folder) Modifier.vidro(10.dp)
                                            else Modifier.background(Brush.linearGradient(listOf(c.copy(alpha = 0.26f), c.copy(alpha = 0.08f))), RoundedCornerShape(10.dp))
                                        ),
                                        contentAlignment = Alignment.Center,
                                    ) { Icon(i, null, tint = c, modifier = Modifier.size(24.dp)) }
                                    Text(n, fontSize = 9.sp, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        // O menu ⋮ Mais.
        AnimatedVisibility(passo == 4, Modifier.align(Alignment.TopEnd).padding(top = 26.dp),
            enter = fadeIn() + scaleIn(initialScale = 0.9f), exit = fadeOut()) {
            Column(Modifier.width(150.dp).background(Lx.superficie, RoundedCornerShape(12.dp)).vidro(12.dp).padding(vertical = 6.dp)) {
                listOf(stringResource(R.string.nova_pasta), stringResource(R.string.selecionar), stringResource(R.string.ordenar_exibir), stringResource(R.string.atualizar), stringResource(R.string.inicio)).forEachIndexed { i, t ->
                    Text(t, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().focoDeMentira(i == 2, 6.dp).padding(horizontal = 12.dp, vertical = 5.dp))
                }
            }
        }
        Legenda(when (passo) { 0 -> stringResource(R.string.g_l_trilha); 1, 2 -> stringResource(R.string.g_l_lista_grade); else -> stringResource(R.string.g_l_mais) })
    }
}

@Composable
private fun CenaSelecao() {
    // foco → segura OK (anel enche) → marcada → desce → OK → 2 marcadas → barra → anda na barra.
    val passo = rememberRoteiro(900, 1300, 900, 1000, 1100, 900, 900, 900, 1300)
    val selecionando = passo >= 2
    val marcadas = when { passo >= 4 -> setOf(1, 2); passo >= 2 -> setOf(1); else -> emptySet() }
    val foco = when { passo <= 2 -> 1; passo <= 4 -> 2; else -> -1 }
    val focoBarra = when (passo) { 5 -> 0; 6 -> 1; 7 -> 2; 8 -> 3; else -> -1 }
    val segurando = passo == 1
    val anel by animateFloatAsState(if (segurando) 1f else 0f, tween(if (segurando) 1200 else 1), label = "anel")
    val itens = listOf(
        Triple(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_episodio, 1)), Triple(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_episodio, 2)),
        Triple(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_episodio, 3)), Triple(Icons.Filled.Description, Color(0xFF5B9CFF), stringResource(R.string.g_legendas)),
    )
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Text(if (selecionando) pluralStringResource(R.plurals.n_selecionados, marcadas.size, marcadas.size) else stringResource(R.string.g_series), fontSize = 15.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                itens.forEachIndexed { i, (ic, c, n) ->
                    Box {
                        MiniLinha(ic, c, n, "1,2 GB", focada = i == foco, selecionando = selecionando, marcada = i in marcadas)
                        // O anel de "segurando OK" enchendo em volta da linha.
                        if (i == 1 && anel > 0f) Canvas(Modifier.matchParentSize()) {
                            drawRoundRect(
                                Lx.ouroClaro, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()),
                                style = Stroke(3.dp.toPx()), alpha = anel,
                            )
                        }
                    }
                }
            }
        }
        // A barra de seleção subindo de baixo.
        AnimatedVisibility(selecionando, Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut()) {
            Row(
                Modifier.fillMaxWidth().background(Lx.superficie).drawBehind {
                    drawLine(Lx.vidroBorda, Offset(0f, 0f), Offset(size.width, 0f), 1f)
                }.padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf(
                    Icons.Filled.DriveFileMove to stringResource(R.string.mover), Icons.Filled.ContentCopy to stringResource(R.string.copiar),
                    Icons.Filled.Share to stringResource(R.string.compartilhar), Icons.Filled.Delete to stringResource(R.string.excluir), Icons.Filled.MoreVert to stringResource(R.string.mais),
                ).forEachIndexed { i, (ic, t) ->
                    Column(Modifier.focoDeMentira(i == focoBarra, 8.dp).padding(horizontal = 6.dp, vertical = 3.dp),
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(ic, null, Modifier.size(16.dp), tint = Lx.texto)
                        Text(t, fontSize = 8.sp)
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().padding(bottom = if (selecionando) 46.dp else 0.dp)) {
            Legenda(when (passo) { 0 -> stringResource(R.string.g_l_destaque); 1 -> stringResource(R.string.g_l_segurando); 2, 3 -> stringResource(R.string.g_l_selecionado); 4 -> stringResource(R.string.g_l_marca_outro); else -> stringResource(R.string.g_l_acoes) })
        }
    }
}

@Composable
private fun CenaColar() {
    // barra de colar → troca de pasta → foco em "Copiar para cá" → progresso → concluído → some.
    val passo = rememberRoteiro(1300, 1300, 900, 1800, 1300, 900)
    val noDestino = passo >= 1
    val progresso by animateFloatAsState(if (passo >= 3) 1f else 0f, tween(if (passo == 3) 1700 else 1), label = "prog")
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(noDestino, label = "pasta") { dest ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Selo(if (dest) Lx.verde else Lx.ouro, 22.dp, 7.dp) {
                            Icon(if (dest) Icons.Filled.Computer else Icons.Filled.Tv, null, tint = if (dest) Lx.verde else Lx.ouro, modifier = Modifier.size(12.dp))
                        }
                        Text(if (dest) stringResource(R.string.g_pc) + " › " + stringResource(R.string.g_filmes) else stringResource(R.string.g_download), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            AnimatedContent(noDestino, label = "lista") { dest ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (!dest) {
                        MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_show), "2,1 GB")
                        MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), "making-of.mp4", "640 MB")
                    } else {
                        MiniLinha(Icons.Filled.Folder, Lx.ouro, stringResource(R.string.g_classicos), stringResource(R.string.g_pasta))
                        MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_documentario), "3,4 GB")
                        if (passo >= 4) {
                            MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_show), "2,1 GB" + stringResource(R.string.g_novo))
                            MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), "making-of.mp4", "640 MB" + stringResource(R.string.g_novo))
                        }
                    }
                }
            }
        }
        // Barra de colar (passos 0–2) e o painel de progresso (3–4).
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            AnimatedVisibility(passo in 3..4, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut(tween(400))) {
                Column(
                    Modifier.padding(10.dp).fillMaxWidth().background(Lx.superficie, RoundedCornerShape(14.dp)).vidro(14.dp, Lx.vidroForte)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                ) {
                    Text(if (passo == 3) stringResource(R.string.op_copiando) + "…" else stringResource(R.string.op_concluido), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (passo == 3) stringResource(R.string.op_n_de_m, 2, 2) + " · " + stringResource(R.string.op_tam_de, String.format(java.util.Locale.getDefault(), "%.1f GB", progresso * 2.7f), String.format(java.util.Locale.getDefault(), "%.1f GB", 2.7f))
                        else stringResource(R.string.g_some),
                        fontSize = 9.sp, color = Lx.apagado)
                    if (passo == 3) Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(3.dp).clip(CircleShape).background(Lx.vidroForte)) {
                        Box(Modifier.fillMaxWidth(progresso).height(3.dp).background(Lx.ouro))
                    }
                }
            }
            AnimatedVisibility(passo <= 2, enter = fadeIn(), exit = fadeOut()) {
                Column(Modifier.fillMaxWidth().background(Lx.superficie).padding(vertical = 6.dp)) {
                    Text(pluralStringResource(R.plurals.colar_copiar, 2, 2), fontSize = 9.sp, color = Lx.apagado, modifier = Modifier.padding(horizontal = 14.dp))
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Text(stringResource(R.string.cancelar), fontSize = 11.sp, color = Lx.ouro, modifier = Modifier.padding(6.dp))
                        Text(stringResource(R.string.copiar_para_ca), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Lx.ouro,
                            modifier = Modifier.focoDeMentira(passo == 2, 8.dp).padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().padding(bottom = 64.dp)) {
            Legenda(when (passo) { 0 -> stringResource(R.string.g_l_abra_destino); 1 -> stringResource(R.string.g_l_outro_pc); 2 -> stringResource(R.string.copiar_para_ca); 3 -> stringResource(R.string.g_l_progresso); else -> stringResource(R.string.g_l_pronto) })
        }
    }
}

@Composable
private fun CenaRede() {
    // Radar varrendo; os computadores aparecem um a um; depois o cartão de entrar.
    val passo = rememberRoteiro(900, 900, 900, 1500, 2200)
    val t = rememberInfiniteTransition(label = "radar")
    val onda by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "onda")
    val achados = listOf(
        Triple(stringResource(R.string.g_pc), Icons.Filled.Computer, Offset(-118f, -62f)),
        Triple(stringResource(R.string.g_nas), Icons.Filled.Dns, Offset(122f, -40f)),
        Triple(stringResource(R.string.g_roteador), Icons.Filled.Router, Offset(-96f, 74f)),
    )
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(300.dp)) {
            val c = center
            for (k in 0 until 3) {
                val f = (onda + k / 3f) % 1f
                drawCircle(Lx.verde.copy(alpha = (1f - f) * 0.45f), radius = size.minDimension / 2 * f, center = c, style = Stroke(1.5.dp.toPx()))
            }
        }
        Selo(Lx.ouro, 46.dp, 14.dp) { Icon(Icons.Filled.Tv, null, tint = Lx.ouro, modifier = Modifier.size(24.dp)) }
        achados.forEachIndexed { i, (nome, icone, pos) ->
            AnimatedVisibility(passo >= i, Modifier.offset(pos.x.dp, pos.y.dp), enter = scaleIn() + fadeIn(), exit = fadeOut()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Selo(Lx.verde, 36.dp, 11.dp) { Icon(icone, null, tint = Lx.verde, modifier = Modifier.size(19.dp)) }
                    Text(nome, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 3.dp))
                    Text(stringResource(R.string.g_encontrado), fontSize = 8.sp, color = Lx.tenue)
                }
            }
        }
        // O cartão de entrar no servidor.
        AnimatedVisibility(passo == 4, enter = fadeIn() + scaleIn(initialScale = 0.92f), exit = fadeOut()) {
            Column(
                Modifier.width(230.dp).background(Lx.superficie, RoundedCornerShape(16.dp)).vidro(16.dp, Lx.vidroForte).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Selo(Lx.verde, 24.dp, 8.dp) { Icon(Icons.Filled.Computer, null, tint = Lx.verde, modifier = Modifier.size(13.dp)) }
                    Text(stringResource(R.string.g_pc), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
                listOf(stringResource(R.string.srv_usuario) to "maria", stringResource(R.string.srv_senha) to "••••••").forEachIndexed { i, (rotulo, valor) ->
                    Column(Modifier.fillMaxWidth().border(1.dp, if (i == 0) Lx.ouro else Lx.vidroBorda, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text(rotulo, fontSize = 7.sp, color = if (i == 0) Lx.ouro else Lx.tenue)
                        Text(valor, fontSize = 11.sp)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.RadioButtonUnchecked, null, Modifier.size(12.dp), tint = Lx.tenue)
                    Text(stringResource(R.string.srv_convidado), fontSize = 9.sp, color = Lx.apagado, modifier = Modifier.padding(start = 6.dp))
                }
            }
        }
        Box(Modifier.fillMaxSize()) {
            Legenda(if (passo < 3) stringResource(R.string.g_l_procurando) else if (passo == 3) stringResource(R.string.g_l_tres) else stringResource(R.string.g_l_usuario_senha))
        }
    }
}

@Composable
private fun CenaAssistir() {
    // Arquivo do servidor com o foco → OK → abre no player, já tocando, e pula +10 s.
    val passo = rememberRoteiro(1300, 2200, 1600)
    val tocando = passo >= 1
    val t = rememberInfiniteTransition(label = "filme")
    val luz by t.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "luz")
    val tempo by animateFloatAsState(if (passo == 2) 0.58f else if (passo == 1) 0.41f else 0.32f, tween(if (passo == 0) 1 else 1400), label = "tempo")
    Box(Modifier.fillMaxSize()) {
        AnimatedContent(tocando, label = "abre", transitionSpec = { fadeIn(tween(350)) + scaleIn(initialScale = 0.94f) togetherWith fadeOut(tween(200)) }) { abriu ->
            if (!abriu) Column(Modifier.fillMaxSize().padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 10.dp)) {
                    Selo(Lx.verde, 22.dp, 7.dp) { Icon(Icons.Filled.Lan, null, tint = Lx.verde, modifier = Modifier.size(12.dp)) }
                    Text(stringResource(R.string.g_nas) + " › " + stringResource(R.string.g_filmes), fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_viagem), "18,6 GB", focada = true)
                    MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_festa_ano), "2,3 GB")
                    MiniLinha(Icons.Filled.Audiotrack, Color(0xFF35D0BE), stringResource(R.string.g_trilha), "312 MB")
                }
            } else Box(
                // O "filme": um céu de fim de tarde que se mexe devagar.
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF1B2B4A), Color(0xFFB8643A).copy(alpha = 0.8f + 0.2f * luz), Color(0xFF2A1A12)))
                ),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val sol = Offset(size.width * (0.3f + 0.05f * luz), size.height * 0.56f)
                    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFD58A).copy(alpha = 0.55f), Color.Transparent), sol, size.minDimension * 0.32f),
                        radius = size.minDimension * 0.32f, center = sol)
                    drawCircle(Color(0xFFFFE2A8), radius = size.minDimension * 0.07f, center = sol)
                    // Dois morros em camadas: o de trás mais claro, como na névoa.
                    fun morro(base: Float, amp: Float, fase: Float, cor: Color) {
                        val p = androidx.compose.ui.graphics.Path()
                        p.moveTo(0f, size.height)
                        var x = 0f
                        while (x <= size.width) {
                            p.lineTo(x, size.height * base - amp * size.height * sin(x / size.width * 6.3f + fase))
                            x += 8f
                        }
                        p.lineTo(size.width, size.height); p.close()
                        drawPath(p, cor)
                    }
                    morro(0.66f, 0.05f, 0.4f, Color(0xFF4A2A22))
                    morro(0.76f, 0.04f, 2.1f, Color(0xFF1C1310))
                }
                Text(stringResource(R.string.g_viagem), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(14.dp))
                if (passo == 2) Text("+10 s", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Color.White,
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 40.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape).padding(14.dp))
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f))) {
                        Box(Modifier.fillMaxWidth(tempo).height(3.dp).background(Lx.ouro))
                    }
                    Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(14.dp), tint = Color.White)
                        Text(stringResource(R.string.g_lendo), fontSize = 8.sp, color = Color.White.copy(alpha = 0.75f), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
        Box(Modifier.fillMaxSize().padding(bottom = if (tocando) 46.dp else 0.dp)) {
            Legenda(when (passo) { 0 -> stringResource(R.string.g_l_ok_video); 1 -> stringResource(R.string.g_l_abre_player); else -> stringResource(R.string.g_l_avancar) })
        }
    }
}

@Composable
private fun CenaPesquisa() {
    // Digita "férias" letra a letra; os resultados aparecem, inclusive do servidor.
    val palavra = stringResource(R.string.g_palavra)
    val passo = rememberRoteiro(*(LongArray(palavra.length + 1) { 260L }.also { it[0] = 900 } + longArrayOf(2600)))
    val letras = passo.coerceAtMost(palavra.length)
    val mostrar = passo > palavra.length
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().vidro(18.dp).focoDeMentira(true, 18.dp).padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Search, null, Modifier.size(15.dp), tint = Lx.apagado)
            Text(palavra.take(letras) + if (!mostrar) "|" else "", fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
            Icon(Icons.Filled.SubdirectoryArrowRight, null, Modifier.size(15.dp), tint = if (mostrar) Lx.ouro else Lx.tenue)
        }
        AnimatedVisibility(mostrar, enter = fadeIn(tween(300)) + slideInVertically { it / 6 }, exit = fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(stringResource(R.string.g_resultados).uppercase(), style = Lx.tituloSecao.copy(fontSize = 8.sp), modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
                MiniLinha(Icons.Filled.Folder, Lx.ouro, stringResource(R.string.g_ferias_pasta), "Pictures")
                MiniLinha(Icons.Filled.Image, Color(0xFFC08BFF), stringResource(R.string.g_ferias_praia), "Pictures/" + stringResource(R.string.g_ferias_pasta))
                MiniLinha(Icons.Filled.VideoFile, Color(0xFFFF6E8A), stringResource(R.string.g_ferias_serra), stringResource(R.string.g_pc) + " › " + stringResource(R.string.g_videos_2024))
                MiniLinha(Icons.Filled.Image, Color(0xFFC08BFF), stringResource(R.string.g_ferias_familia), stringResource(R.string.g_nas) + " › " + stringResource(R.string.g_fotos))
            }
        }
    }
}

@Composable
private fun CenaPro() {
    val estado by Pro.estado.collectAsState()
    val situacao by Loja.situacao.collectAsState()
    val passo = rememberRoteiro(700, 700, 700, 700, 2400)
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_marca), null, Modifier.size(54.dp))
            Column(Modifier.padding(start = 14.dp)) {
                Text(
                    when (val e = estado) {
                        Pro.Estado.Comprado -> stringResource(R.string.g_pro_ativo)
                        is Pro.Estado.Teste -> pluralStringResource(R.plurals.g_dias_gratis, e.diasRestantes, e.diasRestantes)
                        Pro.Estado.Expirado -> stringResource(R.string.g_teste_encerrado)
                    },
                    fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Lx.ouro,
                )
                Text(
                    (situacao as? Loja.Situacao.Pronta)?.let { stringResource(R.string.g_depois_preco, it.preco) } ?: stringResource(R.string.g_depois),
                    fontSize = 12.sp, color = Lx.apagado,
                )
            }
        }
        Column(Modifier.padding(top = 16.dp).fillMaxWidth().vidro(16.dp).padding(vertical = 6.dp)) {
            VantagensDoPro.forEachIndexed { i, (icone, texto) ->
                AnimatedVisibility(passo >= i, enter = fadeIn(tween(300)) + slideInHorizontally { -it / 8 }, exit = fadeOut()) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Selo(Lx.ouro, 26.dp, 8.dp) { Icon(icone, null, tint = Lx.ouro, modifier = Modifier.size(14.dp)) }
                        Text(stringResource(texto), fontSize = 11.sp, modifier = Modifier.padding(start = 10.dp))
                    }
                }
            }
        }
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Selo(Lx.apagado, 20.dp, 6.dp) { Icon(Icons.Filled.Tv, null, tint = Lx.apagado, modifier = Modifier.size(11.dp)) }
            Text(stringResource(R.string.g_gratis_sempre), fontSize = 10.sp, color = Lx.tenue, modifier = Modifier.padding(start = 8.dp))
        }
    }
}
