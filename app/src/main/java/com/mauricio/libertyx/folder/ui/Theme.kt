package com.mauricio.libertyx.folder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta no espírito do One UI: fundo cinza-claro, cartões brancos bem
// arredondados, azul como destaque; no escuro, preto puro e cartões grafite.
private val Claro = lightColorScheme(
    primary = Color(0xFF0A6CFF),
    onPrimary = Color.White,
    secondary = Color(0xFF0A6CFF),
    background = Color(0xFFF4F4F6),
    onBackground = Color(0xFF111111),
    surface = Color(0xFFF4F4F6),
    onSurface = Color(0xFF111111),
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color.White,
    surfaceContainerHighest = Color(0xFFEAEAEE),
    onSurfaceVariant = Color(0xFF6E6E73),
    outlineVariant = Color(0xFFE2E2E6),
    error = Color(0xFFE5243B),
)

private val Escuro = darkColorScheme(
    primary = Color(0xFF4C9BFF),
    onPrimary = Color.White,
    secondary = Color(0xFF4C9BFF),
    background = Color.Black,
    onBackground = Color(0xFFF2F2F2),
    surface = Color.Black,
    onSurface = Color(0xFFF2F2F2),
    surfaceContainer = Color(0xFF17171A),
    surfaceContainerHigh = Color(0xFF232327),
    surfaceContainerHighest = Color(0xFF2C2C31),
    onSurfaceVariant = Color(0xFF9A9AA0),
    outlineVariant = Color(0xFF2A2A2E),
    error = Color(0xFFFF5A6A),
)

@Composable
fun ArquivosTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Escuro else Claro, content = content)
}
