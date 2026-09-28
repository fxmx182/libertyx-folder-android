package com.mauricio.libertyx.folder.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.PhonelinkErase
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.libertyx.folder.vault.Vault

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainVm, screen: Screen.Settings) {
    val ctx = LocalContext.current
    // Relê as configurações sempre que algo muda na pasta segura.
    val versao by Vault.versao.collectAsState()
    val aberta by Vault.unlocked.collectAsState()
    var dialogo by remember { mutableStateOf<String?>(null) }
    val existe = remember(versao) { Vault.isSetUp }
    val nome = remember(versao) { if (Vault.tipo == Vault.Tipo.Pin) "PIN" else "senha" }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = { IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                title = { Text("Configurações", fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp) },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 48.dp)) {
            item { SectionTitle("Pasta segura") }
            item {
                Card(Modifier.padding(horizontal = 16.dp)) {
                    if (!existe) {
                        Opcao(Icons.Filled.Lock, "Criar pasta segura", "Guarde arquivos cifrados, protegidos por PIN ou senha") {
                            abrirPastaSegura(vm)
                        }
                        return@Card
                    }
                    Opcao(Icons.Filled.FolderOpen, "Abrir pasta segura", if (aberta) "Desbloqueada" else "Bloqueada") { abrirPastaSegura(vm) }
                    if (aberta) Opcao(Icons.Filled.Lock, "Bloquear agora", "Exige ${if (nome == "PIN") "o PIN" else "a senha"} para abrir de novo") {
                        Vault.lock(); vm.esquecerCofre()
                        Toast.makeText(ctx, "Pasta segura bloqueada", Toast.LENGTH_SHORT).show()
                    }
                    Opcao(Icons.Filled.Key, if (nome == "PIN") "Alterar PIN" else "Alterar senha", "Também dá para trocar entre PIN e senha com letras") {
                        vm.push(Screen.Pin(PinModo.Trocar) {})
                    }
                    Opcao(
                        if (Vault.visivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        "Mostrar pasta segura no início",
                        if (Vault.visivel) "Aparece na tela inicial" else "Oculta: acesse por aqui, em Configurações",
                        switch = Vault.visivel,
                    ) {
                        // Mudar a visibilidade exige o PIN: senão qualquer um revelaria a pasta.
                        val novo = !Vault.visivel
                        comCofre(vm) { Vault.visivel = novo }
                    }
                    Opcao(Icons.Filled.LockClock, "Bloqueio automático", textoBloqueio(Vault.autoBloqueio)) { dialogo = "bloqueio" }
                }
            }

            item { SectionTitle("Em caso de perda ou roubo") }
            item {
                Card(Modifier.padding(horizontal = 16.dp)) {
                    Opcao(
                        Icons.Filled.PhonelinkErase, "Apagar após ${Vault.LIMITE_APAGAR} tentativas erradas",
                        "Se alguém errar ${if (nome == "PIN") "o PIN" else "a senha"} ${Vault.LIMITE_APAGAR} vezes seguidas, tudo é apagado",
                        switch = existe && Vault.apagarApos10, enabled = existe,
                    ) {
                        val novo = !Vault.apagarApos10
                        comCofre(vm) { Vault.apagarApos10 = novo }
                    }
                    Opcao(
                        Icons.Filled.DeleteForever, "Excluir pasta segura",
                        "Apaga de vez todos os arquivos e ${if (nome == "PIN") "o PIN" else "a senha"}",
                        perigo = true, enabled = existe,
                    ) { dialogo = "excluir" }
                }
            }
        }
    }

    when (dialogo) {
        "bloqueio" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text("Bloqueio automático") },
            text = {
                Column(Modifier.setasTrocamDeCampo()) {
                    listOf(0, 60, 300, 1800).forEach { seg ->
                        Row(
                            Modifier.fillMaxWidth().tvFocus(12.dp).clickable { Vault.autoBloqueio = seg; dialogo = null },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(Vault.autoBloqueio == seg, null)
                            Text(textoBloqueio(seg), Modifier.padding(start = 8.dp, top = 12.dp, bottom = 12.dp))
                        }
                    }
                }
            },
            confirmButton = { TextButton(modifier = Modifier.tvFocus(24.dp), onClick = { dialogo = null }) { Text("Fechar") } },
        )
        "excluir" -> ConfirmDialog(
            "Excluir a pasta segura?",
            "Todos os arquivos dentro dela serão apagados para sempre, sem como recuperar. " +
                "Para continuar, confirme ${if (nome == "PIN") "o PIN" else "a senha"}.",
            "Excluir", { dialogo = null },
        ) {
            dialogo = null
            vm.push(Screen.Pin(PinModo.Confirmar) { v ->
                Vault.erase(); v.esquecerCofre()
                Toast.makeText(ctx, "Pasta segura excluída", Toast.LENGTH_LONG).show()
            })
        }
    }
}

private fun textoBloqueio(seg: Int) = when (seg) {
    0 -> "Ao sair do app"
    60 -> "1 minuto fora do app"
    300 -> "5 minutos fora do app"
    else -> "${seg / 60} minutos fora do app"
}

@Composable
private fun Opcao(
    icon: ImageVector, titulo: String, sub: String,
    switch: Boolean? = null, perigo: Boolean = false, enabled: Boolean = true, onClick: () -> Unit,
) {
    val cor = if (perigo) MaterialTheme.colorScheme.error else Lx.ouro
    Row(
        Modifier.fillMaxWidth().tvFocus(24.dp).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, if (enabled) cor else Lx.tenue)
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge,
                color = when { !enabled -> MaterialTheme.colorScheme.onSurfaceVariant; perigo -> MaterialTheme.colorScheme.error; else -> MaterialTheme.colorScheme.onSurface })
            Text(sub, fontSize = 12.sp, lineHeight = 16.sp, color = Lx.tenue)
        }
        if (switch != null) Switch(switch, null, enabled = enabled, modifier = Modifier.padding(start = 12.dp))
    }
}
