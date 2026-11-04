package br.gabriel.cochilo.sync

import kotlinx.coroutines.flow.Flow

/**
 * How two phones reach each other on the family's Wi-Fi (docs/sync.md). The github build finds
 * phones with Android's network service discovery and talks over a socket; the play build, which
 * has no INTERNET permission and so can't open one, has no link ([available] is false) and shows
 * no sync at all.
 */
interface SyncLink {
    val available: Boolean

    /**
     * Offers this phone on the Wi-Fi for [mode] (for SYNC, only to the pair [pairId]), and hands
     * each incoming connection to [onConnection] on a worker thread. Close it to stop.
     */
    fun offer(mode: Mode, name: String, pairId: String?, onConnection: (Wire) -> Unit): java.io.Closeable

    /** The other phones on the Wi-Fi offering [mode] (for SYNC, only [pairId]), as they come and go. */
    fun browse(mode: Mode, pairId: String?): Flow<List<Peer>>

    /** Opens a connection to [peer]. Blocking; call it off the main thread. */
    fun connect(peer: Peer): Wire

    enum class Mode { PAIR, SYNC }
}

/** A phone found on the Wi-Fi: its name, where to reach it, and its per-run token. */
data class Peer(val name: String, val host: String, val port: Int, val token: String)
