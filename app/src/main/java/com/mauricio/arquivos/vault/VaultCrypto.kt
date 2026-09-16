package com.mauricio.arquivos.vault

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Formato dos arquivos cifrados: AES-256-GCM em blocos de 1 MiB.
 *
 * Por que em blocos, e não o arquivo inteiro num GCM só: o GCM só confere a
 * autenticidade no fim, e o Android segura na memória tudo o que decifrou até
 * lá — um vídeo de 4 GB derrubaria o app. Em blocos, cada pedaço se confere
 * sozinho, a memória fica em 1 MiB e dá para ler do meio (pular num vídeo).
 *
 * ```
 * "AQV1" | prefixo aleatório (8) | bloco 0 | bloco 1 | … | último bloco
 * bloco i = GCM(chave do arquivo, nonce = prefixo‖i, AAD = [é o último?]) → n + 16 bytes
 * ```
 * O marcador de "último bloco" no AAD impede que alguém corte o fim do arquivo
 * sem ser percebido. A chave de cada arquivo é HMAC-SHA256(chave mestra, id).
 */
object VaultCrypto {
    const val CHUNK = 1 shl 20
    private const val TAG = 16
    private val MAGIC = byteArrayOf('A'.code.toByte(), 'Q'.code.toByte(), 'V'.code.toByte(), '1'.code.toByte())
    private const val HEADER = 12
    val random = SecureRandom()

    fun fileKey(master: SecretKey, id: String): SecretKey {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(master)
        return SecretKeySpec(mac.doFinal(id.toByteArray()), "AES")
    }

    private fun cipher(mode: Int, key: SecretKey, prefix: ByteArray, index: Int, last: Boolean): Cipher {
        val nonce = ByteBuffer.allocate(12).put(prefix).putInt(index).array()
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(mode, key, GCMParameterSpec(TAG * 8, nonce))
            updateAAD(byteArrayOf(if (last) 1 else 0))
        }
    }

    /** Tamanho do conteúdo original, deduzido do tamanho do arquivo cifrado. */
    fun plainSize(cifradoLen: Long): Long {
        val corpo = cifradoLen - HEADER
        if (corpo < TAG) throw IOException("Arquivo cifrado corrompido")
        val cheio = (CHUNK + TAG).toLong()
        val n = maxOf(1L, (corpo + cheio - 1) / cheio)
        return corpo - n * TAG
    }

    fun encryptingStream(out: OutputStream, key: SecretKey): OutputStream = object : OutputStream() {
        val prefix = ByteArray(8).also { random.nextBytes(it) }
        val buf = ByteArray(CHUNK)
        var len = 0
        var index = 0
        var fechado = false
        // Um bloco cheio só é escrito quando chega mais dado: até lá, não se sabe se é o último.
        var pendente = false

        init { out.write(MAGIC); out.write(prefix) }

        private fun despejar(last: Boolean) {
            out.write(cipher(Cipher.ENCRYPT_MODE, key, prefix, index++, last).doFinal(buf, 0, len))
            len = 0; pendente = false
        }

        override fun write(b: Int) = write(byteArrayOf(b.toByte()), 0, 1)
        override fun write(b: ByteArray, off: Int, n: Int) {
            var o = off; var r = n
            while (r > 0) {
                if (pendente) despejar(false)
                val k = minOf(r, CHUNK - len)
                System.arraycopy(b, o, buf, len, k)
                len += k; o += k; r -= k
                if (len == CHUNK) pendente = true
            }
        }
        override fun close() {
            if (fechado) return
            fechado = true
            despejar(true)
            out.close()
        }
    }

    /** Leitor com acesso aleatório — usado pelo provedor (outros apps pulam no arquivo) e pelas cópias. */
    class Reader(file: File, private val key: SecretKey) : AutoCloseable {
        private val raf = RandomAccessFile(file, "r")
        private val prefix = ByteArray(8)
        val size: Long
        private val chunks: Int
        private var cacheIndex = -1
        private val cache = ByteArray(CHUNK)
        private var cacheLen = 0
        private val cifrado = ByteArray(CHUNK + TAG)

        init {
            val magic = ByteArray(4)
            raf.readFully(magic)
            if (!magic.contentEquals(MAGIC)) throw IOException("Arquivo cifrado inválido")
            raf.readFully(prefix)
            size = plainSize(raf.length())
            chunks = maxOf(1, ((size + CHUNK - 1) / CHUNK).toInt())
        }

        private fun carregar(i: Int) {
            if (i == cacheIndex) return
            val inicio = HEADER + i.toLong() * (CHUNK + TAG)
            val n = if (i == chunks - 1) (raf.length() - inicio).toInt() else CHUNK + TAG
            raf.seek(inicio)
            raf.readFully(cifrado, 0, n)
            cacheLen = try {
                cipher(Cipher.DECRYPT_MODE, key, prefix, i, i == chunks - 1).doFinal(cifrado, 0, n, cache, 0)
            } catch (e: Exception) {
                throw IOException("Falha de integridade no arquivo cifrado", e)
            }
            cacheIndex = i
        }

        @Synchronized
        fun read(pos: Long, dest: ByteArray, off: Int, n: Int): Int {
            if (pos >= size) return -1
            var lidos = 0
            while (lidos < n && pos + lidos < size) {
                val p = pos + lidos
                val i = (p / CHUNK).toInt()
                carregar(i)
                val dentro = (p - i.toLong() * CHUNK).toInt()
                val k = minOf(n - lidos, cacheLen - dentro)
                if (k <= 0) break
                System.arraycopy(cache, dentro, dest, off + lidos, k)
                lidos += k
            }
            return lidos
        }

        fun stream(): InputStream = object : InputStream() {
            var pos = 0L
            override fun read(): Int { val b = ByteArray(1); return if (read(b, 0, 1) < 0) -1 else b[0].toInt() and 0xFF }
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val n = this@Reader.read(pos, b, off, len); if (n > 0) pos += n; return n
            }
            override fun close() = this@Reader.close()
        }

        override fun close() = raf.close()
    }
}
