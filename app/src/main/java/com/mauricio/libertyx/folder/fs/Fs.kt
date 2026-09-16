package com.mauricio.libertyx.folder.fs

import com.mauricio.libertyx.folder.smb.SmbFs
import java.io.InputStream
import java.io.OutputStream

interface Fs {
    suspend fun list(dir: Loc): List<FileEntry>
    suspend fun stat(loc: Loc): FileEntry?
    suspend fun mkdir(parent: Loc, name: String): Loc
    /** Apaga, recursivamente se for pasta. */
    suspend fun delete(loc: Loc)
    suspend fun rename(loc: Loc, newName: String): Loc
    /**
     * Move sem copiar os bytes, quando origem e destino estão no mesmo lugar
     * (mesmo volume, ou mesmo share do mesmo servidor). `false` = não dá, copie.
     */
    suspend fun fastMove(src: Loc, dest: Loc): Boolean
    fun openRead(loc: Loc): InputStream
    fun openWrite(loc: Loc): OutputStream
}

object Fss {
    fun of(loc: Loc): Fs = when (loc) {
        is Loc.Local -> LocalFs
        is Loc.Smb -> SmbFs
        is Loc.Vault -> com.mauricio.libertyx.folder.vault.VaultFs
    }
}
