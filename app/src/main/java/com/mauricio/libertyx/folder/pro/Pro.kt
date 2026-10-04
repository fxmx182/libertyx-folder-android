package com.mauricio.libertyx.folder.pro

import android.content.Context
import com.mauricio.libertyx.folder.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * O LibertyX Folder Pro: o que está liberado, e por quê.
 *
 * O mesmo modelo do LibertyX Player. O app é grátis e completo como
 * gerenciador dos arquivos do aparelho; o Pro é uma compra única que libera a
 * **rede** — servidores SMB, a busca automática deles e tudo o que se faz com
 * eles. Nos primeiros sete dias a rede vem liberada de graça, para quem
 * instalou experimentar antes de pagar.
 *
 * **O teste é do app, não da Play.** A Play só oferece período grátis em
 * assinatura; para compra única não existe. Então o app anota o dia da
 * primeira abertura e conta a partir dele. Na TV a anotação vai no backup do
 * Android (só ela: ver `xml/backup_tv`), para reinstalar não recomeçar a
 * contagem — não é à prova de quem insiste, e não precisa ser.
 *
 * **Terminado o teste, nada some.** Trava só a rede; os arquivos do aparelho
 * continuam inteiros. E a busca na rede continua achando os computadores — o
 * que se trava é abrir, não ver que eles estão lá.
 *
 * Só existe na variante com [BuildConfig.PRO] (a TV); no celular, [ativo] é
 * sempre verdadeiro e nada disto aparece.
 */
object Pro {

    sealed interface Estado {
        /** Comprado, ou o APK do dono ([BuildConfig.LIBERADO]). */
        data object Comprado : Estado
        data class Teste(val diasRestantes: Int) : Estado
        data object Expirado : Estado
    }

    val DIAS_DE_TESTE = BuildConfig.DIAS_DE_TESTE
    private const val DIA = 24L * 60 * 60 * 1000

    /** Arquivo próprio: é o único que vai para o backup do Android. */
    private const val ARQUIVO = "pro"
    private const val KEY_INICIO = "inicioDoTeste"
    private const val KEY_COMPRADO = "comprado"

    private lateinit var app: Context
    private val _estado = MutableStateFlow<Estado>(Estado.Comprado)
    val estado: StateFlow<Estado> = _estado.asStateFlow()

    private fun prefs() = app.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE)

    fun iniciar(contexto: Context) {
        app = contexto.applicationContext
        if (!BuildConfig.PRO) return
        // A primeira abertura depois de instalar — ou de atualizar da versão
        // que ainda não tinha o Pro: quem já usava ganha a semana também.
        if (!prefs().contains(KEY_INICIO)) prefs().edit().putLong(KEY_INICIO, System.currentTimeMillis()).apply()
        recalcular()
    }

    /** A rede liberada agora, seja por compra, seja pelo teste. */
    val ativo: Boolean get() = estado.value !is Estado.Expirado

    /**
     * O que a Play respondeu sobre a compra. Guardado, e não só consultado:
     * sem rede, ou numa TV sem Play (Fire TV), a Play não responde, e quem
     * pagou não pode perder o Pro por isso. Um reembolso chega do mesmo jeito,
     * na próxima resposta, com a lista vazia.
     */
    fun registrarCompra(comprado: Boolean) {
        prefs().edit().putBoolean(KEY_COMPRADO, comprado).apply()
        recalcular()
    }

    fun recalcular() {
        if (!BuildConfig.PRO) { _estado.value = Estado.Comprado; return }
        _estado.value = when {
            BuildConfig.LIBERADO || prefs().getBoolean(KEY_COMPRADO, false) -> Estado.Comprado
            else -> {
                val inicio = prefs().getLong(KEY_INICIO, System.currentTimeMillis())
                // Relógio atrasado não vira dia negativo nem teste maior.
                val passados = ((System.currentTimeMillis() - inicio).coerceAtLeast(0) / DIA).toInt()
                val restam = DIAS_DE_TESTE - passados
                if (restam > 0) Estado.Teste(restam) else Estado.Expirado
            }
        }
    }
}
