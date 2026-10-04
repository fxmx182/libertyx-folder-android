package com.mauricio.libertyx.folder

import android.Manifest
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.collectAsState
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
import com.mauricio.libertyx.folder.ui.brilhoDeFundo
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
        // O app é escuro sempre, então os ícones das barras do sistema são
        // claros sempre — sem isso eles seguem o tema do aparelho e somem.
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
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
                    // O brilho fica na raiz, atrás de tudo: as telas são
                    // transparentes e todas ganham a mesma profundidade.
                    // Margem de segurança da TV: há aparelho que corta ~5% das bordas.
                    Modifier.fillMaxSize().brilhoDeFundo()
                        .then(if (Device.isTv) Modifier.padding(horizontal = 48.dp, vertical = 24.dp) else Modifier)
                ) {
                    val proEstado by com.mauricio.libertyx.folder.pro.Pro.estado.collectAsState()
                    val redeLiberada = proEstado !is com.mauricio.libertyx.folder.pro.Pro.Estado.Expirado
                    // O guia vem antes do pedido de permissão, como no LibertyX
                    // Player: a última página dele avisa do pedido.
                    if (BuildConfig.GUIA && !com.mauricio.libertyx.folder.ui.Prefs.guiaVisto) {
                        com.mauricio.libertyx.folder.ui.GuiaScreen { com.mauricio.libertyx.folder.ui.Prefs.guiaVisto = true }
                    } else if (!temAcesso) PedirAcesso() else {
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
                                // Rede travada (teste encerrado, sem compra): a tela
                                // do Pro no lugar, venha a pessoa por onde vier.
                                is Screen.Network -> if (redeLiberada) NetworkScreen(vm, s) else com.mauricio.libertyx.folder.ui.ProScreen(vm)
                                is Screen.Folder -> if (s.loc is com.mauricio.libertyx.folder.fs.Loc.Smb && !redeLiberada)
                                    com.mauricio.libertyx.folder.ui.ProScreen(vm) else ListingScreen(vm, s)
                                is Screen.Pro -> com.mauricio.libertyx.folder.ui.ProScreen(vm)
                                is Screen.Guia -> com.mauricio.libertyx.folder.ui.GuiaScreen(primeiraVez = false) { vm.pop() }
                                is Screen.Settings -> com.mauricio.libertyx.folder.ui.SettingsScreen(vm, s)
                                is Screen.Pin -> com.mauricio.libertyx.folder.ui.PinScreen(vm, s)
                                else -> ListingScreen(vm, s)
                            }
                        }
                        Box(Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 64.dp)) { OpPanel() }
                        // Faixa sob a barra de status: a lista rolando não passa por baixo
                        // dos ícones. Em degradê, senão o corte reto apareceria sobre o brilho.
                        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth()
                            .windowInsetsTopHeight(androidx.compose.foundation.layout.WindowInsets.statusBars)
                            .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.75f),
                            ))))
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
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(R.drawable.ic_marca), null, Modifier.size(88.dp))
            Text(stringResource(R.string.perm_titulo), style = MaterialTheme.typography.headlineSmall,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                modifier = Modifier.padding(top = 20.dp), textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.perm_texto),
                textAlign = TextAlign.Center, color = com.mauricio.libertyx.folder.ui.Lx.apagado, modifier = Modifier.padding(top = 12.dp),
            )
            if (semTelaDeAcesso) Text(
                stringResource(R.string.perm_sem_tela) + "\n" +
                    "adb shell appops set $packageName MANAGE_EXTERNAL_STORAGE allow",
                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp),
            )
            val foco = androidx.compose.runtime.remember { androidx.compose.ui.focus.FocusRequester() }
            Button(onClick = { pedir() }, Modifier.padding(top = 28.dp).widthIn(max = 480.dp).fillMaxWidth().focusRequester(foco).tvFocus(24.dp)) { Text(stringResource(R.string.perm_permitir)) }
            TextButton(onClick = { vm.push(Screen.Network()); temAcesso = true }, Modifier.tvFocus(24.dp)) { Text(stringResource(R.string.perm_so_rede)) }
            if (Device.isTv) androidx.compose.runtime.LaunchedEffect(Unit) { runCatching { foco.requestFocus() } }
        }
    }
}
