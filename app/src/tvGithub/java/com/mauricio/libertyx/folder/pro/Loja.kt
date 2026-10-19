package com.mauricio.libertyx.folder.pro

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * A TV do APK do GitHub não tem loja: a compra do Pro é a chave LibertyX
 * ([Pro.ativarChave]), paga pelo Telegram. Mesma forma da `Loja` das outras
 * variantes, para o código comum compilar igual; sempre "indisponível".
 */
object Loja {
    sealed interface Situacao {
        data object Conectando : Situacao
        data object Indisponivel : Situacao
        data class Pronta(val preco: String) : Situacao
    }

    val situacao: StateFlow<Situacao> = MutableStateFlow(Situacao.Indisponivel)
    val pendente: StateFlow<Boolean> = MutableStateFlow(false)

    fun iniciar(contexto: Context) {}
    fun atualizar() {}
    fun comprar(atividade: Activity): Boolean = false
}
