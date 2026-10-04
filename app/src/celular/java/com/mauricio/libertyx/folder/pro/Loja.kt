package com.mauricio.libertyx.folder.pro

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * O celular ainda não vai para a loja: sem Pro, sem compra e sem a biblioteca
 * de cobrança no APK. Mesma forma da `Loja` da TV, para o código comum
 * compilar igual nas duas variantes.
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
