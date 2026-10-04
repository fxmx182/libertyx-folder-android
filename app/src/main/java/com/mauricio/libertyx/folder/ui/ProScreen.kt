package com.mauricio.libertyx.folder.ui

import android.app.Activity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mauricio.libertyx.folder.R
import com.mauricio.libertyx.folder.pro.Loja
import com.mauricio.libertyx.folder.pro.Pro

/** O que o Pro libera, na ordem em que a tela e o guia mostram. */
val VantagensDoPro: List<Pair<ImageVector, Int>> = listOf(
    Icons.Filled.Lan to R.string.pro_v_smb,
    Icons.Filled.Radar to R.string.pro_v_busca,
    Icons.Filled.PlayCircle to R.string.pro_v_abrir,
    Icons.Filled.SwapHoriz to R.string.pro_v_copiar,
)

/**
 * A tela do Pro: o que ele libera, quanto custa e o botão de comprar.
 *
 * Feita para o controle remoto: duas colunas (a TV é larga e baixa), o botão
 * de comprar já com o foco ao abrir, e nada que dependa de toque. Sem
 * contagem regressiva piscando nem "oferta por tempo limitado": o preço é o
 * mesmo sempre, e quem decide é quem está no sofá.
 */
@Composable
fun ProScreen(vm: MainVm) {
    val ctx = LocalContext.current
    val estado by Pro.estado.collectAsState()
    val situacao by Loja.situacao.collectAsState()
    val pendente by Loja.pendente.collectAsState()
    val foco = remember { FocusRequester() }

    // Ao abrir, pergunta de novo à Play: uma compra pendente pode ter compensado.
    LaunchedEffect(Unit) { Loja.atualizar() }
    LaunchedEffect(estado, situacao) { runCatching { foco.requestFocus() } }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        IconButton(onClick = { vm.pop() }, Modifier.padding(8.dp).tvFocus(24.dp)) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.voltar))
        }
        Row(
            // Tudo numa tela só, sem rolar: na TV, conteúdo que rola some de
            // vista quando o foco desce até o botão de comprar.
            Modifier.fillMaxSize().padding(start = 40.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // A marca, grande, com a mesma luz de ouro do ícone.
            Column(Modifier.weight(0.38f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Box(
                    Modifier.size(200.dp).background(
                        Brush.radialGradient(listOf(Lx.ouro.copy(alpha = 0.22f), Color.Transparent)), CircleShape,
                    ),
                    contentAlignment = Alignment.Center,
                ) { Image(painterResource(R.drawable.ic_marca), null, Modifier.size(132.dp)) }
                Text(
                    buildAnnotatedString {
                        append("LibertyX")
                        withStyle(SpanStyle(color = Lx.ouro)) { append(" Folder") }
                    },
                    style = Lx.titulao.copy(fontSize = 26.sp), modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    "PRO", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.sp,
                    color = Color(0xFF1A1200),
                    modifier = Modifier.padding(top = 10.dp).clip(RoundedCornerShape(8.dp))
                        .background(Brush.horizontalGradient(listOf(Lx.ouroClaro, Lx.ouro, Lx.ouroEscuro)))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                )
            }

            Column(
                Modifier.weight(0.62f).widthIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val (titulo, sub) = when (val e = estado) {
                    Pro.Estado.Comprado -> stringResource(R.string.pro_titulo_ativo) to stringResource(R.string.pro_sub_ativo)
                    is Pro.Estado.Teste -> pluralStringResource(R.plurals.pro_titulo_teste, e.diasRestantes, e.diasRestantes) to
                        stringResource(R.string.pro_sub_teste, Pro.DIAS_DE_TESTE)
                    Pro.Estado.Expirado -> stringResource(R.string.pro_titulo_expirado) to stringResource(R.string.pro_sub_expirado)
                }
                Text(titulo, fontSize = 23.sp, lineHeight = 27.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp)
                Text(sub, fontSize = 13.sp, lineHeight = 17.sp, color = Lx.apagado)

                Column(Modifier.fillMaxWidth().vidro().padding(vertical = 4.dp)) {
                    VantagensDoPro.forEach { (icone, texto) ->
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Selo(Lx.ouro, 28.dp, 9.dp) { Icon(icone, null, tint = Lx.ouro, modifier = Modifier.size(15.dp)) }
                            Text(stringResource(texto), fontSize = 13.sp, modifier = Modifier.padding(start = 12.dp))
                        }
                    }
                }
                Text(
                    stringResource(R.string.pro_gratis_sempre),
                    fontSize = 11.sp, lineHeight = 14.sp, color = Lx.tenue,
                )
                if (estado != Pro.Estado.Comprado) {
                    when (val s = situacao) {
                        Loja.Situacao.Conectando -> Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Lx.ouro)
                            Text(stringResource(R.string.pro_consultando), Modifier.padding(start = 12.dp), color = Lx.apagado)
                        }
                        Loja.Situacao.Indisponivel -> {
                            Text(
                                stringResource(R.string.pro_indisponivel),
                                fontSize = 12.sp, lineHeight = 16.sp, color = Lx.apagado,
                            )
                            Button(
                                onClick = { Loja.atualizar() },
                                modifier = Modifier.focusRequester(foco).tvFocus(24.dp),
                            ) { Text(stringResource(R.string.tentar_de_novo)) }
                        }
                        is Loja.Situacao.Pronta -> {
                            Button(
                                onClick = {
                                    val ok = (ctx as? Activity)?.let { Loja.comprar(it) } ?: false
                                    if (!ok) Toast.makeText(ctx, ctx.getString(R.string.pro_play_falhou), Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Lx.ouro, contentColor = Color(0xFF1A1200)),
                                modifier = Modifier.fillMaxWidth().height(52.dp).focusRequester(foco).tvFocus(26.dp),
                            ) {
                                Text(stringResource(R.string.pro_comprar, s.preco), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                            Text(stringResource(R.string.pro_compra_unica), fontSize = 12.sp, color = Lx.tenue)
                        }
                    }
                    if (pendente) Text(
                        stringResource(R.string.pro_pendente),
                        fontSize = 13.sp, color = Lx.ouroClaro,
                    )
                    TextButton(
                        onClick = {
                            Loja.atualizar()
                            Toast.makeText(ctx, ctx.getString(R.string.pro_procurando), Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.tvFocus(24.dp),
                    ) { Text(stringResource(R.string.pro_restaurar)) }
                } else {
                    Button(
                        onClick = { vm.pop() },
                        modifier = Modifier.focusRequester(foco).tvFocus(24.dp),
                    ) { Text(stringResource(R.string.voltar)) }
                }
            }
        }
    }
}

/**
 * O selo de status do Pro, no início e nas Configurações: quantos dias de
 * teste faltam, ou que a rede está travada. Some de vez depois da compra.
 */
@Composable
fun SeloDoPro(modifier: Modifier = Modifier) {
    val estado by Pro.estado.collectAsState()
    val texto = when (val e = estado) {
        Pro.Estado.Comprado -> return
        is Pro.Estado.Teste -> pluralStringResource(R.plurals.pro_selo_teste, e.diasRestantes, e.diasRestantes)
        Pro.Estado.Expirado -> "PRO"
    }
    Text(
        texto, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.4.sp, color = Lx.ouro,
        modifier = modifier.border(1.dp, Lx.ouro.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
            .background(Lx.ouro.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
