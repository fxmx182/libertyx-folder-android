package com.mauricio.libertyx.folder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.smb.Descobertas
import com.mauricio.libertyx.folder.smb.SmbFs
import com.mauricio.libertyx.folder.smb.SmbServer
import com.mauricio.libertyx.folder.smb.SmbServerStore
import com.rapid7.client.dcerpc.mssrvs.ServerService
import com.rapid7.client.dcerpc.transport.SMBTransportFactories
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ServerDraft(
    val id: String? = null, val nome: String = "", val host: String = "", val porta: Int = 445,
    val usuario: String = "", val senha: String = "", val dominio: String = "", val convidado: Boolean = false,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScreen(vm: MainVm, screen: Screen.Network) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val store = SmbServerStore.get(ctx)
    val servidores by store.servers.collectAsState()
    val achados by Descobertas.achados.collectAsState()
    val buscando by Descobertas.buscando.collectAsState()
    var rascunho by remember { mutableStateOf<ServerDraft?>(null) }
    var remover by remember { mutableStateOf<SmbServer?>(null) }

    LaunchedEffect(Unit) { scope.launch { Descobertas.buscar(ctx.applicationContext) } }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                navigationIcon = { IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { vm.pop() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Voltar") } },
                title = { Text("Servidores de rede", fontWeight = FontWeight.SemiBold) },
                actions = {
                    if (buscando) CircularProgressIndicator(Modifier.padding(end = 16.dp).size(20.dp), strokeWidth = 2.dp)
                    else IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { scope.launch { Descobertas.buscar(ctx.applicationContext) } }) { Icon(Icons.Filled.Refresh, "Buscar") }
                },
            )
        },
    ) { pad ->
        LazyColumn(Modifier.fillMaxSize().padding(pad), contentPadding = PaddingValues(bottom = 100.dp)) {
            if (servidores.isNotEmpty()) {
                item { SectionTitle("Salvos") }
                item {
                    Card(Modifier.padding(horizontal = 16.dp)) {
                        servidores.forEach { s ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f)) {
                                    NetRow(Icons.Filled.Lan, s.name, s.displayHost + if (s.isGuest) " · convidado" else if (s.username.isNotBlank()) " · ${s.username}" else "") {
                                        vm.push(Screen.Folder(Loc.Smb(s.id)))
                                    }
                                }
                                var menu by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Opções") }
                                    DropdownMenu(menu, { menu = false }) {
                                        DropdownMenuItem({ Text("Editar") }, {
                                            menu = false
                                            rascunho = ServerDraft(s.id, s.name, s.host, s.port, s.username, store.password(s), s.domain, s.isGuest)
                                        })
                                        DropdownMenuItem({ Text("Remover") }, { menu = false; remover = s })
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Encontrados nesta rede", Modifier.weight(1f))
                }
            }
            item {
                Card(Modifier.padding(horizontal = 16.dp)) {
                    val salvos = servidores.map { it.host }.toSet()
                    achados.forEach { a ->
                        val jaSalvo = a.host in salvos
                        NetRow(Icons.Filled.Computer, a.nome,
                            (if (a.nome != a.host) a.host else "SMB") + if (jaSalvo) " · já salvo" else "", destaque = !jaSalvo) {
                            val s = servidores.firstOrNull { it.host == a.host }
                            if (s != null) vm.push(Screen.Folder(Loc.Smb(s.id)))
                            else rascunho = ServerDraft(nome = a.nome, host = a.host)
                        }
                    }
                    if (achados.isEmpty()) Text(
                        if (buscando) "Procurando computadores e NAS com compartilhamento…" else
                            "Nada encontrado. Confira se está no mesmo Wi-Fi que o servidor, ou adicione pelo IP.",
                        Modifier.padding(horizontal = 20.dp, vertical = 14.dp), color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Card(Modifier.padding(16.dp)) {
                    NetRow(Icons.Filled.Add, "Adicionar manualmente", "Pelo IP ou nome do servidor") { rascunho = ServerDraft() }
                }
            }
        }
    }

    rascunho?.let { d ->
        ServerDialog(d, onDismiss = { rascunho = null }) { s ->
            rascunho = null
            if (d.id == null) vm.push(Screen.Folder(Loc.Smb(s.id)))
        }
    }
    remover?.let { s ->
        ConfirmDialog("Remover ${s.name}?", "Só o atalho e a senha salvos são removidos; nada é apagado do servidor.", "Remover",
            { remover = null }) { store.remove(s); remover = null }
    }
}

/** Cadastro de servidor: só salva depois de conseguir conectar e listar os compartilhamentos. */
@Composable
fun ServerDialog(inicial: ServerDraft, onDismiss: () -> Unit, onSaved: (SmbServer) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var d by remember { mutableStateOf(inicial) }
    var verSenha by remember { mutableStateOf(false) }
    var avancado by remember { mutableStateOf(inicial.porta != 445 || inicial.dominio.isNotEmpty()) }
    var testando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!testando) onDismiss() },
        title = { Text(if (inicial.id == null) "Servidor SMB" else "Editar servidor") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()).setasTrocamDeCampo()) {
                OutlinedTextField(d.host, { d = d.copy(host = it.trim()) }, label = { Text("Endereço (IP ou nome)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(d.nome, { d = d.copy(nome = it) }, label = { Text("Nome de exibição (opcional)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                Row(Modifier.fillMaxWidth().tvFocus(12.dp).clickable { d = d.copy(convidado = !d.convidado) }.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(d.convidado, null); Text("Entrar como convidado")
                }
                if (!d.convidado) {
                    OutlinedTextField(d.usuario, { d = d.copy(usuario = it) }, label = { Text("Usuário") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        d.senha, { d = d.copy(senha = it) }, label = { Text("Senha") }, singleLine = true,
                        visualTransformation = if (verSenha) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = { IconButton(modifier = Modifier.tvFocus(24.dp), onClick = { verSenha = !verSenha }) { Icon(if (verSenha) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null) } },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
                TextButton(onClick = { avancado = !avancado }, Modifier.tvFocus(24.dp)) { Text(if (avancado) "Ocultar avançado" else "Avançado") }
                if (avancado) {
                    OutlinedTextField(d.porta.toString(), { d = d.copy(porta = it.filter(Char::isDigit).take(5).toIntOrNull() ?: 445) },
                        label = { Text("Porta") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth())
                    if (!d.convidado) OutlinedTextField(d.dominio, { d = d.copy(dominio = it.trim()) }, label = { Text("Domínio / grupo de trabalho") },
                        singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                }
                if (testando) Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Text("  Conectando…")
                }
                erro?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp)) }
            }
        },
        confirmButton = {
            TextButton(modifier = Modifier.tvFocus(24.dp), enabled = d.host.isNotBlank() && !testando, onClick = {
                testando = true; erro = null
                val s = SmbServer(
                    id = d.id ?: java.util.UUID.randomUUID().toString(),
                    name = d.nome.ifBlank { d.host }, host = d.host, port = d.porta,
                    username = if (d.convidado) "" else d.usuario.trim(), domain = d.dominio, isGuest = d.convidado,
                )
                scope.launch {
                    val r = withContext(Dispatchers.IO) {
                        runCatching {
                            val sessao = SmbFs.openSession(s, if (d.convidado) "" else d.senha)
                            try {
                                // Prova de verdade: listar os compartilhamentos, não só autenticar.
                                ServerService(SMBTransportFactories.SRVSVC.getTransport(sessao)).shares1
                            } finally { runCatching { sessao.connection.close(true) } }
                        }
                    }
                    testando = false
                    r.onSuccess {
                        SmbServerStore.get(ctx).save(s, if (d.convidado) "" else d.senha)
                        onSaved(s)
                    }.onFailure { e -> erro = mensagem(e) }
                }
            }) { Text("Conectar") }
        },
        dismissButton = { TextButton(modifier = Modifier.tvFocus(24.dp), onClick = onDismiss, enabled = !testando) { Text("Cancelar") } },
    )
}

private fun mensagem(e: Throwable): String {
    val t = generateSequence(e) { it.cause }.toList()
    val api = t.filterIsInstance<com.hierynomus.mssmb2.SMBApiException>().firstOrNull()
    return when {
        api?.status == com.hierynomus.mserref.NtStatus.STATUS_LOGON_FAILURE -> "Usuário ou senha incorretos."
        api?.status == com.hierynomus.mserref.NtStatus.STATUS_ACCESS_DENIED -> "Acesso negado. Este servidor pode não aceitar convidado."
        t.any { it is java.net.UnknownHostException } -> "Servidor não encontrado. Confira o endereço."
        t.any { it is java.net.ConnectException || it is java.net.SocketTimeoutException } -> "Sem resposta na porta ${445}. O servidor está ligado e na mesma rede?"
        t.any { it is java.util.concurrent.TimeoutException } -> "O servidor não respondeu a tempo. Se escolheu convidado, ele pode exigir usuário e senha."
        api != null -> "O servidor recusou: ${api.status}"
        else -> e.message ?: e.toString()
    }
}
