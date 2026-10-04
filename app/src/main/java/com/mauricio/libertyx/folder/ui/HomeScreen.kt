package com.mauricio.libertyx.folder.ui

import android.os.Environment
import androidx.compose.ui.res.stringResource
import com.mauricio.libertyx.folder.Textos
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.mauricio.libertyx.folder.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.libertyx.folder.fs.FileEntry
import com.mauricio.libertyx.folder.media.Categories
import com.mauricio.libertyx.folder.media.Category
import com.mauricio.libertyx.folder.media.Storage
import com.mauricio.libertyx.folder.media.Volume
import com.mauricio.libertyx.folder.ops.Ops
import com.mauricio.libertyx.folder.smb.Descobertas
import com.mauricio.libertyx.folder.smb.SmbServerStore
import com.mauricio.libertyx.folder.fs.Loc
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun HomeScreen(vm: MainVm, screen: Screen.Home) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var recentes by remember { mutableStateOf<List<FileEntry>>(emptyList()) }
    var volumes by remember { mutableStateOf<List<Volume>>(emptyList()) }
    val versao by Ops.versao.collectAsState()
    val servidores by SmbServerStore.get(ctx).servers.collectAsState()
    val achados by Descobertas.achados.collectAsState()
    val buscando by Descobertas.buscando.collectAsState()
    var termo by remember { mutableStateOf("") }
    var servidorNovo by remember { mutableStateOf<ServerDraft?>(null) }
    var buscaTv by remember { mutableStateOf(false) }
    val versaoCofre by com.mauricio.libertyx.folder.vault.Vault.versao.collectAsState()
    val cofreAberto by com.mauricio.libertyx.folder.vault.Vault.unlocked.collectAsState()
    // Oculta: some do início enquanto bloqueada; aberta, continua à mão.
    val cofreVisivel = remember(versaoCofre, cofreAberto) { com.mauricio.libertyx.folder.vault.Vault.visivel || cofreAberto }
    // A rede é o Pro (na TV): terminado o teste, os computadores continuam
    // aparecendo — o que se trava é abrir, e quem tenta abrir vê o porquê.
    val proEstado by com.mauricio.libertyx.folder.pro.Pro.estado.collectAsState()
    fun rede(acao: () -> Unit) {
        if (proEstado !is com.mauricio.libertyx.folder.pro.Pro.Estado.Expirado) acao() else vm.push(Screen.Pro())
    }

    LaunchedEffect(versao) {
        withContext(Dispatchers.IO) {
            volumes = Storage.volumes(ctx)
            recentes = Categories.recent(ctx)
        }
    }
    // A busca na rede começa sozinha ao abrir o app.
    LaunchedEffect(Unit) { if (achados.isEmpty()) scope.launch { Descobertas.buscar(ctx.applicationContext) } }

    LazyColumn(Modifier.fillMaxSize(), state = screen.listState, contentPadding = PaddingValues(bottom = 110.dp)) {
        item {
            Row(
                Modifier.statusBarsPadding().padding(start = 20.dp, end = 14.dp).padding(top = 40.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A marca do app na própria tela, e não só no launcher: o mesmo
                // desenho do ícone, recortado sem a margem do adaptativo.
                Image(painterResource(R.drawable.ic_marca), null, Modifier.size(40.dp))
                Text(
                    buildAnnotatedString {
                        append("LibertyX")
                        withStyle(SpanStyle(color = Lx.ouro)) { append(" Folder") }
                    },
                    style = Lx.titulao, modifier = Modifier.padding(start = 12.dp).weight(1f),
                )
                // O vidro no próprio botão, e não numa caixa recortada em volta:
                // o recorte comia a borda dourada do foco, e na TV a engrenagem
                // ficava sem realce nenhum.
                IconButton(
                    modifier = Modifier.size(44.dp).tvFocus(22.dp).vidro(22.dp),
                    onClick = { vm.push(Screen.Settings()) },
                ) { Icon(Icons.Filled.Settings, stringResource(R.string.configuracoes), tint = Lx.apagado) }
            }
        }
        item {
            Box(Modifier.padding(horizontal = 16.dp).fillMaxWidth().vidro(28.dp)) {
                if (Device.isTv) Row(
                    Modifier.fillMaxWidth().tvFocus(28.dp).clickable { buscaTv = true }.padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Search, null, tint = Lx.apagado)
                    Text(stringResource(R.string.pesquisar_aparelho), Modifier.padding(start = 12.dp), color = Lx.apagado)
                } else TextField(
                    termo, { termo = it }, singleLine = true,
                    placeholder = { Text(stringResource(R.string.pesquisar_aparelho), color = Lx.tenue) },
                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = Lx.apagado) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (termo.isNotBlank()) vm.push(Screen.Search(termo.trim(), null)) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Lx.ouro,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (recentes.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.sec_recentes)) }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentes, key = { it.loc.toString() }) { e ->
                        Column(Modifier.width(104.dp).tvFocus(16.dp, 1.06f).clip(RoundedCornerShape(16.dp)).clickable { Open.open(ctx, e) }) {
                            Thumb(e, 104.dp, corner = 16.dp)
                            Text(e.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                color = Lx.apagado, modifier = Modifier.padding(top = 6.dp, start = 2.dp))
                        }
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.sec_categorias)) }
        item {
            Card(Modifier.padding(horizontal = 16.dp)) {
                val cats = listOf(
                    Triple(Category.Images, Icons.Filled.Image, Color(0xFFC08BFF)),
                    Triple(Category.Videos, Icons.Filled.VideoLibrary, Color(0xFFFF6E8A)),
                    Triple(Category.Audio, Icons.Filled.Audiotrack, Color(0xFF35D0BE)),
                    Triple(Category.Documents, Icons.Filled.Description, Color(0xFF5B9CFF)),
                    Triple(Category.Downloads, Icons.Filled.Download, Lx.ouro),
                    Triple(Category.Apks, Icons.Filled.Android, Color(0xFF8BD962)),
                    Triple(Category.Compressed, Icons.Filled.FolderZip, Color(0xFFD9A15C)),
                )
                cats.chunked(4).forEach { linha ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        linha.forEach { (c, icone, cor) ->
                            CategoryTile(stringResource(c.curto), icone, cor, Modifier.weight(1f)) {
                                if (c == Category.Downloads) {
                                    val d = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                    vm.push(Screen.Folder(Storage.locFor(ctx, d.path)))
                                } else vm.push(Screen.Cat(c))
                            }
                        }
                        repeat(4 - linha.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }

        item { SectionTitle(stringResource(R.string.sec_armazenamento)) }
        item {
            Card(Modifier.padding(horizontal = 16.dp)) {
                volumes.forEach { v ->
                    Row(
                        Modifier.fillMaxWidth().tvFocus(24.dp).clickable { vm.push(Screen.Folder(v.loc)) }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RowIcon(
                            when { !v.removable -> Icons.Filled.PhoneAndroid; v.name.contains("USB", true) -> Icons.Filled.Usb; else -> Icons.Filled.SdCard },
                            Lx.ouro,
                        )
                        Column(Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(v.name, style = MaterialTheme.typography.bodyLarge)
                            if (v.total > 0) {
                                Text(stringResource(R.string.op_tam_de, Fmt.size(v.total - v.free), Fmt.size(v.total)), fontSize = 12.sp, color = Lx.tenue)
                                Spacer(Modifier.height(6.dp)); UsageBar(v.total - v.free, v.total)
                            }
                        }
                    }
                }
            }
        }

        // Oculta nas configurações: some do início (continua em Configurações).
        if (com.mauricio.libertyx.folder.BuildConfig.PASTA_SEGURA && cofreVisivel) item {
            val aberto by com.mauricio.libertyx.folder.vault.Vault.unlocked.collectAsState()
            val vv by com.mauricio.libertyx.folder.vault.Vault.versao.collectAsState()
            Column(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp).fillMaxWidth()
                    .vidro(borda = Lx.ouro.copy(alpha = 0.30f)).padding(vertical = 6.dp)
            ) {
                val sub = remember(aberto, vv) {
                    when {
                        !com.mauricio.libertyx.folder.vault.Vault.isSetUp -> Textos.get(R.string.cofre_sub_criar)
                        aberto -> com.mauricio.libertyx.folder.vault.Vault.snapshot().count { !it.isDir }.let { Textos.plural(R.plurals.cofre_sub_aberta, it, it) }
                        else -> Textos.get(R.string.cofre_bloqueada_curto)
                    }
                }
                NetRow(if (aberto) Icons.Filled.LockOpen else Icons.Filled.Lock, stringResource(R.string.cofre_nome), sub) { abrirPastaSegura(vm) }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle(stringResource(R.string.sec_rede))
                if (com.mauricio.libertyx.folder.BuildConfig.PRO) SeloDoPro(Modifier.padding(top = 14.dp))
                Spacer(Modifier.weight(1f))
                if (buscando) CircularProgressIndicator(Modifier.padding(top = 16.dp, end = 12.dp).size(16.dp), strokeWidth = 2.dp, color = Lx.ouro)
                else IconButton(onClick = { scope.launch { Descobertas.buscar(ctx.applicationContext) } }, Modifier.padding(top = 14.dp).tvFocus(24.dp)) {
                    Icon(Icons.Filled.Refresh, stringResource(R.string.buscar_de_novo), tint = Lx.apagado)
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 16.dp)) {
                servidores.forEach { s ->
                    NetRow(Icons.Filled.Lan, s.name, s.displayHost + if (s.isGuest) stringResource(R.string.rede_convidado) else if (s.username.isNotBlank()) " · ${s.username}" else "") {
                        rede { vm.push(Screen.Folder(Loc.Smb(s.id))) }
                    }
                }
                val salvos = servidores.map { it.host }.toSet()
                achados.filter { it.host !in salvos }.forEach { a ->
                    NetRow(Icons.Filled.Computer, a.nome, if (a.nome != a.host) stringResource(R.string.rede_achado_em, a.host) else stringResource(R.string.rede_achado), destaque = true) {
                        rede { servidorNovo = ServerDraft(nome = a.nome, host = a.host) }
                    }
                }
                if (servidores.isEmpty() && achados.isEmpty()) {
                    Text(
                        if (buscando) stringResource(R.string.rede_procurando) else stringResource(R.string.rede_nenhum),
                        Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = Lx.tenue, fontSize = 14.sp,
                    )
                }
                NetRow(Icons.Filled.Add, stringResource(R.string.rede_servidores), stringResource(R.string.rede_servidores_sub)) { rede { vm.push(Screen.Network()) } }
            }
        }
    }

    if (buscaTv) NameDialog(stringResource(R.string.pesquisar_aparelho), "", stringResource(R.string.pesquisar), { buscaTv = false }) { t ->
        buscaTv = false; vm.push(Screen.Search(t, null))
    }
    servidorNovo?.let { d ->
        ServerDialog(d, onDismiss = { servidorNovo = null }) { s -> servidorNovo = null; vm.push(Screen.Folder(Loc.Smb(s.id))) }
    }
}

@Composable
private fun CategoryTile(nome: String, icone: ImageVector, cor: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.tvFocus(16.dp, 1.05f).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Selo(cor, 50.dp, 16.dp) { Icon(icone, null, tint = cor, modifier = Modifier.size(25.dp)) }
        Text(nome, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Lx.apagado,
            modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun NetRow(icon: ImageVector, titulo: String, sub: String, destaque: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().tvFocus(24.dp).clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, if (destaque) Lx.verde else Lx.ouro)
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 12.sp, lineHeight = 16.sp, color = Lx.tenue, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
