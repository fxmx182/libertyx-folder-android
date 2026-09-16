package com.mauricio.libertyx.folder.vault

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.mauricio.libertyx.folder.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A pasta segura.
 *
 * **Chaves, de fora para dentro:**
 * 1. PIN → PBKDF2-HMAC-SHA256 (150 mil voltas, sal aleatório) → chave do PIN.
 * 2. A chave do PIN cifra (AES-GCM) a **chave mestra**, sorteada uma vez.
 * 3. Esse embrulho é cifrado de novo por uma chave do **Android Keystore**, que
 *    não sai do chip de segurança. Sem o aparelho, copiar a pasta e testar PINs
 *    num computador não serve para nada — e no aparelho as tentativas têm espera.
 *
 * Trocar o PIN só refaz o embrulho; os arquivos não são recifrados.
 * Os arquivos ficam na área privada do app, com nomes aleatórios; os nomes
 * reais, pastas e origem ficam num índice também cifrado.
 */
object Vault {
    private val dir: File get() = File(App.instance.filesDir, "cofre").apply { mkdirs() }
    private val metaFile get() = File(dir, "cofre.json")
    private val indexFile get() = File(dir, "indice.bin")
    fun blob(id: String) = File(dir, "$id.bin")

    private const val ITER = 150_000
    private const val ALIAS = "arquivos_cofre"

    @Volatile private var master: SecretKey? = null
    private val _unlocked = MutableStateFlow(false)
    val unlocked: StateFlow<Boolean> = _unlocked
    val isSetUp: Boolean get() = metaFile.exists()

    // ------------------------------------------------------------ índice

    data class Item(val path: String, val id: String?, val size: Long, val modified: Long, val origem: String?) {
        val isDir get() = id == null
    }

    private val itens = LinkedHashMap<String, Item>()

    val versao = MutableStateFlow(0)

    fun key(): SecretKey = master ?: throw IOException("A pasta segura está bloqueada")

    @Synchronized fun snapshot(): List<Item> = itens.values.toList()
    @Synchronized fun get(path: String): Item? = itens[path]

    @Synchronized fun put(item: Item) {
        // Garante as pastas-mãe.
        var p = item.path.substringBeforeLast('/', "")
        while (p.isNotEmpty() && itens[p] == null) {
            itens[p] = Item(p, null, 0, System.currentTimeMillis(), null)
            p = p.substringBeforeLast('/', "")
        }
        itens[item.path] = item
        salvarIndice()
    }

    @Synchronized fun remove(path: String) {
        val alvo = itens.keys.filter { it == path || it.startsWith("$path/") }
        for (k in alvo) itens.remove(k)?.id?.let { blob(it).delete() }
        salvarIndice()
    }

    @Synchronized fun move(de: String, para: String) {
        if (itens.containsKey(para)) throw IOException("Já existe um item com esse nome")
        val alvo = itens.keys.filter { it == de || it.startsWith("$de/") }
        if (alvo.isEmpty()) throw IOException("Item não encontrado")
        val movidos = alvo.map { k -> itens.remove(k)!!.let { it.copy(path = para + k.removePrefix(de)) } }
        movidos.forEach { put(it) }
        salvarIndice()
    }

    private fun salvarIndice() {
        val arr = JSONArray()
        itens.values.forEach { i ->
            arr.put(JSONObject().apply {
                put("p", i.path); put("id", i.id ?: JSONObject.NULL); put("s", i.size); put("m", i.modified)
                put("o", i.origem ?: JSONObject.NULL)
            })
        }
        val tmp = File(dir, "indice.tmp")
        VaultCrypto.encryptingStream(tmp.outputStream(), VaultCrypto.fileKey(key(), "indice")).use {
            it.write(arr.toString().toByteArray())
        }
        if (!tmp.renameTo(indexFile)) throw IOException("Falha ao gravar o índice")
        versao.value++
    }

