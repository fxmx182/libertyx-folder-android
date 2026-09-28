package com.mauricio.libertyx.folder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.libertyx.folder.R

/**
 * A linguagem visual do app — a mesma família do LibertyX Player.
 *
 * Ouro sobre preto quente, superfícies de vidro (um branco quase transparente
 * com a borda um pouco mais viva, que é o que dá a impressão de espessura),
 * cantos generosos e tipografia apertada. O app é escuro sempre, e não só à
 * noite: a marca é ouro, e ouro sobre fundo claro vira mostarda. O tema claro
 * do Material, que o app usava antes, deixava a identidade se perder.
 *
 * A cor não é enfeite aqui: cada tipo de arquivo tem a sua, e a pasta é a cor
 * da marca — o ícone do app é uma pasta dourada.
 */
object Lx {
    /** O amarelo da marca, colhido do ícone. */
    val ouro = Color(0xFFFBC501)
    val ouroClaro = Color(0xFFFFE08A)
    val ouroEscuro = Color(0xFFB97A06)

    val verde = Color(0xFF35D0BE)
    val vermelho = Color(0xFFFF5A5A)

    /** Os três níveis de texto. */
    val texto = Color.White.copy(alpha = 0.94f)
    val apagado = Color(0xFFEBEBF5).copy(alpha = 0.60f)
    val tenue = Color(0xFFEBEBF5).copy(alpha = 0.38f)

    val vidro = Color.White.copy(alpha = 0.055f)
    val vidroForte = Color.White.copy(alpha = 0.10f)
    val vidroBorda = Color.White.copy(alpha = 0.11f)

    /** Preto com um resto de marrom, o mesmo do fundo do ícone. */
    val fundo = Color(0xFF0C0B0A)
    val superficie = Color(0xFF16151A)

    val raioCartao = 22.dp
    val raioPequeno = 14.dp

    /**
     * A letra do app, a mesma do Player: a Roboto do sistema é a letra de todo
     * app de Android e de nenhum em particular. A Manrope tem números de
     * largura constante — tamanhos e datas alinham sozinhos numa lista.
     */
    val fonte = FontFamily(
        Font(R.font.manrope_regular, FontWeight.Normal),
        Font(R.font.manrope_medium, FontWeight.Medium),
        Font(R.font.manrope_semibold, FontWeight.SemiBold),
        Font(R.font.manrope_bold, FontWeight.Bold),
        Font(R.font.manrope_extrabold, FontWeight.ExtraBold),
    )

    /** Título de seção: pequeno, em caixa alta e espaçado — o contrário do
     *  corpo, que é apertado. A diferença entre os dois separa as seções sem
     *  precisar de linha divisória. */
    val tituloSecao = TextStyle(
        fontFamily = fonte, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp, color = tenue,
    )

    /** O nome do app na tela inicial — pesado e apertado, como capa de revista. */
    val titulao = TextStyle(
        fontFamily = fonte, fontSize = 30.sp, lineHeight = 34.sp,
        fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.8).sp, color = texto,
    )
}

/** Uma superfície de vidro: cartão, linha, painel. */
fun Modifier.vidro(raio: Dp = Lx.raioCartao, cor: Color = Lx.vidro, borda: Color = Lx.vidroBorda): Modifier =
    background(cor, RoundedCornerShape(raio)).border(0.5.dp, borda, RoundedCornerShape(raio))

/**
 * A luz de cima: um brilho dourado bem fraco atrás de tudo, no alto à
 * esquerda, e uma segunda luz fria no rodapé. Sem isso o preto fica chapado e
 * o vidro não parece vidro — é a única coisa que dá profundidade à tela.
 */
fun Modifier.brilhoDeFundo(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            listOf(Lx.ouro.copy(alpha = 0.11f), Color.Transparent),
            center = Offset(size.width * 0.12f, -size.height * 0.04f),
            radius = size.width * 1.15f,
        )
    )
    drawRect(
        Brush.radialGradient(
            listOf(Color(0xFF3A2E6E).copy(alpha = 0.13f), Color.Transparent),
            center = Offset(size.width * 1.05f, size.height * 1.02f),
            radius = size.width * 0.95f,
        )
    )
}

private val Escuro = darkColorScheme(
    primary = Lx.ouro,
    onPrimary = Color(0xFF1A1200),
    secondary = Lx.ouroClaro,
    tertiary = Lx.verde,
    background = Lx.fundo,
    onBackground = Lx.texto,
    surface = Lx.fundo,
    onSurface = Lx.texto,
    surfaceContainer = Lx.superficie,
    surfaceContainerHigh = Color(0xFF1E1D22),
    surfaceContainerHighest = Color(0xFF26252B),
    surfaceVariant = Lx.vidro,
    onSurfaceVariant = Lx.apagado,
    outline = Lx.vidroBorda,
    outlineVariant = Lx.vidroBorda,
    error = Lx.vermelho,
)

@Composable
fun ArquivosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Escuro, typography = tipografia(), content = content)
}

/** Todos os estilos do Material com a letra do app — assim nenhum texto escapa. */
private fun tipografia(): Typography {
    val base = Typography()
    fun TextStyle.nossa() = copy(fontFamily = Lx.fonte)
    return Typography(
        displayLarge = base.displayLarge.nossa(), displayMedium = base.displayMedium.nossa(),
        displaySmall = base.displaySmall.nossa(), headlineLarge = base.headlineLarge.nossa(),
        headlineMedium = base.headlineMedium.nossa(), headlineSmall = base.headlineSmall.nossa(),
        titleLarge = base.titleLarge.nossa(), titleMedium = base.titleMedium.nossa(),
        titleSmall = base.titleSmall.nossa(), bodyLarge = base.bodyLarge.nossa(),
        bodyMedium = base.bodyMedium.nossa(), bodySmall = base.bodySmall.nossa(),
        labelLarge = base.labelLarge.nossa(), labelMedium = base.labelMedium.nossa(),
        labelSmall = base.labelSmall.nossa(),
    )
}
