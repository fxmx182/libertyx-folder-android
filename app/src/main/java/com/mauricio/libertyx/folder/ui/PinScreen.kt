package com.mauricio.libertyx.folder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.vault.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class PinModo { Criar, Desbloquear, Trocar }

/** Garante a pasta segura aberta antes de [depois]: cria ou pede o PIN quando preciso. */
fun comCofre(vm: MainVm, depois: (MainVm) -> Unit) {
    when {
        !Vault.isSetUp -> vm.push(Screen.Pin(PinModo.Criar, depois))
        !Vault.unlocked.value -> vm.push(Screen.Pin(PinModo.Desbloquear, depois))
        else -> depois(vm)
    }
}

fun abrirPastaSegura(vm: MainVm) = comCofre(vm) { it.push(Screen.Folder(Loc.Vault())) }

@Composable
fun PinScreen(vm: MainVm, screen: Screen.Pin) {
    val scope = rememberCoroutineScope()
    var pin by remember { mutableStateOf("") }
    // Etapas: Criar = novo → confirmar; Trocar = atual → novo → confirmar.
    var etapa by remember { mutableStateOf(if (screen.modo == PinModo.Trocar) "atual" else if (screen.modo == PinModo.Criar) "novo" else "entrar") }
    var atual by remember { mutableStateOf("") }
    var primeiro by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var espera by remember { mutableLongStateOf(Vault.esperaRestanteMs()) }
    var apagar by remember { mutableStateOf(false) }

    LaunchedEffect(espera > 0) {
        while (espera > 0) { delay(500); espera = Vault.esperaRestanteMs() }
    }

    fun concluir() { vm.pop(); screen.depois(vm) }

    fun confirmar() {
        if (pin.length < 4) { erro = "O PIN precisa ter pelo menos 4 dígitos"; return }
        erro = null
        when (etapa) {
            "atual" -> { atual = pin; pin = ""; etapa = "novo" }
            "novo" -> { primeiro = pin; pin = ""; etapa = "confirmar" }
            "confirmar" -> {
                if (pin != primeiro) { erro = "Os PINs não conferem. Digite de novo."; pin = ""; etapa = "novo"; return }
                ocupado = true
                scope.launch {
                    val ok = withContext(Dispatchers.Default) {
                        if (screen.modo == PinModo.Criar) { Vault.setup(pin); true } else Vault.changePin(atual, pin)
                    }
                    ocupado = false
                    if (ok) concluir() else { erro = "PIN atual incorreto"; pin = ""; etapa = "atual" }
                }
            }
            "entrar" -> {
                ocupado = true
                scope.launch {
                    val r = withContext(Dispatchers.Default) { Vault.unlock(pin) }
                    ocupado = false; pin = ""
                    when (r) {
                        Vault.Resultado.Ok -> concluir()
                        Vault.Resultado.Errado -> { erro = "PIN incorreto"; espera = Vault.esperaRestanteMs() }
                        is Vault.Resultado.Espere -> espera = r.ms
                    }
                }
            }
        }
    }

    val titulo = when (etapa) {
        "atual" -> "Digite o PIN atual"
        "novo" -> if (screen.modo == PinModo.Criar) "Crie um PIN para a pasta segura" else "Digite o novo PIN"
        "confirmar" -> "Confirme o PIN"
        else -> "Pasta segura"
    }

    // Teclas numéricas do controle remoto ou de um teclado também digitam.
    val focoInicial = remember { androidx.compose.ui.focus.FocusRequester() }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
            .onPreviewKeyEvent { ev ->
                if (ev.type != KeyEventType.KeyDown || ocupado || espera > 0) return@onPreviewKeyEvent false
                val c = ev.utf16CodePoint.toChar()
                when {
                    c in '0'..'9' -> { if (pin.length < 12) { pin += c; erro = null }; true }
                    ev.key == Key.Backspace || ev.key == Key.Delete -> { pin = pin.dropLast(1); true }
                    ev.key == Key.Enter || ev.key == Key.NumPadEnter -> { confirmar(); true }
                    else -> false
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") }
        }
        val cabecalho: @Composable () -> Unit = {
            Icon(Icons.Filled.Lock, null, Modifier.size(52.dp), tint = MaterialTheme.colorScheme.primary)
            Text(titulo, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp))
            val sub = when {
                espera > 0 -> "Muitas tentativas. Tente de novo em ${(espera + 999) / 1000} s"
                erro != null -> erro!!
                screen.modo == PinModo.Criar && etapa == "novo" -> "De 4 a 12 dígitos. Os arquivos são cifrados com AES-256; sem o PIN não há como recuperá-los."
                else -> ""
            }
            Text(sub, color = if (erro != null || espera > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 32.dp, vertical = 10.dp).height(44.dp))
            Row(Modifier.height(24.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (ocupado) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                else repeat(maxOf(pin.length, 4)) { i ->
                    Box(Modifier.size(14.dp).clip(CircleShape).background(
                        if (i < pin.length) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest))
                }
            }
        }
        val teclado: @Composable () -> Unit = {
            val bloqueado = ocupado || espera > 0
            val teclas = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "OK")
            teclas.chunked(3).forEach { linha ->
                Row(Modifier.fillMaxWidth().padding(horizontal = if (Device.isTv) 60.dp else 40.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                    linha.forEach { t ->
                        Box(
                            Modifier.size(if (Device.isTv) 64.dp else 84.dp).then(if (t == "5") Modifier.focusRequester(focoInicial) else Modifier).tvFocus(42.dp, 1.08f).clip(CircleShape).clickable(enabled = !bloqueado) {
                                when (t) {
                                    "⌫" -> pin = pin.dropLast(1)
                                    "OK" -> confirmar()
                                    else -> if (pin.length < 12) { pin += t; erro = null }
                                }
                            },
                            contentAlignment = Alignment.Center,
                        ) {
                            when (t) {
                                "⌫" -> Icon(Icons.AutoMirrored.Filled.Backspace, "Apagar")
                                "OK" -> Text("OK", fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                                    color = if (pin.length >= 4) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                                else -> Text(t, fontSize = 30.sp)
                            }
                        }
                    }
                }
            }
            if (screen.modo == PinModo.Desbloquear) TextButton(onClick = { apagar = true }, Modifier.tvFocus(24.dp)) { Text("Esqueci o PIN") }
            else Spacer(Modifier.height(48.dp))
        }
        // Na TV a tela é baixa demais para empilhar tudo: título à esquerda, teclado à direita.
        if (Device.isTv) Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { cabecalho() }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) { teclado() }
        } else {
            Spacer(Modifier.weight(0.6f))
            cabecalho()
            Spacer(Modifier.weight(1f))
            teclado()
            Spacer(Modifier.height(12.dp))
        }
    }

    if (Device.isTv) LaunchedEffect(Unit) { runCatching { focoInicial.requestFocus() } }

    if (apagar) ConfirmDialog(
        "Apagar a pasta segura?",
        "Sem o PIN não existe forma de decifrar os arquivos. A única saída é apagar a pasta segura com tudo o que está nela.",
        "Apagar tudo", { apagar = false },
    ) { apagar = false; Vault.erase(); vm.pop() }
}
