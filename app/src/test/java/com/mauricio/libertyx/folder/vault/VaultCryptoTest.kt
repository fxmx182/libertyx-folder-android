package com.mauricio.libertyx.folder.vault

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import java.io.RandomAccessFile
import java.util.Random
import javax.crypto.spec.SecretKeySpec

class VaultCryptoTest {
    private val master = SecretKeySpec(ByteArray(32) { it.toByte() }, "AES")
    private val C = VaultCrypto.CHUNK

    private fun cifrar(dados: ByteArray, id: String = "x", pedacos: Int = 7777): File {
        val f = File.createTempFile("cofre", ".bin")
        VaultCrypto.encryptingStream(f.outputStream(), VaultCrypto.fileKey(master, id)).use { out ->
            var o = 0
            while (o < dados.size) { val n = minOf(pedacos, dados.size - o); out.write(dados, o, n); o += n }
        }
        return f
    }

    @Test fun idaEVoltaNosTamanhosLimite() {
        val r = Random(1)
        for (n in listOf(0, 1, C - 1, C, C + 1, 2 * C, 3 * C + 5)) {
            val dados = ByteArray(n).also(r::nextBytes)
            val f = cifrar(dados)
            VaultCrypto.Reader(f, VaultCrypto.fileKey(master, "x")).use { rd ->
                assertEquals("tamanho $n", n.toLong(), rd.size)
                assertArrayEquals("conteúdo $n", dados, rd.stream().readBytes())
            }
        }
    }

    @Test fun leituraAleatoria() {
        val dados = ByteArray(3 * C + 1234).also(Random(2)::nextBytes)
        val f = cifrar(dados, pedacos = C + 17)
        VaultCrypto.Reader(f, VaultCrypto.fileKey(master, "x")).use { rd ->
            val r = Random(3)
            repeat(200) {
                val pos = r.nextInt(dados.size)
                val len = r.nextInt(2 * C)
                val buf = ByteArray(len)
                val n = rd.read(pos.toLong(), buf, 0, len)
                val esperado = minOf(len, dados.size - pos)
                assertEquals(esperado, n)
                assertArrayEquals(dados.copyOfRange(pos, pos + n), buf.copyOf(n))
            }
        }
    }

    @Test fun adulteracaoECorteSaoDetectados() {
        val dados = ByteArray(2 * C + 10).also(Random(4)::nextBytes)
        // um byte trocado
        val f = cifrar(dados)
        RandomAccessFile(f, "rw").use { it.seek(C + 100L); val b = it.read(); it.seek(C + 100L); it.write(b xor 1) }
        try { VaultCrypto.Reader(f, VaultCrypto.fileKey(master, "x")).use { it.stream().readBytes() }; fail("alteração passou") } catch (_: Exception) {}
        // último bloco removido: o bloco que sobra não está marcado como último
        val g = cifrar(dados)
        RandomAccessFile(g, "rw").use { it.setLength(12L + 2 * (C + 16)) }
        try { VaultCrypto.Reader(g, VaultCrypto.fileKey(master, "x")).use { it.stream().readBytes() }; fail("corte passou") } catch (_: Exception) {}
        // chave errada
        val h = cifrar(dados)
        try { VaultCrypto.Reader(h, VaultCrypto.fileKey(master, "outro")).use { it.stream().readBytes() }; fail("chave errada passou") } catch (_: Exception) {}
    }
}
