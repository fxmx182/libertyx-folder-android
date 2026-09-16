package com.mauricio.libertyx.folder.ui

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mauricio.libertyx.folder.App

object Device {
    /** TV pelo modo de interface que o sistema declara (não pelo tamanho da tela); leanback cobre os antigos. */
    val isTv: Boolean by lazy {
        val c = App.instance
        val modo = c.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        modo?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            c.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }
}

/**
 * Realce de foco — a três metros, com um controle remoto, é a única coisa que
 * diz onde você está. Exagerado de propósito: borda na cor de destaque, fundo
 * tingido e, em células, um leve crescimento. No toque nada disso aparece.
 *
 * Tem de vir **antes** do `clickable` na cadeia: `onFocusChanged` só enxerga o
 * foco de quem está dentro dele.
 */
fun Modifier.tvFocus(radius: Dp = 16.dp, grow: Float = 1f): Modifier = composed {
    var focado by remember { mutableStateOf(false) }
    val destaque = MaterialTheme.colorScheme.primary
    val escala by animateFloatAsState(if (focado) grow else 1f, label = "foco-escala")
    val borda by animateColorAsState(if (focado) destaque else Color.Transparent, label = "foco-borda")
    val forma = RoundedCornerShape(radius)
    this
        .onFocusChanged { focado = it.hasFocus }
        .scale(escala)
        .border(if (focado) 3.dp else 0.dp, borda, forma)
        .background(if (focado) destaque.copy(alpha = 0.12f) else Color.Transparent, forma)
}

/**
 * Num formulário, cima/baixo trocam de campo. Sem isso o controle remoto entra
 * no primeiro campo e não sai mais (as setas ficam com o cursor). Lição do
 * LibertyX Player: `Next`/`Previous` em vez de `Down`/`Up`, porque a busca
 * geométrica prende o foco na linha da caixa de seleção.
 */
fun Modifier.setasTrocamDeCampo(): Modifier = composed {
    val foco = LocalFocusManager.current
    onPreviewKeyEvent { e ->
        if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (e.key) {
            Key.DirectionDown -> foco.moveFocus(FocusDirection.Next)
            Key.DirectionUp -> foco.moveFocus(FocusDirection.Previous)
            else -> false
        }
    }
}
