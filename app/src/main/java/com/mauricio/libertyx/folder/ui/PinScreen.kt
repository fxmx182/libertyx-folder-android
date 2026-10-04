package com.mauricio.libertyx.folder.ui

import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.mauricio.libertyx.folder.Textos
import com.mauricio.libertyx.folder.R
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
                erro = Textos.get(if (ehPin) R.string.pin_incorreto else R.string.senha_incorreta) +
                    (r.restantes?.let { Textos.plural(R.plurals.pin_restantes, it, it) } ?: "")
                espera = Vault.esperaRestanteMs(); aoErrar(); tentativa++
            }
            is Vault.Resultado.Espere -> { espera = r.ms; tentativa++ }
            Vault.Resultado.Apagada -> {
                Toast.makeText(ctx, ctx.getString(R.string.pin_esgotadas), Toast.LENGTH_LONG).show()
                vm.esquecerCofre(); vm.home()
            }
        }
    }

    fun confirmar() {
        val v = valor
        if (v.length < minimo) { erro = Textos.plural(if (ehPin) R.plurals.pin_minimo_digitos else R.plurals.pin_minimo_caracteres, minimo, minimo); return }
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
                if (v != primeiro) { erro = Textos.get(R.string.pin_nao_conferem); valor = ""; etapa = "novo"; return }
                ocupado = true
                scope.launch {
                    val r = withContext(Dispatchers.Default) {
                        if (screen.modo == PinModo.Criar) { Vault.setup(v, tipoNovo); Vault.Resultado.Ok }
                        else Vault.changePin(atual, v, tipoNovo)
                    }
                    ocupado = false; valor = ""
                    if (r == Vault.Resultado.Ok && screen.modo == PinModo.Trocar) {
                        Toast.makeText(ctx, ctx.getString(if (tipoNovo == Vault.Tipo.Pin) R.string.pin_alterado else R.string.senha_alterada), Toast.LENGTH_SHORT).show()
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
        "atual" -> stringResource(if (ehPin) R.string.pin_atual else R.string.senha_atual)
        "novo" -> when {
            screen.modo == PinModo.Criar -> stringResource(if (ehPin) R.string.pin_criar else R.string.senha_criar)
            else -> stringResource(if (ehPin) R.string.pin_novo else R.string.senha_nova)
        }
        "confirmar" -> stringResource(if (ehPin) R.string.pin_confirme else R.string.senha_confirme)
        else -> stringResource(if (screen.modo == PinModo.Confirmar) R.string.pin_confirme_continuar else R.string.cofre_nome)
    }

    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { teclado?.hide(); vm.pop() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar))
            }
        }
        Column(
            Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 28.dp, vertical = if (Device.isTv) 0.dp else 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // O cadeado num anel de ouro: é a única tela do app com uma marca
            // dessas, e é a que precisa dizer sozinha onde a pessoa está.
            Box(
                Modifier.size(84.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Lx.ouro.copy(alpha = 0.18f), Color.Transparent)))
                    .border(1.dp, Lx.ouro.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Lock, null, Modifier.size(34.dp), tint = Lx.ouro) }
            Text(titulo, fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                letterSpacing = (-0.4).sp)
            val ajuda = when {
                espera > 0 -> null
                screen.modo == PinModo.Criar && etapa == "novo" ->
                    stringResource(if (ehPin) R.string.pin_ajuda_criar_pin else R.string.pin_ajuda_criar_senha)
                screen.modo == PinModo.Confirmar -> stringResource(if (ehPin) R.string.pin_ajuda_confirmar_pin else R.string.pin_ajuda_confirmar_senha)
                else -> null
            }
            if (ajuda != null) Text(ajuda, color = Lx.apagado, textAlign = TextAlign.Center, fontSize = 14.sp)

            OutlinedTextField(
                value = valor,
                onValueChange = { novo ->
                    val limpo = if (ehPin) novo.filter(Char::isDigit).take(16) else novo.take(64)
                    valor = limpo; erro = null
                },
                label = { Text(stringResource(if (ehPin) R.string.pin_rotulo else R.string.senha_rotulo)) },
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
                        Icon(if (ver) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, stringResource(if (ver) R.string.ocultar else R.string.mostrar))
                    }
                },
                isError = erro != null,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(Lx.raioPequeno),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Lx.ouro, cursorColor = Lx.ouro,
                    unfocusedBorderColor = Lx.vidroBorda, focusedLabelColor = Lx.ouro,
                ),
                modifier = Modifier.fillMaxWidth().focusRequester(foco),
            )
            val aviso = when {
                espera > 0 -> stringResource(R.string.pin_espera, ((espera + 999) / 1000).toInt())
                else -> erro
            }
            if (aviso != null) Text(aviso, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, fontSize = 14.sp)

            if (ocupado) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            else Button(
                onClick = { confirmar() },
                enabled = valor.length >= minimo && espera <= 0,
                modifier = Modifier.fillMaxWidth().tvFocus(24.dp),
            ) {
                Text(stringResource(when (etapa) { "entrar" -> if (screen.modo == PinModo.Confirmar) R.string.confirmar else R.string.desbloquear; "confirmar" -> R.string.salvar; else -> R.string.continuar }))
            }

            // Tipo: só na hora de escolher um novo.
            if (etapa == "novo") TextButton(
                onClick = { tipoNovo = if (tipoNovo == Vault.Tipo.Pin) Vault.Tipo.Senha else Vault.Tipo.Pin; valor = ""; erro = null },
                modifier = Modifier.tvFocus(24.dp),
            ) { Text(stringResource(if (tipoNovo == Vault.Tipo.Pin) R.string.pin_usar_senha else R.string.pin_usar_pin)) }

            if (screen.modo == PinModo.Desbloquear) TextButton(onClick = { apagar = true }, Modifier.tvFocus(24.dp)) {
                Text(stringResource(if (ehPin) R.string.pin_esqueci else R.string.senha_esqueci))
            }
        }
    }

    if (apagar) ConfirmDialog(
        stringResource(R.string.pin_apagar_titulo),
        stringResource(if (ehPin) R.string.pin_apagar_texto_pin else R.string.pin_apagar_texto_senha),
        stringResource(R.string.apagar_tudo), { apagar = false },
    ) { apagar = false; Vault.erase(); teclado?.hide(); vm.esquecerCofre() }
}
