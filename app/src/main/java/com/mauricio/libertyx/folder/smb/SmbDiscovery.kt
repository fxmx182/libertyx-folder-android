package com.mauricio.libertyx.folder.smb

import android.content.Context
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

/**
 * Acha servidores SMB na rede sozinho — o mesmo desenho que já funciona no
 * LibertyX, com um passo a mais para dar nome às máquinas:
 *
 * - **mDNS** (`_smb._tcp`): NAS, Mac e Linux com Avahi se anunciam com nome.
 * - **Varredura da porta 445** na sub-rede: acha Windows e Samba que não se
 *   anunciam. Só sabe o IP…
 * - …por isso cada IP achado recebe uma **consulta NetBIOS** (UDP 137), que é
 *   como o Windows diz o próprio nome (`DESKTOP-ABC`). Sem resposta, fica o IP.
 *
 * Android de PC vive em rede virtual (10.0.2.x): mDNS não atravessa e a
 * varredura só vê a rede virtual — então varre também a rede dos servidores
 * salvos e as faixas comuns de roteador.
 */
class SmbDiscovery(private val context: Context, private val conhecidos: List<String>) {

    data class Encontrado(val host: String, val nome: String, val viaMdns: Boolean)

    suspend fun procurar(aoEncontrar: (Encontrado) -> Unit) = coroutineScope {
        val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        val lock = runCatching { wifi?.createMulticastLock("arquivos-mdns")?.apply { setReferenceCounted(false); acquire() } }.getOrNull()
        try {
            coroutineScope {
                launch { withTimeoutOrNull(15_000) { porMdns(aoEncontrar) } }
                launch { porVarredura(aoEncontrar) }
            }
        } finally {
            runCatching { lock?.release() }
        }
    }