    private fun carregarIndice() {
        itens.clear()
        if (!indexFile.exists()) return
        val texto = VaultCrypto.Reader(indexFile, VaultCrypto.fileKey(key(), "indice")).stream().use { it.readBytes() }
        val arr = JSONArray(String(texto))
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val it = Item(o.getString("p"), if (o.isNull("id")) null else o.getString("id"), o.getLong("s"), o.getLong("m"),
                if (o.isNull("o")) null else o.getString("o"))
            itens[it.path] = it
        }
        // Blobs órfãos (cópia interrompida) somem.
        val vivos = itens.values.mapNotNull { it.id }.toSet()
        dir.listFiles()?.filter { it.name.endsWith(".bin") && it.name != indexFile.name && it.nameWithoutExtension !in vivos }
            ?.forEach { it.delete() }
    }

    // ------------------------------------------------------------ PIN

    private fun derivar(pin: String, sal: ByteArray): SecretKey {
        val spec = PBEKeySpec(pin.toCharArray(), sal, ITER, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(bytes, "AES")
    }

    private fun chaveDoAparelho(): SecretKey? = runCatching {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build())
            generateKey()
        }
    }.getOrNull()

    private fun gcm(mode: Int, key: SecretKey, data: ByteArray, iv: ByteArray?): Pair<ByteArray, ByteArray> {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        if (iv == null) c.init(mode, key) else c.init(mode, key, GCMParameterSpec(128, iv))
        return c.doFinal(data) to c.iv
    }

    private fun b64(b: ByteArray) = Base64.encodeToString(b, Base64.NO_WRAP)
    private fun unb64(s: String) = Base64.decode(s, Base64.NO_WRAP)

    private fun gravarMeta(pin: String, mestra: SecretKey, tipo: Tipo) {
        val sal = ByteArray(16).also { VaultCrypto.random.nextBytes(it) }
        val (embrulho, iv) = gcm(Cipher.ENCRYPT_MODE, derivar(pin, sal), mestra.encoded, null)
        val meta = JSONObject().put("v", 1).put("sal", b64(sal)).put("iter", ITER).put("iv", b64(iv)).put("tipo", tipo.name)
        val dev = chaveDoAparelho()
        if (dev != null) {
            val (externo, iv2) = gcm(Cipher.ENCRYPT_MODE, dev, embrulho, null)
            meta.put("dados", b64(externo)).put("iv2", b64(iv2))
        } else {
            meta.put("dados", b64(embrulho))
        }
        val tmp = File(dir, "cofre.tmp"); tmp.writeText(meta.toString())
        if (!tmp.renameTo(metaFile)) throw IOException("Falha ao gravar a pasta segura")
    }

    /** PIN só com números (teclado numérico) ou senha com letras (teclado completo). */
    enum class Tipo { Pin, Senha }

    val tipo: Tipo get() = runCatching { Tipo.valueOf(JSONObject(metaFile.readText()).optString("tipo", "Pin")) }.getOrDefault(Tipo.Pin)

    @Synchronized fun setup(pin: String, tipo: Tipo) {
        check(!isSetUp)
        val mestra = SecretKeySpec(ByteArray(32).also { VaultCrypto.random.nextBytes(it) }, "AES")
        gravarMeta(pin, mestra, tipo)
        master = mestra
        itens.clear(); salvarIndice()
        _unlocked.value = true
    }

    // Espera crescente depois de 5 erros: 30 s, 60 s, 120 s…
    private val prefs get() = App.instance.getSharedPreferences("cofre", Context.MODE_PRIVATE)
    fun esperaRestanteMs(): Long = maxOf(0, prefs.getLong("ate", 0) - System.currentTimeMillis())

    sealed interface Resultado {
        data object Ok : Resultado
        data class Errado(val restantes: Int?) : Resultado
        data class Espere(val ms: Long) : Resultado
        /** Limite de tentativas atingido com "apagar após 10 erros" ligado. */
        data object Apagada : Resultado
    }

    // ------------------------------------------------------------ configurações

    /** Segundos fora do app até bloquear; 0 = ao sair do app. */
    var autoBloqueio: Int
        get() = prefs.getInt("auto", 60)
        set(v) { prefs.edit().putInt("auto", v).apply(); versao.value++ }

    /** Proteção contra roubo: 10 PINs errados seguidos apagam tudo. */
    var apagarApos10: Boolean
        get() = prefs.getBoolean("apagar10", false)
        set(v) { prefs.edit().putBoolean("apagar10", v).apply(); versao.value++ }

    /** Oculta o cartão do início e a opção "Mover para pasta segura" enquanto bloqueada. */
    var visivel: Boolean
        get() = prefs.getBoolean("visivel", true)
        set(v) { prefs.edit().putBoolean("visivel", v).apply(); versao.value++ }

    const val LIMITE_APAGAR = 10

    /** Confere o PIN contando as tentativas — usado para abrir e para confirmar ações perigosas. */
    @Synchronized private fun verificar(pin: String): Pair<Resultado, SecretKey?> {
        esperaRestanteMs().takeIf { it > 0 }?.let { return Resultado.Espere(it) to null }
        val mestra = abrir(pin)
        if (mestra == null) {
            val erros = prefs.getInt("erros", 0) + 1
            if (apagarApos10 && erros >= LIMITE_APAGAR) { erase(); return Resultado.Apagada to null }
            val ed = prefs.edit().putInt("erros", erros)
            if (erros >= 5) ed.putLong("ate", System.currentTimeMillis() + 30_000L * (1L shl minOf(erros - 5, 8)))
            ed.apply()
            return Resultado.Errado(if (apagarApos10) LIMITE_APAGAR - erros else null) to null
        }
        prefs.edit().putInt("erros", 0).putLong("ate", 0).apply()
        return Resultado.Ok to mestra
    }

    @Synchronized fun unlock(pin: String): Resultado {
        val (r, mestra) = verificar(pin)
        if (mestra == null) return r
        master = mestra
        carregarIndice()
        _unlocked.value = true
        versao.value++
        return Resultado.Ok
    }

    /** Reconfirma o PIN antes de ação destrutiva, mesmo com a pasta aberta. */
    fun confirmar(pin: String): Resultado = verificar(pin).first

    private fun abrir(pin: String): SecretKey? = runCatching {
        val meta = JSONObject(metaFile.readText())
        var embrulho = unb64(meta.getString("dados"))
        if (meta.has("iv2")) embrulho = gcm(Cipher.DECRYPT_MODE, chaveDoAparelho()!!, embrulho, unb64(meta.getString("iv2"))).first
        val sal = unb64(meta.getString("sal"))
        val bytes = gcm(Cipher.DECRYPT_MODE, derivar(pin, sal), embrulho, unb64(meta.getString("iv"))).first
        SecretKeySpec(bytes, "AES")
    }.getOrNull()

    @Synchronized fun changePin(atual: String, novo: String, tipo: Tipo): Resultado {
        val (r, mestra) = verificar(atual)
        if (mestra == null) return r
        gravarMeta(novo, mestra, tipo)
        return Resultado.Ok
    }

    @Synchronized fun lock() {
        master = null
        itens.clear()
        _unlocked.value = false
    }

    /** "Esqueci o PIN": sem o PIN não há como abrir — a única saída é apagar tudo. */
    @Synchronized fun erase() {
        lock()
        dir.deleteRecursively()
        runCatching { KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(ALIAS) }
        // Tudo volta ao padrão: uma pasta nova não herda nada da apagada.
        prefs.edit().clear().apply()
        versao.value++
    }
}
