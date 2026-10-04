package com.mauricio.libertyx.folder.ui

import android.widget.Toast
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mauricio.libertyx.folder.Textos
import com.mauricio.libertyx.folder.R
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.FolderOff
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.libertyx.folder.fs.FileEntry
import com.mauricio.libertyx.folder.fs.Fss
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.media.Categories
import com.mauricio.libertyx.folder.media.Storage
import com.mauricio.libertyx.folder.ops.Ops
import com.mauricio.libertyx.folder.smb.SmbServerStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

/** Pasta (do aparelho ou do servidor), categoria ou resultado de busca — tudo é uma lista de arquivos. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingScreen(vm: MainVm, screen: Screen) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val pasta = (screen as? Screen.Folder)?.loc

    var itens by remember(screen) { mutableStateOf<List<FileEntry>?>(null) }
    var erro by remember(screen) { mutableStateOf<String?>(null) }
    var carregando by remember(screen) { mutableStateOf(false) }
    var recarga by remember(screen) { mutableIntStateOf(0) }
    val versao by Ops.versao.collectAsState()

    LaunchedEffect(screen, versao, recarga) {
        carregando = true; erro = null
        try {
            when (screen) {
                is Screen.Folder -> itens = Fss.of(screen.loc).list(screen.loc)
                is Screen.Cat -> itens = Categories.list(ctx, screen.cat)
                is Screen.Search -> {
                    if (screen.dentro == null) {
                        itens = withContext(Dispatchers.IO) { Categories.search(ctx, screen.termo) }
                    } else {
                        // Varre subpastas e mostra o que acha enquanto acha.
                        val achados = mutableListOf<FileEntry>()
                        itens = emptyList()
                        suspend fun varrer(l: Loc, nivel: Int) {
                            coroutineContext.ensureActive()
                            val filhos = runCatching { Fss.of(l).list(l) }.getOrDefault(emptyList())
                            val bate = filhos.filter { it.name.contains(screen.termo, ignoreCase = true) }
                            if (bate.isNotEmpty()) { achados += bate; itens = achados.toList() }
                            if (nivel < 12) filhos.filter { it.isDir && !it.name.startsWith(".") }.forEach { varrer(it.loc, nivel + 1) }
                        }
                        varrer(screen.dentro, 0)
                    }
                }
                else -> {}
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            erro = e.message ?: e.toString()
        } finally {
            carregando = false
        }
    }

    val sel = remember(screen) { mutableStateMapOf<Loc, FileEntry>() }
    var filtrando by remember(screen) { mutableStateOf(false) }
    var filtro by remember(screen) { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var menuSel by remember { mutableStateOf(false) }
    var dialogo by remember { mutableStateOf<String?>(null) }

    BackHandler(sel.isNotEmpty()) { sel.clear() }
    BackHandler(filtrando && sel.isEmpty()) { filtrando = false; filtro = "" }

    val servidor = (pasta as? Loc.Smb)?.let { SmbServerStore.get(ctx).byId(it.serverId) }
    val titulo = when (screen) {
        is Screen.Folder -> when (val l = screen.loc) {
            is Loc.Local -> if (l.path == l.root) Storage.volumes(ctx).firstOrNull { it.path == l.root }?.name ?: l.path else l.name
            is Loc.Smb -> if (l.isServerRoot) servidor?.name ?: stringResource(R.string.servidor) else l.name
            is Loc.Vault -> if (l.path.isEmpty()) stringResource(R.string.cofre_nome) else l.name
        }
        is Screen.Cat -> stringResource(screen.cat.titulo)
        is Screen.Search -> "“${screen.termo}”"
        else -> ""
    }
    val mostrados = remember(itens, filtro, Prefs.sort, Prefs.asc, Prefs.hidden) {
        val base = itens ?: emptyList()
        val f = if (filtro.isBlank()) base else base.filter { it.name.contains(filtro.trim(), ignoreCase = true) }
        if (screen is Screen.Folder) Prefs.ordenar(f) else f.filter { Prefs.hidden || !it.hidden }
    }
    val selecionando = sel.isNotEmpty()

    fun abrir(e: FileEntry) {
        if (selecionando) { if (sel.containsKey(e.loc)) sel.remove(e.loc) else sel[e.loc] = e; return }
        if (e.isDir) vm.push(Screen.Folder(e.loc)) else Open.open(ctx, e)
    }
    fun marcar(e: FileEntry) { if (sel.containsKey(e.loc)) sel.remove(e.loc) else sel[e.loc] = e }

    fun erroToast(e: Throwable) = Toast.makeText(ctx, e.message ?: e.toString(), Toast.LENGTH_LONG).show()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    navigationIcon = {
                        IconButton(modifier = Modifier.tvFocus(24.dp), onClick = {
                            when { selecionando -> sel.clear(); filtrando -> { filtrando = false; filtro = "" }; else -> vm.pop() }
                        }) { Icon(if (selecionando) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar)) }
                    },
                    title = {
                        when {
                            selecionando -> Text(pluralStringResource(R.plurals.n_selecionados, sel.size, sel.size))
                            filtrando -> {
                                val foco = remember { FocusRequester() }
                                TextField(
                                    filtro, { filtro = it }, placeholder = { Text(stringResource(R.string.pesquisar_pasta)) }, singleLine = true,
                                    modifier = Modifier.fillMaxWidth().focusRequester(foco),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                        cursorColor = Lx.ouro,
                                    ),
                                )
                                LaunchedEffect(Unit) { foco.requestFocus() }
                            }
                            else -> Text(titulo, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
                        }
                    },
                    actions = {
                        if (selecionando) {
                            TextButton(modifier = Modifier.tvFocus(24.dp), onClick = {
                                if (sel.size == mostrados.size) sel.clear() else mostrados.forEach { sel[it.loc] = it }
                            }) { Text(stringResource(if (sel.size == mostrados.size) R.string.sel_nenhum else R.string.sel_tudo)) }
                        } else if (filtrando) {
                            if (pasta != null && filtro.isNotBlank()) IconButton(modifier = Modifier.tvFocus(24.dp), onClick = {
                                vm.push(Screen.Search(filtro.trim(), pasta)); filtrando = false; filtro = ""
                            }) { Icon(Icons.Filled.SubdirectoryArrowRight, stringResource(R.string.pesquisar_subpastas)) }
                        } else {
                            if (screen !is Screen.Search) IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { filtrando = true }) { Icon(Icons.Filled.Search, stringResource(R.string.pesquisar)) }
                            IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { Prefs.grid = !Prefs.grid }) {
                                Icon(if (Prefs.grid) Icons.Filled.ViewList else Icons.Filled.GridView, stringResource(R.string.visualizacao))
                            }
                            Box {
                                IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, stringResource(R.string.mais)) }
                                DropdownMenu(menu, { menu = false }) {
                                    val podeCriar = pasta != null && !(pasta is Loc.Smb && pasta.isServerRoot)
                                    if (podeCriar) DropdownMenuItem({ Text(stringResource(R.string.nova_pasta)) }, { menu = false; dialogo = "nova" })
                                    DropdownMenuItem({ Text(stringResource(R.string.selecionar)) }, { menu = false; mostrados.firstOrNull()?.let { sel[it.loc] = it } })
                                    DropdownMenuItem({ Text(stringResource(R.string.ordenar_exibir)) }, { menu = false; dialogo = "ordenar" })
                                    DropdownMenuItem({ Text(stringResource(R.string.atualizar)) }, { menu = false; recarga++ })
                                    if (pasta is Loc.Smb) DropdownMenuItem({ Text(stringResource(R.string.reconectar)) }, {
                                        menu = false; com.mauricio.libertyx.folder.smb.SmbFs.forget(pasta.serverId); recarga++
                                    })
                                    if (pasta is Loc.Vault) {
                                        DropdownMenuItem({ Text(stringResource(R.string.cofre_config)) }, { menu = false; vm.push(Screen.Settings()) })
                                        DropdownMenuItem({ Text(stringResource(R.string.cofre_bloquear)) }, {
                                            menu = false; com.mauricio.libertyx.folder.vault.Vault.lock(); vm.esquecerCofre()
                                        })
                                    }
                                    DropdownMenuItem({ Text(stringResource(R.string.inicio)) }, { menu = false; vm.home() })
                                }
                            }
                        }
                    },
                )
                if (pasta != null && !selecionando) Breadcrumbs(vm, pasta, servidor?.name)
                if (carregando && !itens.isNullOrEmpty()) LinearProgressIndicator(
                    Modifier.fillMaxWidth().height(2.dp), color = Lx.ouro, trackColor = Color.Transparent)
                else androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
            }
        },
        bottomBar = {
            val clip = vm.clip
            when {
                selecionando -> SelectionBar(
                    itens = sel.values.toList(),
                    onMover = { vm.clip = Clip(sel.values.toList(), true); sel.clear() },
                    onCopiar = { vm.clip = Clip(sel.values.toList(), false); sel.clear() },
                    onShare = { Open.share(ctx, sel.values.toList()) },
                    onExcluir = { dialogo = "excluir" },
                    menuAberto = menuSel, setMenu = { menuSel = it },
                    onRenomear = { dialogo = "renomear" },
                    onDetalhes = { dialogo = "detalhes" },
                    onAbrirCom = { sel.values.firstOrNull()?.let { Open.open(ctx, it, escolher = true) } },
                    onCofre = {
                        val itensSel = sel.values.toList()
                        sel.clear()
                        if (itensSel.all { it.loc is Loc.Vault }) Ops.restore(ctx, itensSel)
                        else comCofre(vm) { Ops.transfer(ctx, itensSel, Loc.Vault(), mover = true) }
                    },
                )
                clip != null -> PasteBar(
                    clip = clip,
                    habilitado = pasta != null && !(pasta is Loc.Smb && pasta.isServerRoot) && !Ops.ocupado,
                    onCancelar = { vm.clip = null },
                    onColar = {
                        Ops.transfer(ctx, clip.itens, pasta!!, clip.mover)
                        vm.clip = null
                    },
                )
            }
        },
    ) { pad ->
        // Na TV não há gesto de puxar, e o indicador aparecia sozinho ao rolar pelo controle.
        val corpo: @Composable () -> Unit = {
            when {
                itens == null && carregando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        if (pasta is Loc.Smb) Text(stringResource(R.string.conectando_a, servidor?.name ?: stringResource(R.string.servidor)), Modifier.padding(top = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                erro != null -> LazyColumn(Modifier.fillMaxSize()) { item {
                    EmptyState(stringResource(R.string.nao_abriu), erro, when {
                        erro == Textos.get(R.string.smb_acesso_negado) -> Icons.Filled.Lock
                        pasta is Loc.Smb -> Icons.Filled.WifiOff
                        else -> Icons.Filled.FolderOff
                    })
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        TextButton(modifier = Modifier.tvFocus(24.dp), onClick = { (pasta as? Loc.Smb)?.let { com.mauricio.libertyx.folder.smb.SmbFs.forget(it.serverId) }; recarga++ }) { Text(stringResource(R.string.tentar_de_novo)) }
                    }
                } }
                mostrados.isEmpty() && !carregando -> LazyColumn(Modifier.fillMaxSize()) { item {
                    EmptyState(stringResource(if (filtro.isNotBlank() || screen is Screen.Search) R.string.nada_encontrado else R.string.nenhum_arquivo))
                } }
                Prefs.grid -> LazyVerticalGrid(
                    GridCells.Adaptive(112.dp), state = screen.gridState, modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    items(mostrados, key = { it.loc.toString() }) { e ->
                        FileCell(e, selecionando, sel.containsKey(e.loc), { abrir(e) }, { marcar(e) })
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize(), state = screen.listState, contentPadding = PaddingValues(bottom = 90.dp)) {
                    if (screen is Screen.Search && carregando) item {
                        Text(stringResource(R.string.pesquisando), Modifier.padding(20.dp, 8.dp), color = Lx.tenue)
                    }
                    items(mostrados, key = { it.loc.toString() }) { e ->
                        val sub = if (screen is Screen.Folder) null else listOfNotNull(
                            descrever(ctx, e.loc.parent() ?: e.loc).substringAfterLast("/0/").ifEmpty { null },
                            if (!e.isDir) Fmt.size(e.size) else null,
                        ).joinToString("  ·  ")
                        FileRow(e, selecionando, sel.containsKey(e.loc), { abrir(e) }, { marcar(e) }, sub)
                    }
                }
            }
        }
        if (Device.isTv) Box(Modifier.fillMaxSize().padding(pad)) { corpo() }
        else PullToRefreshBox(
            isRefreshing = carregando && itens != null, onRefresh = { recarga++ },
            modifier = Modifier.fillMaxSize().padding(pad),
        ) { corpo() }
    }

    val alvo = sel.values.toList()
    when (dialogo) {
        "nova" -> NameDialog(stringResource(R.string.nova_pasta), "", stringResource(R.string.criar), { dialogo = null }) { nome ->
            dialogo = null
            scope.launch { runCatching { Fss.of(pasta!!).mkdir(pasta, nome) }.onSuccess { recarga++ }.onFailure(::erroToast) }
        }
        "renomear" -> alvo.singleOrNull()?.let { e ->
            NameDialog(stringResource(R.string.renomear), e.name, stringResource(R.string.renomear), { dialogo = null }) { nome ->
                dialogo = null; sel.clear()
                scope.launch { runCatching { Fss.of(e.loc).rename(e.loc, nome) }.onSuccess { recarga++ }.onFailure(::erroToast) }
            }
        }
        "excluir" -> ConfirmDialog(
            pluralStringResource(R.plurals.excluir_titulo, alvo.size, alvo.size),
            stringResource(when {
                alvo.any { it.loc is Loc.Smb } -> R.string.excluir_servidor
                alvo.any { it.loc is Loc.Vault } -> R.string.excluir_cofre
                else -> R.string.nao_desfaz
            }),
            stringResource(R.string.excluir), { dialogo = null },
        ) { dialogo = null; Ops.delete(ctx, alvo); sel.clear() }
        "detalhes" -> DetailsDialog(alvo) { dialogo = null }
        "ordenar" -> SortDialog { dialogo = null }
    }
}

@Composable
private fun Breadcrumbs(vm: MainVm, loc: Loc, nomeServidor: String?) {
    val ctx = LocalContext.current
    val cadeia = remember(loc) { generateSequence(loc) { it.parent() }.toList().reversed() }
    val rolagem = rememberScrollState()
    LaunchedEffect(loc) { rolagem.scrollTo(Int.MAX_VALUE) }
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rolagem).padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        cadeia.forEachIndexed { i, l ->
            val nome = when {
                l is Loc.Local && l.path == l.root -> Storage.volumes(ctx).firstOrNull { it.path == l.root }?.name ?: stringResource(R.string.raiz)
                l is Loc.Smb && l.isServerRoot -> nomeServidor ?: stringResource(R.string.servidor)
                l is Loc.Vault && l.path.isEmpty() -> stringResource(R.string.cofre_nome)
                else -> l.name
            }
            val ultimo = i == cadeia.lastIndex
            Text(
                nome, fontSize = 13.sp, maxLines = 1,
                color = if (ultimo) Lx.ouro else Lx.apagado,
                fontWeight = if (ultimo) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier
                    .then(if (ultimo) Modifier.vidro(12.dp, Lx.ouro.copy(alpha = 0.10f), Lx.ouro.copy(alpha = 0.28f))
                          else Modifier.vidro(12.dp))
                    .tvFocus(12.dp).clickable(enabled = !ultimo) { vm.jump(l) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            if (!ultimo) Icon(Icons.Filled.ChevronRight, null, Modifier.size(14.dp).padding(horizontal = 1.dp), tint = Lx.tenue)
        }
    }
}

@Composable
private fun BarAction(icon: ImageVector, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Column(
        Modifier.tvFocus(14.dp).clickable(enabled = enabled, onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val cor = if (enabled) Lx.texto else Lx.tenue
        Icon(icon, label, tint = cor)
        Text(label, fontSize = 11.sp, color = cor)
    }
}

@Composable
private fun SelectionBar(
    itens: List<FileEntry>, onMover: () -> Unit, onCopiar: () -> Unit, onShare: () -> Unit, onExcluir: () -> Unit,
    menuAberto: Boolean, setMenu: (Boolean) -> Unit, onRenomear: () -> Unit, onDetalhes: () -> Unit, onAbrirCom: () -> Unit,
    onCofre: () -> Unit,
) {
    val noCofre = itens.all { it.loc is Loc.Vault }
    val temShare = itens.any { it.isShare }
    Box(Modifier.background(Lx.superficie).drawBehind {
        drawLine(Lx.vidroBorda, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1f)
    }) {
        Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            BarAction(Icons.Filled.DriveFileMove, stringResource(R.string.mover), !temShare, onMover)
            BarAction(Icons.Filled.ContentCopy, stringResource(R.string.copiar), true, onCopiar)
            BarAction(Icons.Filled.Share, stringResource(R.string.compartilhar), itens.any { !it.isDir }, onShare)
            BarAction(Icons.Filled.Delete, stringResource(R.string.excluir), !temShare, onExcluir)
            Box {
                BarAction(Icons.Filled.MoreVert, stringResource(R.string.mais)) { setMenu(true) }
                DropdownMenu(menuAberto, { setMenu(false) }) {
                    DropdownMenuItem({ Text(stringResource(R.string.renomear)) }, { setMenu(false); onRenomear() }, enabled = itens.size == 1 && !temShare)
                    DropdownMenuItem({ Text(stringResource(R.string.abrir_com_item)) }, { setMenu(false); onAbrirCom() }, enabled = itens.size == 1 && !itens[0].isDir)
                    // Pasta oculta e bloqueada: a opção nem aparece, para não denunciar que ela existe.
                    // Na TV não há pasta segura: só "retirar", para quem tinha uma de antes.
                    if (noCofre || (com.mauricio.libertyx.folder.BuildConfig.PASTA_SEGURA &&
                            (com.mauricio.libertyx.folder.vault.Vault.visivel || com.mauricio.libertyx.folder.vault.Vault.unlocked.value)))
                    DropdownMenuItem({ Text(stringResource(if (noCofre) R.string.cofre_retirar else R.string.cofre_mover)) },
                        { setMenu(false); onCofre() }, enabled = noCofre || (!temShare && itens.none { it.loc is Loc.Vault }))
                    DropdownMenuItem({ Text(stringResource(R.string.detalhes)) }, { setMenu(false); onDetalhes() })
                }
            }
        }
    }
}

@Composable
fun PasteBar(clip: Clip, habilitado: Boolean, onCancelar: () -> Unit, onColar: (() -> Unit)?) {
    Box(Modifier.background(Lx.superficie).drawBehind {
        drawLine(Lx.vidroBorda, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1f)
    }) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Text(
                pluralStringResource(if (clip.mover) R.plurals.colar_mover else R.plurals.colar_copiar, clip.itens.size, clip.itens.size),
                Modifier.padding(start = 20.dp, top = 12.dp, end = 20.dp), fontSize = 13.sp, color = Lx.apagado,
            )
            Row(Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = onCancelar, Modifier.weight(1f).tvFocus(24.dp)) { Text(stringResource(R.string.cancelar)) }
                HorizontalDivider(Modifier.height(24.dp).size(1.dp, 24.dp).align(Alignment.CenterVertically))
                TextButton(onClick = { onColar?.invoke() }, Modifier.weight(1f).tvFocus(24.dp), enabled = habilitado && onColar != null) {
                    Text(stringResource(if (clip.mover) R.string.mover_para_ca else R.string.copiar_para_ca), fontWeight = FontWeight.Bold, color = Lx.ouro)
                }
            }
        }
    }
}