    private suspend fun porMdns(aoEncontrar: (Encontrado) -> Unit) {
        val nsd = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        suspendCancellableCoroutine<Unit> { cont ->
            val ouvinte = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(t: String) {}
                override fun onDiscoveryStopped(t: String) {}
                override fun onStartDiscoveryFailed(t: String, e: Int) { if (cont.isActive) cont.resumeWith(Result.success(Unit)) }
                override fun onStopDiscoveryFailed(t: String, e: Int) {}
                override fun onServiceLost(info: NsdServiceInfo) {}
                override fun onServiceFound(info: NsdServiceInfo) {
                    @Suppress("DEPRECATION")
                    nsd.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, e: Int) {}
                        override fun onServiceResolved(r: NsdServiceInfo) {
                            @Suppress("DEPRECATION")
                            val ip = (r.host as? Inet4Address)?.hostAddress ?: return
                            aoEncontrar(Encontrado(ip, r.serviceName ?: ip, true))
                        }
                    })
                }
            }
            runCatching { nsd.discoverServices("_smb._tcp.", NsdManager.PROTOCOL_DNS_SD, ouvinte) }
                .onFailure { if (cont.isActive) cont.resumeWith(Result.success(Unit)) }
            cont.invokeOnCancellation { runCatching { nsd.stopServiceDiscovery(ouvinte) } }
        }
    }

    private suspend fun porVarredura(aoEncontrar: (Encontrado) -> Unit) = withContext(Dispatchers.IO) {
        val propria = prefixoDaRede()
        val virtual = propria == null || ehRedeVirtual(propria)
        val alvos = LinkedHashMap<String, Int>()
        propria?.let { alvos[it] = 700 }
        conhecidos.mapNotNull(::prefixoDoEndereco).forEach { alvos.putIfAbsent(it, 700) }
        if (virtual) REDES_DE_CASA.forEach { alvos.putIfAbsent(it, 400) }

        val limite = Semaphore(if (virtual) 96 else 48)
        coroutineScope {
            for ((prefixo, prazo) in alvos) for (n in 1..254) launch {
                val ip = "$prefixo.$n"
                val achou = limite.withPermit { responde(ip, prazo) }
                if (achou) aoEncontrar(Encontrado(ip, nomeNetbios(ip) ?: ip, false))
            }
        }
    }

    private fun responde(host: String, prazo: Int) = runCatching {
        Socket().use { it.connect(InetSocketAddress(host, 445), prazo); true }
    }.getOrDefault(false)

    private fun prefixoDoEndereco(host: String): String? = runCatching {
        val ip = InetAddress.getByName(host) as? Inet4Address ?: return null
        if (!ip.isSiteLocalAddress) return null
        ip.hostAddress?.substringBeforeLast('.')
    }.getOrNull()

    private fun ehRedeVirtual(prefixo: String): Boolean {
        val p = prefixo.split(".").map { it.toIntOrNull() ?: return false }
        if (p.size != 3) return false
        return (p[0] == 10 && p[1] == 0 && p[2] in 2..3) || (p[0] == 192 && p[1] == 168 && p[2] == 240) ||
            (p[0] == 172 && p[1] in 16..31)
    }

    private fun prefixoDaRede(): String? {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val ip = runCatching {
            cm?.getLinkProperties(cm.activeNetwork)?.linkAddresses?.map { it.address }
                ?.filterIsInstance<Inet4Address>()?.firstOrNull { !it.isLoopbackAddress }?.hostAddress
        }.getOrNull() ?: runCatching {
            NetworkInterface.getNetworkInterfaces().toList().filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.toList() }.filterIsInstance<Inet4Address>()
                .firstOrNull { it.isSiteLocalAddress }?.hostAddress
        }.getOrNull() ?: return null
        return ip.split(".").takeIf { it.size == 4 }?.take(3)?.joinToString(".")
    }

    companion object {
        private val REDES_DE_CASA = listOf(
            "192.168.0", "192.168.1", "192.168.15", "192.168.100", "192.168.2", "192.168.3",
            "192.168.10", "192.168.50", "192.168.18", "192.168.31", "10.0.0", "10.0.1",
        )

        /**
         * Nome NetBIOS da máquina (consulta NBSTAT em UDP 137).
         * Resposta: cabeçalho 12 + nome 34 + tipo/classe/ttl/tamanho 10 = 56,
         * então o número de nomes está no byte 56 e cada nome ocupa 18 bytes.
         */
        fun nomeNetbios(ip: String, prazoMs: Int = 800): String? = runCatching {
            val q = ByteArray(50)
            q[0] = 0x13; q[1] = 0x37 // id
            q[5] = 1 // uma pergunta
            q[12] = 0x20
            val nome = ByteArray(16).also { it[0] = '*'.code.toByte() }
            for (i in 0 until 16) {
                val b = nome[i].toInt() and 0xFF
                q[13 + i * 2] = ('A'.code + (b shr 4)).toByte()
                q[14 + i * 2] = ('A'.code + (b and 0xF)).toByte()
            }
            q[46] = 0; q[47] = 0x21; q[48] = 0; q[49] = 1
            DatagramSocket().use { s ->
                s.soTimeout = prazoMs
                s.send(DatagramPacket(q, q.size, InetAddress.getByName(ip), 137))
                val buf = ByteArray(1024)
                val r = DatagramPacket(buf, buf.size)
                s.receive(r)
                if (r.length < 57) return null
                val n = buf[56].toInt() and 0xFF
                for (i in 0 until n) {
                    val o = 57 + i * 18
                    if (o + 18 > r.length) break
                    val sufixo = buf[o + 15].toInt() and 0xFF
                    val grupo = (buf[o + 16].toInt() and 0x80) != 0
                    if (sufixo == 0x20 || (sufixo == 0 && !grupo)) {
                        val t = String(buf, o, 15, Charsets.ISO_8859_1).trim()
                        if (t.isNotEmpty()) return t
                    }
                }
                null
            }
        }.getOrNull()
    }
}

/** A busca da tela inicial e da tela de rede é uma só, e sobrevive à troca de tela. */
object Descobertas {
    private val _achados = MutableStateFlow<List<SmbDiscovery.Encontrado>>(emptyList())
    val achados: StateFlow<List<SmbDiscovery.Encontrado>> = _achados
    private val _buscando = MutableStateFlow(false)
    val buscando: StateFlow<Boolean> = _buscando

    suspend fun buscar(context: Context) {
        if (_buscando.value) return
        _buscando.value = true
        try {
            val salvos = SmbServerStore.get(context).servers.value.map { it.host }
            SmbDiscovery(context, salvos).procurar { e ->
                synchronized(this) {
                    val atual = _achados.value
                    val i = atual.indexOfFirst { it.host == e.host }
                    // A mesma máquina vista por dois caminhos (ex.: o PC pela rede virtual do emulador).
                    if (i < 0 && e.nome != e.host && atual.any { it.nome.equals(e.nome, ignoreCase = true) }) return@synchronized
                    _achados.value = when {
                        i < 0 -> atual + e
                        // O nome do mDNS é melhor que IP puro.
                        atual[i].nome == atual[i].host && e.nome != e.host -> atual.toMutableList().also { it[i] = e }
                        else -> atual
                    }
                }
            }
        } finally {
            _buscando.value = false
        }
    }
}
