package com.mauricio.libertyx.folder.smb

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Servidor salvo. A senha mora no arquivo cifrado, pelo [id]. */
data class SmbServer(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val host: String,
    val port: Int = 445,
    val username: String = "",
    val domain: String = "",
    val isGuest: Boolean = false,
) {
    val displayHost: String get() = if (port == 445) host else "$host:$port"
}

class SmbServerStore private constructor(context: Context) {

    private val prefs = context.getSharedPreferences("arquivos", Context.MODE_PRIVATE)
    private val secret: SharedPreferences = abrirCofre(context)

    private val _servers = MutableStateFlow<List<SmbServer>>(emptyList())
    val servers: StateFlow<List<SmbServer>> = _servers

    init {
        _servers.value = runCatching {
            val raiz = JSONArray(prefs.getString(KEY, "[]"))
            (0 until raiz.length()).map { i ->
                val o = raiz.getJSONObject(i)
                SmbServer(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    host = o.getString("host"),
                    port = o.optInt("port", 445),
                    username = o.optString("username"),
                    domain = o.optString("domain"),
                    isGuest = o.optBoolean("guest"),
                )
            }
        }.getOrDefault(emptyList())
    }

    fun save(server: SmbServer, password: String?) {
        val lista = _servers.value.toMutableList()
        val i = lista.indexOfFirst { it.id == server.id }
        if (i >= 0) lista[i] = server else lista += server
        if (password != null) secret.edit().putString(server.id, password).apply()
        _servers.value = lista
        persist()
        SmbFs.forget(server.id)
    }

    fun remove(server: SmbServer) {
        _servers.value = _servers.value.filterNot { it.id == server.id }
        secret.edit().remove(server.id).apply()
        persist()
        SmbFs.forget(server.id)
    }

    fun password(server: SmbServer): String =
        if (server.isGuest) "" else secret.getString(server.id, null) ?: ""

    fun byId(id: String): SmbServer? = _servers.value.firstOrNull { it.id == id }

    private fun persist() {
        val raiz = JSONArray()
        for (s in _servers.value) {
            raiz.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("host", s.host); put("port", s.port)
                put("username", s.username); put("domain", s.domain); put("guest", s.isGuest)
            })
        }
        prefs.edit().putString(KEY, raiz.toString()).apply()
    }

    /** Cofre cifrado, com queda para preferência comum onde o chaveiro do sistema falha (emulador, ROM). */
    private fun abrirCofre(context: Context): SharedPreferences = runCatching {
        val chave = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
        EncryptedSharedPreferences.create(
            context, "arquivos.secrets", chave,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }.getOrElse { context.getSharedPreferences("arquivos.secrets.plain", Context.MODE_PRIVATE) }

    companion object {
        private const val KEY = "servers"
        @Volatile private var instance: SmbServerStore? = null
        fun get(context: Context): SmbServerStore = instance ?: synchronized(this) {
            instance ?: SmbServerStore(context.applicationContext).also { instance = it }
        }
    }
}
