package com.mauricio.arquivos.fs

import java.io.File

/**
 * Onde um arquivo mora — no aparelho ou num servidor SMB.
 *
 * Tudo que as telas e as operações sabem de um arquivo passa por aqui, para
 * que copiar do servidor para o celular e do celular para o servidor seja o
 * mesmo código.
 */
sealed class Loc {
    abstract val name: String
    abstract fun parent(): Loc?
    abstract fun child(name: String): Loc

    /** Caminho absoluto; [root] é a raiz do volume (interno, cartão, USB), acima da qual não se sobe. */
    data class Local(val path: String, val root: String) : Loc() {
        val file get() = File(path)
        override val name: String get() = if (path == root) "" else file.name
        override fun parent(): Loc? = if (path == root) null else Local(file.parent ?: root, root)
        override fun child(name: String) = Local(if (path.endsWith("/")) "$path$name" else "$path/$name", root)
    }

    /** [share] vazio = a lista de compartilhamentos do servidor; [path] usa `/` e é relativo ao share. */
    data class Smb(val serverId: String, val share: String = "", val path: String = "") : Loc() {
        override val name: String get() = if (path.isEmpty()) share else path.substringAfterLast('/')
        override fun parent(): Loc? = when {
            share.isEmpty() -> null
            path.isEmpty() -> Smb(serverId)
            else -> Smb(serverId, share, path.substringBeforeLast('/', ""))
        }
        override fun child(name: String): Smb =
            if (share.isEmpty()) Smb(serverId, name)
            else Smb(serverId, share, if (path.isEmpty()) name else "$path/$name")
        val isServerRoot get() = share.isEmpty()
        val smbPath get() = path.replace('/', '\\')
    }

    /** Dentro da pasta segura; [path] vazio = raiz. */
    data class Vault(val path: String = "") : Loc() {
        override val name: String get() = path.substringAfterLast('/')
        override fun parent(): Loc? = if (path.isEmpty()) null else Vault(path.substringBeforeLast('/', ""))
        override fun child(name: String) = Vault(if (path.isEmpty()) name else "$path/$name")
    }
}


data class FileEntry(
    val loc: Loc,
    val name: String,
    val isDir: Boolean,
    val size: Long = 0,
    val modified: Long = 0,
    val hidden: Boolean = name.startsWith("."),
    /** Só para compartilhamentos SMB: não se pode apagar nem renomear. */
    val isShare: Boolean = false,
) {
    val ext: String get() = if (isDir) "" else name.substringAfterLast('.', "").lowercase()
}
