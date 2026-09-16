package com.mauricio.libertyx.folder.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.vault.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Criar, abrir, trocar, ou reconfirmar antes de uma ação destrutiva (excluir a pasta). */
enum class PinModo { Criar, Desbloquear, Trocar, Confirmar }

/** Garante a pasta segura aberta antes de [depois]: cria ou pede o PIN quando preciso. */
fun comCofre(vm: MainVm, depois: (MainVm) -> Unit) {
    when {
        !Vault.isSetUp -> vm.push(Screen.Pin(PinModo.Criar, depois))
        !Vault.unlocked.value -> vm.push(Screen.Pin(PinModo.Desbloquear, depois))
        else -> depois(vm)
    }
}

fun abrirPastaSegura(vm: MainVm) = comCofre(vm) { it.push(Screen.Folder(Loc.Vault())) }

/**
 * PIN digitado no teclado do próprio sistema, e não num teclado desenhado na
 * tela: é o que o usuário espera, funciona com teclado físico e com o teclado
 * da TV, e permite escolher senha com letras.
 */
@Composable
fun PinScreen(vm: MainVm, screen: Screen.Pin) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val teclado = LocalSoftwareKeyboardController.current

    // Etapas: Criar = novo → confirmar; Trocar = atual → novo → confirmar; Desbloquear/Confirmar = entrar.
    var etapa by remember {
        mutableStateOf(when (screen.modo) { PinModo.Trocar -> "atual"; PinModo.Criar -> "novo"; else -> "entrar" })
    }
    var valor by remember { mutableStateOf("") }
    var atual by remember { mutableStateOf("") }
    var primeiro by remember { mutableStateOf("") }
    var erro by remember { mutableStateOf<String?>(null) }
    var ocupado by remember { mutableStateOf(false) }
    var ver by remember { mutableStateOf(false) }
    var espera by remember { mutableLongStateOf(Vault.esperaRestanteMs()) }
    var apagar by remember { mutableStateOf(false) }
    // Ao criar ou trocar, escolhe-se o tipo; ao entrar, vale o que foi gravado.
    var tipoNovo by remember { mutableStateOf(if (Vault.isSetUp) Vault.tipo else Vault.Tipo.Pin) }
    val tipoCampo = if (etapa == "novo" || etapa == "confirmar") tipoNovo else Vault.tipo
    val ehPin = tipoCampo == Vault.Tipo.Pin
    val minimo = if (ehPin) 4 else 6
    val foco = remember { FocusRequester() }

    LaunchedEffect(espera > 0) { while (espera > 0) { delay(500); espera = Vault.esperaRestanteMs() } }
    // Campo focado e teclado aberto ao entrar, a cada etapa, depois de cada erro e quando a espera acaba.
    var tentativa by remember { mutableStateOf(0) }
    LaunchedEffect(etapa, tentativa, espera > 0) {
        if (espera <= 0) runCatching { delay(50); foco.requestFocus(); teclado?.show() }
    }

    fun concluir() { teclado?.hide(); vm.pop(); screen.depois(vm) }

    fun tratar(r: Vault.Resultado, aoErrar: () -> Unit) {
        when (r) {
            Vault.Resultado.Ok -> concluir()
            is Vault.Resultado.Errado -> {
                erro = (if (ehPin) "PIN incorreto" else "Senha incorreta") +
                    (r.restantes?.let { " · a pasta será apagada em $it tentativa${if (it == 1) "" else "s"}" } ?: "")
                espera = Vault.esperaRestanteMs(); aoErrar(); tentativa++
            }
            is Vault.Resultado.Espere -> { espera = r.ms; tentativa++ }
            Vault.Resultado.Apagada -> {
                Toast.makeText(ctx, "Tentativas esgotadas: a pasta segura foi apagada", Toast.LENGTH_LONG).show()
                vm.esquecerCofre(); vm.home()
            }
        }
    }

    fun confirmar() {
        val v = valor
        if (v.length < minimo) { erro = "Use pelo menos $minimo ${if (ehPin) "dígitos" else "caracteres"}"; return }
        erro = null
        when (etapa) {
            "atual" -> {
                ocupado = true
                scope.launch {
                    val r = withContext(Dispatchers.Default) { Vault.confirmar(v) }
                    ocupado = false; valor = ""
                    if (r == Vault.Resultado.Ok) { atual = v; etapa = "novo" } else tratar(r) {}
                }
            }
            "novo" -> { primeiro = v; valor = ""; etapa = "confirmar" }
            "confirmar" -> {
                if (v != primeiro) { erro = "Não conferem. Digite de novo."; valor = ""; etapa = "novo"; return }
                ocupado = true
                scope.launch {
                    val r = withContext(Dispatchers.Default) {
                        if (screen.modo == PinModo.Criar) { Vault.setup(v, tipoNovo); Vault.Resultado.Ok }
                        else Vault.changePin(atual, v, tipoNovo)
                    }
                    ocupado = false; valor = ""
                    if (r == Vault.Resultado.Ok && screen.modo == PinModo.Trocar) {
                        Toast.makeText(ctx, if (tipoNovo == Vault.Tipo.Pin) "PIN alterado" else "Senha alterada", Toast.LENGTH_SHORT).show()
                    }
                    tratar(r) { etapa = "atual" }
                }
            }
            "entrar" -> {
                ocupado = true
                scope.launch {
                    val r = withContext(Dispatchers.Default) {
                        if (screen.modo == PinModo.Confirmar) Vault.confirmar(v) else Vault.unlock(v)
                    }
                    ocupado = false; valor = ""
                    tratar(r) {}
                }
            }
        }
    }

    val titulo = when (etapa) {
        "atual" -> if (ehPin) "Digite o PIN atual" else "Digite a senha atual"
        "novo" -> when {
            screen.modo == PinModo.Criar -> if (ehPin) "Crie o PIN da pasta segura" else "Crie a senha da pasta segura"
            else -> if (ehPin) "Digite o novo PIN" else "Digite a nova senha"
        }
        "confirmar" -> if (ehPin) "Confirme o PIN" else "Confirme a senha"
        else -> if (screen.modo == PinModo.Confirmar) "Confirme para continuar" else "Pasta segura"
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { teclado?.hide(); vm.pop() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar")
            }
        }
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 28.dp, vertical = if (Device.isTv) 0.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Filled.Lock, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Text(titulo, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            val ajuda = when {
                espera > 0 -> null
                screen.modo == PinModo.Criar && etapa == "novo" ->
                    "Os arquivos são cifrados com AES-256. Sem ${if (ehPin) "o PIN" else "a senha"} não há como recuperá-los."
                screen.modo == PinModo.Confirmar -> "Por segurança, digite ${if (ehPin) "o PIN" else "a senha"} de novo."
                else -> null
            }
            if (ajuda != null) Text(ajuda, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, fontSize = 14.sp)

            OutlinedTextField(
                value = valor,
                onValueChange = { novo ->
                    val limpo = if (ehPin) novo.filter(Char::isDigit).take(16) else novo.take(64)
                    valor = limpo; erro = null
                },
                label = { Text(if (ehPin) "PIN" else "Senha") },
                singleLine = true,
                enabled = espera <= 0,
                readOnly = ocupado,
                visualTransformation = if (ver) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (ehPin) KeyboardType.NumberPassword else KeyboardType.Password,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { confirmar() }),
                trailingIcon = {
                    IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { ver = !ver }) {
                        Icon(if (ver) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (ver) "Ocultar" else "Mostrar")
                    }
                },
                isError = erro != null,
                modifier = Modifier.fillMaxWidth().focusRequester(foco),
            )
            val aviso = when {
                espera > 0 -> "Muitas tentativas. Tente de novo em ${(espera + 999) / 1000} s"
                else -> erro
            }
            if (aviso != null) Text(aviso, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 14.sp)

            if (ocupado) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            else Button(
                onClick = { confirmar() },
                enabled = valor.length >= minimo && espera <= 0,
                modifier = Modifier.fillMaxWidth().tvFocus(24.dp),
            ) {
                Text(when (etapa) { "entrar" -> if (screen.modo == PinModo.Confirmar) "Confirmar" else "Desbloquear"; "confirmar" -> "Salvar"; else -> "Continuar" })
            }

            // Tipo: só na hora de escolher um novo.
            if (etapa == "novo") TextButton(
                onClick = { tipoNovo = if (tipoNovo == Vault.Tipo.Pin) Vault.Tipo.Senha else Vault.Tipo.Pin; valor = ""; erro = null },
                modifier = Modifier.tvFocus(24.dp),
            ) { Text(if (tipoNovo == Vault.Tipo.Pin) "Usar senha com letras" else "Usar PIN numérico") }

            if (screen.modo == PinModo.Desbloquear) TextButton(onClick = { apagar = true }, Modifier.tvFocus(24.dp)) {
                Text("Esqueci ${if (ehPin) "o PIN" else "a senha"}")
            }
        }
    }

    if (apagar) ConfirmDialog(
        "Apagar a pasta segura?",
        "Sem ${if (ehPin) "o PIN" else "a senha"} não existe forma de decifrar os arquivos. A única saída é apagar a pasta segura com tudo o que está nela.",
        "Apagar tudo", { apagar = false },
    ) { apagar = false; Vault.erase(); teclado?.hide(); vm.esquecerCofre() }
}
