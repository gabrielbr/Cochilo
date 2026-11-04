package br.gabriel.cochilo.sync

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The play build has no INTERNET permission, so no socket: no partner sync (docs/sync.md). */
object SyncLinks {
    fun create(@Suppress("UNUSED_PARAMETER") context: Context): SyncLink = object : SyncLink {
        override val available = false
        override fun offer(mode: SyncLink.Mode, name: String, pairId: String?, onConnection: (Wire) -> Unit) = java.io.Closeable {}
        override fun browse(mode: SyncLink.Mode, pairId: String?): Flow<List<Peer>> = flowOf(emptyList())
        override fun connect(peer: Peer): Wire = throw java.io.IOException("no network in this build")
    }
}
