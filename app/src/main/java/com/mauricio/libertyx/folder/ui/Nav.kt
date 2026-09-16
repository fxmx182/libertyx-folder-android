package com.mauricio.libertyx.folder.ui

import android.content.Context
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mauricio.libertyx.folder.App
import com.mauricio.libertyx.folder.fs.FileEntry
import com.mauricio.libertyx.folder.fs.Loc
import com.mauricio.libertyx.folder.media.Category

/** Cada tela da pilha guarda a própria rolagem, para o "voltar" cair onde estava. */
sealed class Screen {
    val listState = LazyListState()
    val gridState = LazyGridState()

    class Home : Screen()
    class Folder(val loc: Loc) : Screen()
    class Cat(val cat: Category) : Screen()
    /** [dentro] nulo = busca no aparelho inteiro (índice do sistema); senão, varre a pasta e subpastas. */
    class Search(val termo: String, val dentro: Loc?) : Screen()
    class Network : Screen()
    class Pin(val modo: PinModo, val depois: (MainVm) -> Unit) : Screen()
}

/** Itens escolhidos em "Mover"/"Copiar", esperando a pasta de destino. */
data class Clip(val itens: List<FileEntry>, val mover: Boolean)

enum class Sort(val titulo: String) { Name("Nome"), Date("Data"), Size("Tamanho"), Type("Tipo") }

class MainVm : ViewModel() {
    val stack = mutableStateListOf<Screen>(Screen.Home())
    var clip by mutableStateOf<Clip?>(null)

    val top get() = stack.last()
    fun push(s: Screen) { stack.add(s) }
    fun pop(): Boolean = if (stack.size > 1) { stack.removeAt(stack.lastIndex); true } else false
    /** Tira da pilha tudo que mostra conteúdo da pasta segura (ao bloquear). */
    fun esquecerCofre() {
        val fora = stack.filter { s ->
            s is Screen.Pin || (s as? Screen.Folder)?.loc is Loc.Vault || (s as? Screen.Search)?.dentro is Loc.Vault
        }
        if (fora.isNotEmpty()) { stack.removeAll(fora); if (stack.isEmpty()) stack.add(Screen.Home()) }
        if (clip?.itens?.any { it.loc is Loc.Vault } == true) clip = null
    }

    fun home() { while (stack.size > 1) stack.removeAt(stack.lastIndex) }
    /** Troca a pasta atual por outra do mesmo lugar (breadcrumb), sem empilhar. */
    fun jump(loc: Loc) {
        while (stack.size > 1 && (stack.last() as? Screen.Folder)?.loc?.let { isAncestorOrSelf(loc, it) } == true &&
            (stack.last() as Screen.Folder).loc != loc) stack.removeAt(stack.lastIndex)
        if ((stack.last() as? Screen.Folder)?.loc != loc) push(Screen.Folder(loc))
    }

    private fun isAncestorOrSelf(a: Loc, b: Loc): Boolean {
        var p: Loc? = b
        while (p != null) { if (p == a) return true; p = p.parent() }
        return false
    }
}

object Prefs {
    private val sp = App.instance.getSharedPreferences("ui", Context.MODE_PRIVATE)
    var sort by pref(Sort.valueOf(sp.getString("sort", Sort.Name.name)!!)) { sp.edit().putString("sort", it.name).apply() }
    var asc by pref(sp.getBoolean("asc", true)) { sp.edit().putBoolean("asc", it).apply() }
    var grid by pref(sp.getBoolean("grid", false)) { sp.edit().putBoolean("grid", it).apply() }
    var hidden by pref(sp.getBoolean("hidden", false)) { sp.edit().putBoolean("hidden", it).apply() }

    private fun <T> pref(inicial: T, salvar: (T) -> Unit) = object : kotlin.properties.ReadWriteProperty<Any?, T> {
        val st = mutableStateOf(inicial)
        override fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>) = st.value
        override fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: T) { st.value = value; salvar(value) }
    }

    fun ordenar(lista: List<FileEntry>): List<FileEntry> {
        val nat = Comparator<FileEntry> { a, b -> natural(a.name, b.name) }
        val base = when (sort) {
            Sort.Name -> nat
            Sort.Date -> compareBy<FileEntry> { it.modified }.then(nat)
            Sort.Size -> compareBy<FileEntry> { it.size }.then(nat)
            Sort.Type -> compareBy<FileEntry> { it.ext }.then(nat)
        }
        val dir = if (asc) base else base.reversed()
        return lista.filter { hidden || !it.hidden }.sortedWith(compareByDescending<FileEntry> { it.isDir }.then(dir))
    }

    /** "Episódio 2" antes de "Episódio 10". */
    fun natural(a: String, b: String): Int {
        var i = 0; var j = 0
        while (i < a.length && j < b.length) {
            val ca = a[i]; val cb = b[j]
            if (ca.isDigit() && cb.isDigit()) {
                val si = i; while (i < a.length && a[i].isDigit()) i++
                val sj = j; while (j < b.length && b[j].isDigit()) j++
                val na = a.substring(si, i).trimStart('0'); val nb = b.substring(sj, j).trimStart('0')
                if (na.length != nb.length) return na.length - nb.length
                val c = na.compareTo(nb); if (c != 0) return c
            } else {
                val c = ca.lowercaseChar().compareTo(cb.lowercaseChar()); if (c != 0) return c
                i++; j++
            }
        }
        return (a.length - i) - (b.length - j)
    }
}
