package com.mauricio.libertyx.folder

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/**
 * Os textos do app para quem não tem tela: erros, notificação, títulos das
 * operações. Nas telas em Compose vale `stringResource`, que refaz a tela
 * quando o idioma muda; aqui é o contexto do aplicativo, que o sistema
 * atualiza junto.
 *
 * O idioma é o do aparelho, como no LibertyX Player: inglês é o padrão
 * (`values/`), para qualquer idioma sem tradução; português e espanhol vêm de
 * `values-pt/` e `values-es/`. Texto novo entra nos três.
 */
object Textos {
    fun get(@StringRes id: Int, vararg args: Any): String = App.instance.getString(id, *args)

    fun plural(@PluralsRes id: Int, n: Int, vararg args: Any): String =
        App.instance.resources.getQuantityString(id, n, *args)
}
