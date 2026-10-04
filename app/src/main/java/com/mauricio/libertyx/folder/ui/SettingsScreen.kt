package com.mauricio.libertyx.folder.ui

import android.widget.Toast
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mauricio.libertyx.folder.R
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
import com.mauricio.libertyx.folder.BuildConfig
import com.mauricio.libertyx.folder.pro.Pro
import com.mauricio.libertyx.folder.vault.Vault
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.WorkspacePremium

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainVm, screen: Screen.Settings) {
    val ctx = LocalContext.current
    // Relê as configurações sempre que algo muda na pasta segura.
    val versao by Vault.versao.collectAsState()
    val aberta by Vault.unlocked.collectAsState()
    var dialogo by remember { mutableStateOf<String?>(null) }
    val existe = remember(versao) { Vault.isSetUp }
    val ehPin = remember(versao) { Vault.tipo == Vault.Tipo.Pin }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = { IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar)) } },
                title = { Text(stringResource(R.string.configuracoes), fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp) },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 48.dp)) {
            if (BuildConfig.PRO) {
                item { SectionTitle(stringResource(R.string.pro_nome)) }
                item {
                    val pro by Pro.estado.collectAsState()
                    Card(Modifier.padding(horizontal = 16.dp)) {
                        Opcao(
                            Icons.Filled.WorkspacePremium, stringResource(R.string.pro_nome),
                            when (val e = pro) {
                                Pro.Estado.Comprado -> stringResource(R.string.cfg_pro_ativo)
                                is Pro.Estado.Teste -> pluralStringResource(R.plurals.cfg_pro_teste, e.diasRestantes, e.diasRestantes)
                                Pro.Estado.Expirado -> stringResource(R.string.cfg_pro_expirado)
                            },
                        ) { vm.push(Screen.Pro()) }
                    }
                }
            }
            if (BuildConfig.GUIA) {
                item { SectionTitle(stringResource(R.string.sec_ajuda)) }
                item {
                    Card(Modifier.padding(horizontal = 16.dp)) {
                        Opcao(Icons.Filled.School, stringResource(R.string.cfg_guia), stringResource(R.string.cfg_guia_sub)) {
                            vm.push(Screen.Guia())
                        }
                    }
                }
            }
            // Na TV não existe pasta segura. Quem criou uma numa versão de
            // antes ainda acha aqui como tirar os arquivos e apagá-la — o
            // recurso sumir não pode levar os arquivos de alguém junto.
            if (!BuildConfig.PASTA_SEGURA) {
                if (existe) {
                    item { SectionTitle(stringResource(R.string.sec_cofre_antigo)) }
                    item {
                        Card(Modifier.padding(horizontal = 16.dp)) {
                            Opcao(Icons.Filled.FolderOpen, stringResource(R.string.cfg_antigo_abrir),
                                stringResource(R.string.cfg_antigo_abrir_sub)) { abrirPastaSegura(vm) }
                            Opcao(Icons.Filled.DeleteForever, stringResource(R.string.cfg_excluir_cofre),
                                stringResource(if (ehPin) R.string.cfg_antigo_excluir_sub_pin else R.string.cfg_antigo_excluir_sub_senha),
                                perigo = true) { dialogo = "excluir" }
                        }
                    }
                }
                item {
                    Text(
                        stringResource(R.string.versao, BuildConfig.VERSION_NAME),
                        Modifier.padding(start = 26.dp, top = 28.dp), fontSize = 12.sp, color = Lx.tenue,
                    )
                }
                return@LazyColumn
            }
            item { SectionTitle(stringResource(R.string.cofre_nome)) }
            item {
                Card(Modifier.padding(horizontal = 16.dp)) {
                    if (!existe) {
                        Opcao(Icons.Filled.Lock, stringResource(R.string.cfg_criar), stringResource(R.string.cfg_criar_sub)) {
                            abrirPastaSegura(vm)
                        }
                        return@Card
                    }
                    Opcao(Icons.Filled.FolderOpen, stringResource(R.string.cfg_abrir), stringResource(if (aberta) R.string.cfg_desbloqueada else R.string.cofre_bloqueada_curto)) { abrirPastaSegura(vm) }
                    if (aberta) Opcao(Icons.Filled.Lock, stringResource(R.string.cfg_bloquear_agora), stringResource(if (ehPin) R.string.cfg_bloquear_sub_pin else R.string.cfg_bloquear_sub_senha)) {
                        Vault.lock(); vm.esquecerCofre()
                        Toast.makeText(ctx, ctx.getString(R.string.cfg_bloqueada_toast), Toast.LENGTH_SHORT).show()
                    }
                    Opcao(Icons.Filled.Key, stringResource(if (ehPin) R.string.cfg_alterar_pin else R.string.cfg_alterar_senha), stringResource(R.string.cfg_alterar_sub)) {
                        vm.push(Screen.Pin(PinModo.Trocar) {})
                    }
                    Opcao(
                        if (Vault.visivel) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        stringResource(R.string.cfg_mostrar),
                        stringResource(if (Vault.visivel) R.string.cfg_mostrar_sim else R.string.cfg_mostrar_nao),
                        switch = Vault.visivel,
                    ) {
                        // Mudar a visibilidade exige o PIN: senão qualquer um revelaria a pasta.
                        val novo = !Vault.visivel
                        comCofre(vm) { Vault.visivel = novo }
                    }
                    Opcao(Icons.Filled.LockClock, stringResource(R.string.cfg_bloqueio_auto), textoBloqueio(Vault.autoBloqueio)) { dialogo = "bloqueio" }
                }
            }

            item { SectionTitle(stringResource(R.string.sec_perda)) }
            item {
                Card(Modifier.padding(horizontal = 16.dp)) {
                    Opcao(
                        Icons.Filled.PhonelinkErase, stringResource(R.string.cfg_apagar10, Vault.LIMITE_APAGAR),
                        stringResource(if (ehPin) R.string.cfg_apagar10_sub_pin else R.string.cfg_apagar10_sub_senha, Vault.LIMITE_APAGAR),
                        switch = existe && Vault.apagarApos10, enabled = existe,
                    ) {
                        val novo = !Vault.apagarApos10
                        comCofre(vm) { Vault.apagarApos10 = novo }
                    }
                    Opcao(
                        Icons.Filled.DeleteForever, stringResource(R.string.cfg_excluir_cofre),
                        stringResource(if (ehPin) R.string.cfg_excluir_sub_pin else R.string.cfg_excluir_sub_senha),
                        perigo = true, enabled = existe,
                    ) { dialogo = "excluir" }
                }
            }
        }
    }

    when (dialogo) {
        "bloqueio" -> AlertDialog(
            onDismissRequest = { dialogo = null },
            title = { Text(stringResource(R.string.cfg_bloqueio_auto)) },
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
            confirmButton = { TextButton(modifier = Modifier.tvFocus(24.dp), onClick = { dialogo = null }) { Text(stringResource(R.string.fechar)) } },
        )
        "excluir" -> ConfirmDialog(
            stringResource(R.string.cfg_excluir_titulo),
            stringResource(if (ehPin) R.string.cfg_excluir_texto_pin else R.string.cfg_excluir_texto_senha),
            stringResource(R.string.excluir), { dialogo = null },
        ) {
            dialogo = null
            vm.push(Screen.Pin(PinModo.Confirmar) { v ->
                Vault.erase(); v.esquecerCofre()
                Toast.makeText(ctx, ctx.getString(R.string.cfg_excluida_toast), Toast.LENGTH_LONG).show()
            })
        }
    }
}

@Composable
private fun textoBloqueio(seg: Int): String =
    if (seg == 0) stringResource(R.string.bloq_ao_sair)
    else pluralStringResource(R.plurals.bloq_minutos, seg / 60, seg / 60)

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
