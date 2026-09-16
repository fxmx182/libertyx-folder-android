package com.mauricio.libertyx.folder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.focusRequester
import com.mauricio.libertyx.folder.ui.Device
import com.mauricio.libertyx.folder.ui.tvFocus
import androidx.core.content.ContextCompat
import com.mauricio.libertyx.folder.ui.ArquivosTheme
import com.mauricio.libertyx.folder.ui.HomeScreen
import com.mauricio.libertyx.folder.ui.ListingScreen
import com.mauricio.libertyx.folder.ui.MainVm
import com.mauricio.libertyx.folder.ui.NetworkScreen
import com.mauricio.libertyx.folder.ui.OpPanel
import com.mauricio.libertyx.folder.ui.Screen

class MainActivity : ComponentActivity() {
    private val vm: MainVm by viewModels()
    private var temAcesso by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        temAcesso = acesso()
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        }
        setContent {
            ArquivosTheme {
                // Surface, e não Box com fundo: é ela que define a cor do texto.
                // Sem isso, texto solto sai preto — invisível no tema escuro e na TV.
                androidx.compose.material3.Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { Box(
                    // Margem de segurança da TV: há aparelho que corta ~5% das bordas.
                    Modifier.fillMaxSize().then(if (Device.isTv) Modifier.padding(horizontal = 48.dp, vertical = 24.dp) else Modifier)
                ) {
                    if (!temAcesso) PedirAcesso() else {
                        BackHandler(vm.stack.size > 1) { vm.pop() }
                        // Com "mover/copiar" pendente, voltar no início cancela em vez de fechar o app.
                        BackHandler(vm.stack.size == 1 && vm.clip != null) { vm.clip = null }
                        AnimatedContent(
                            vm.top, label = "tela",
                            transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(90)) },
                        ) { s ->
                            when (s) {
                                is Screen.Home -> {
                                    Box(Modifier.fillMaxSize()) {
                                        HomeScreen(vm, s)
                                        vm.clip?.let { c ->
                                            Box(Modifier.align(Alignment.BottomCenter)) {
                                                com.mauricio.libertyx.folder.ui.PasteBar(c, false, { vm.clip = null }, null)
                                            }
                                        }
                                    }
                                }
                                is Screen.Network -> NetworkScreen(vm, s)
                                is Screen.Settings -> com.mauricio.libertyx.folder.ui.SettingsScreen(vm, s)
                                is Screen.Pin -> com.mauricio.libertyx.folder.ui.PinScreen(vm, s)
                                else -> ListingScreen(vm, s)
                            }
                        }
                        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 64.dp)) { OpPanel() }
                        // Faixa sob a barra de status: a lista rolando não passa por baixo dos ícones.
                        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()
                            .windowInsetsTopHeight(androidx.compose.foundation.layout.WindowInsets.statusBars)
                            .background(MaterialTheme.colorScheme.background))
                    }
                } }
            }
        }
    }

    private var saiuEm = 0L

    override fun onStop() {
        super.onStop()
        saiuEm = System.currentTimeMillis()
        // "Ao sair do app": bloqueia já (exceto no meio de uma cópia para a pasta).
        if (!isChangingConfigurations && com.mauricio.libertyx.folder.vault.Vault.autoBloqueio == 0 &&
            !com.mauricio.libertyx.folder.ops.Ops.ocupado) {
            com.mauricio.libertyx.folder.vault.Vault.lock()
            vm.esquecerCofre()
        }
    }

    override fun onStart() {
        super.onStart()
        // Um minuto fora do app bloqueia a pasta segura. Não na hora: abrir uma
        // foto dela em outro app também tira este da frente.
        if (saiuEm > 0 && System.currentTimeMillis() - saiuEm > com.mauricio.libertyx.folder.vault.Vault.autoBloqueio * 1000L && !com.mauricio.libertyx.folder.ops.Ops.ocupado) {
            com.mauricio.libertyx.folder.vault.Vault.lock()
            vm.esquecerCofre()
        }
    }

    override fun onResume() {
        super.onResume()
        temAcesso = acesso()
    }

    private fun acesso(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) Environment.isExternalStorageManager()
        else ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED

    private fun pedir() {
        if (Build.VERSION.SDK_INT >= 30) {
            val telas = listOf(
                Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName")),
                Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
            )
            for (t in telas) {
                if (runCatching { startActivity(t) }.isSuccess) return
            }
            // Há Fire TV sem essa tela nas configurações: sobra o acesso às mídias,
            // e o aviso de como liberar o resto.
            val midias = if (Build.VERSION.SDK_INT >= 33) arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO,
            ) else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            requestPermissions(midias, 1)
            semTelaDeAcesso = true
        } else {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
        }
    }

    private var semTelaDeAcesso by mutableStateOf(false)

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        temAcesso = acesso()
    }

    @androidx.compose.runtime.Composable
    private fun PedirAcesso() {
        Column(
            Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        ) {
            Icon(Icons.Filled.FolderOpen, null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Permitir acesso aos arquivos", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 20.dp), textAlign = TextAlign.Center)
            Text(
                "Para mostrar, copiar e organizar os arquivos do aparelho, o LibertyX Folder precisa do acesso a todos os arquivos.",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp),
            )
            if (semTelaDeAcesso) Text(
                "Este aparelho não tem a tela de acesso a todos os arquivos. Pelo computador, libere com:\n" +
                    "adb shell appops set $packageName MANAGE_EXTERNAL_STORAGE allow",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp),
            )
            val foco = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
            Button(onClick = { pedir() }, Modifier.padding(top = 28.dp).widthIn(max = 480.dp).fillMaxWidth().focusRequester(foco).tvFocus(24.dp)) { Text("Permitir") }
            TextButton(onClick = { vm.push(Screen.Network()); temAcesso = true }, Modifier.tvFocus(24.dp)) { Text("Só usar servidores de rede") }
            if (Device.isTv) androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
        }
    }
}
