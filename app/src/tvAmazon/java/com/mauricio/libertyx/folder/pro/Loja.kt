package com.mauricio.libertyx.folder.pro

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import com.amazon.device.iap.PurchasingListener
import com.amazon.device.iap.PurchasingService
import com.amazon.device.iap.model.FulfillmentResult
import com.amazon.device.iap.model.ProductDataResponse
import com.amazon.device.iap.model.PurchaseResponse
import com.amazon.device.iap.model.PurchaseUpdatesResponse
import com.amazon.device.iap.model.Receipt
import com.amazon.device.iap.model.RequestId
import com.amazon.device.iap.model.UserDataResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A conversa com a Amazon Appstore sobre a compra do Pro — a variante do Fire
 * TV. Mesma forma da `Loja` da Google Play (`src/tvPlay`), para o resto do app
 * não saber de qual loja veio.
 *
 * Sem servidor próprio, como na Play: a compra fica na conta Amazon de quem
 * pagou, e o app pergunta a ela sempre que volta para a frente. O Pro vale em
 * todo Fire TV da mesma conta e volta sozinho num aparelho novo.
 *
 * Diferenças da Amazon que este arquivo existe para não errar:
 * - o produto é um **entitlement** (compra única); um reembolso não some da
 *   lista, chega como recibo **cancelado** — por isso só a lista completa
 *   ([PurchasingService.getPurchaseUpdates] com `reset = true`, paginada com
 *   `hasMore`) pode dizer que o Pro acabou;
 * - todo recibo válido recebe [PurchasingService.notifyFulfillment], senão a
 *   Appstore continua oferecendo-o como não entregue;
 * - a documentação pede a consulta das compras no `onResume`, e não ao abrir
 *   o processo — daí o observador de atividades em [iniciar];
 * - sem Appstore (APK instalado à mão numa Google TV, emulador) nada responde
 *   com sucesso; aí vale o que foi guardado da última resposta
 *   ([Pro.registrarCompra]), como na Play.
 */
object Loja {

    /** O SKU cadastrado no console da Amazon: entitlement, compra única. */
    const val PRODUTO = "libertyx_folder_pro"

    sealed interface Situacao {
        data object Conectando : Situacao
        /** Sem Appstore neste aparelho, ou fora da loja. */
        data object Indisponivel : Situacao
        data class Pronta(val preco: String) : Situacao
    }

    private val _situacao = MutableStateFlow<Situacao>(Situacao.Conectando)
    val situacao: StateFlow<Situacao> = _situacao.asStateFlow()

    /** Compra que aguarda aprovação (controle dos pais da conta Amazon). */
    private val _pendente = MutableStateFlow(false)
    val pendente: StateFlow<Boolean> = _pendente.asStateFlow()

    private var iniciada = false
    private var preco: String? = null

    /** A consulta da lista completa em andamento; respostas de outras são ignoradas. */
    private var consulta: String? = null
    private val recibos = mutableListOf<Receipt>()

    fun iniciar(contexto: Context) {
        val app = contexto.applicationContext as Application
        // Antes de qualquer outra chamada, na thread principal (pede a documentação).
        PurchasingService.registerListener(app, ouvinte)
        iniciada = true
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityResumed(activity: Activity) = atualizar()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityStarted(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivityStopped(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    /** Refaz a pergunta — ao voltar para o app, ao abrir a tela do Pro e no "restaurar compra". */
    fun atualizar() {
        if (!iniciada) return
        if (preco == null) {
            _situacao.value = Situacao.Conectando
            PurchasingService.getProductData(setOf(PRODUTO))
        }
        recibos.clear()
        consulta = PurchasingService.getPurchaseUpdates(true).id()
    }

    /** @return false quando não há como comprar aqui (a tela avisa). */
    fun comprar(atividade: Activity): Boolean {
        if (!iniciada || preco == null) return false
        // A Appstore abre a própria tela de compra por cima do app.
        PurchasingService.purchase(PRODUTO)
        return true
    }

    private fun RequestId.id() = toString()

    private val ouvinte = object : PurchasingListener {
        override fun onUserDataResponse(resposta: UserDataResponse) {}

        override fun onProductDataResponse(resposta: ProductDataResponse) {
            val produto = if (resposta.requestStatus == ProductDataResponse.RequestStatus.SUCCESSFUL)
                resposta.productData[PRODUTO] else null
            preco = produto?.price
            _situacao.value = preco?.let { Situacao.Pronta(it) } ?: Situacao.Indisponivel
        }

        override fun onPurchaseUpdatesResponse(resposta: PurchaseUpdatesResponse) {
            if (resposta.requestId.id() != consulta) return
            // Só a resposta de sucesso conta. Um erro, ou a falta da Appstore,
            // não pode tirar o Pro de quem pagou.
            if (resposta.requestStatus != PurchaseUpdatesResponse.RequestStatus.SUCCESSFUL) return
            recibos += resposta.receipts
            if (resposta.hasMore()) {
                consulta = PurchasingService.getPurchaseUpdates(false).id()
                return
            }
            processar(recibos.toList(), completa = true)
        }

        override fun onPurchaseResponse(resposta: PurchaseResponse) {
            when (resposta.requestStatus) {
                PurchaseResponse.RequestStatus.SUCCESSFUL ->
                    resposta.receipt?.let { processar(listOf(it), completa = false) }
                // Comprou em outro aparelho, ou a lista ainda não tinha chegado.
                PurchaseResponse.RequestStatus.ALREADY_PURCHASED -> atualizar()
                PurchaseResponse.RequestStatus.PENDING -> _pendente.value = true
                else -> {}
            }
        }
    }

    /**
     * @param completa a lista é tudo o que a conta tem, e não só a compra que
     *   acabou de acontecer — só então a falta de um recibo válido quer dizer
     *   "não comprou" (ou foi reembolsado).
     */
    private fun processar(lista: List<Receipt>, completa: Boolean) {
        val valido = lista.firstOrNull { it.sku == PRODUTO && !it.isCanceled }
        if (valido != null) {
            _pendente.value = false
            Pro.registrarCompra(true)
            PurchasingService.notifyFulfillment(valido.receiptId, FulfillmentResult.FULFILLED)
        } else if (completa) {
            Pro.registrarCompra(false)
        }
    }
}
