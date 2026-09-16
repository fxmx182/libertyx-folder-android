package com.mauricio.arquivos.ui

import android.os.Environment
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.arquivos.fs.FileEntry
import com.mauricio.arquivos.media.Categories
import com.mauricio.arquivos.media.Category
import com.mauricio.arquivos.media.Storage
import com.mauricio.arquivos.media.Volume
import com.mauricio.arquivos.ops.Ops
import com.mauricio.arquivos.smb.Descobertas
import com.mauricio.arquivos.smb.SmbServerStore
import com.mauricio.arquivos.fs.Loc
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
            Column(Modifier.statusBarsPadding().padding(horizontal = 24.dp).padding(top = 48.dp, bottom = 20.dp)) {
                Text("Arquivos", fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
        }
        item {
            Surface(
                Modifier.padding(horizontal = 16.dp).fillMaxWidth(), shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                TextField(
                    termo, { termo = it }, singleLine = true,
                    placeholder = { Text("Pesquisar no aparelho") },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { if (termo.isNotBlank()) vm.push(Screen.Search(termo.trim(), null)) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (recentes.isNotEmpty()) {
            item { SectionTitle("Arquivos recentes") }
            item {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recentes, key = { it.loc.toString() }) { e ->
                        Column(Modifier.width(96.dp).clip(RoundedCornerShape(14.dp)).clickable { Open.open(ctx, e) }) {
                            Thumb(e, 96.dp, corner = 14.dp)
                            Text(e.name, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp, start = 2.dp))
                        }
                    }
                }
            }
        }

        item { SectionTitle("Categorias") }
        item {
            Card(Modifier.padding(horizontal = 16.dp)) {
                val cats = listOf(
                    Triple(Category.Images, Icons.Filled.Image, Color(0xFF9C5BE8)),
                    Triple(Category.Videos, Icons.Filled.VideoLibrary, Color(0xFFFF4F7B)),
                    Triple(Category.Audio, Icons.Filled.Audiotrack, Color(0xFF16B3A5)),
                    Triple(Category.Documents, Icons.Filled.Description, Color(0xFFF07A1E)),
                    Triple(Category.Downloads, Icons.Filled.Download, Color(0xFF0A6CFF)),
                    Triple(Category.Apks, Icons.Filled.Android, Color(0xFF6DBE3F)),
                    Triple(Category.Compressed, Icons.Filled.FolderZip, Color(0xFFB07A3A)),
                )
                cats.chunked(4).forEach { linha ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        linha.forEach { (c, icone, cor) ->
                            CategoryTile(c.titulo.replace("Arquivos de instalação", "Instalação"), icone, cor, Modifier.weight(1f)) {
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

        item { SectionTitle("Armazenamento") }
        item {
            Card(Modifier.padding(horizontal = 16.dp)) {
                volumes.forEach { v ->
                    Row(
                        Modifier.fillMaxWidth().clickable { vm.push(Screen.Folder(v.loc)) }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RowIcon(
                            when { !v.removable -> Icons.Filled.PhoneAndroid; v.name.contains("USB", true) -> Icons.Filled.Usb; else -> Icons.Filled.SdCard },
                            Color(0xFF0A6CFF),
                        )
                        Column(Modifier.padding(start = 16.dp).weight(1f)) {
                            Text(v.name, style = MaterialTheme.typography.bodyLarge)
                            if (v.total > 0) {
                                Text("${Fmt.size(v.total - v.free)} de ${Fmt.size(v.total)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(6.dp)); UsageBar(v.total - v.free, v.total)
                            }
                        }
                    }
                }
            }
        }

        item {
            val aberto by com.mauricio.arquivos.vault.Vault.unlocked.collectAsState()
            val vv by com.mauricio.arquivos.vault.Vault.versao.collectAsState()
            Card(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp)) {
                val sub = remember(aberto, vv) {
                    when {
                        !com.mauricio.arquivos.vault.Vault.isSetUp -> "Guarde arquivos cifrados, protegidos por PIN"
                        aberto -> "Desbloqueada · ${com.mauricio.arquivos.vault.Vault.snapshot().count { !it.isDir }} arquivos"
                        else -> "Bloqueada"
                    }
                }
                NetRow(if (aberto) Icons.Filled.LockOpen else Icons.Filled.Lock, "Pasta segura", sub) { abrirPastaSegura(vm) }
            }
        }

        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Rede", Modifier.weight(1f))
                if (buscando) CircularProgressIndicator(Modifier.padding(top = 12.dp, end = 12.dp).size(16.dp), strokeWidth = 2.dp)
                else IconButton(onClick = { scope.launch { Descobertas.buscar(ctx.applicationContext) } }, Modifier.padding(top = 12.dp)) {
                    Icon(Icons.Filled.Refresh, "Buscar de novo", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Card(Modifier.padding(horizontal = 16.dp)) {
                servidores.forEach { s ->
                    NetRow(Icons.Filled.Lan, s.name, s.displayHost + if (s.isGuest) " · convidado" else if (s.username.isNotBlank()) " · ${s.username}" else "") {
                        vm.push(Screen.Folder(Loc.Smb(s.id)))
                    }
                }
                val salvos = servidores.map { it.host }.toSet()
                achados.filter { it.host !in salvos }.forEach { a ->
                    NetRow(Icons.Filled.Computer, a.nome, if (a.nome != a.host) "${a.host} · encontrado na rede" else "Encontrado na rede", destaque = true) {
                        servidorNovo = ServerDraft(nome = a.nome, host = a.host)
                    }
                }
                if (servidores.isEmpty() && achados.isEmpty()) {
                    Text(
                        if (buscando) "Procurando servidores SMB na rede…" else "Nenhum servidor encontrado",
                        Modifier.padding(horizontal = 20.dp, vertical = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                NetRow(Icons.Filled.Add, "Servidores de rede", "Procurar e adicionar servidor SMB") { vm.push(Screen.Network()) }
            }
        }
    }

    servidorNovo?.let { d ->
        ServerDialog(d, onDismiss = { servidorNovo = null }) { s -> servidorNovo = null; vm.push(Screen.Folder(Loc.Smb(s.id))) }
    }
}

@Composable
private fun CategoryTile(nome: String, icone: ImageVector, cor: Color, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(cor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(icone, null, tint = cor, modifier = Modifier.size(26.dp))
        }
        Text(nome, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
fun NetRow(icon: ImageVector, titulo: String, sub: String, destaque: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon, if (destaque) Color(0xFF16B3A5) else Color(0xFF0A6CFF))
        Column(Modifier.padding(start = 16.dp).weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(sub, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
