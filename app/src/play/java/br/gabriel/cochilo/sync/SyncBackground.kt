package br.gabriel.cochilo.sync

import android.content.Context

/** The play build has no partner sync, so nothing to keep running (docs/sync.md). */
object SyncBackground {
    val running = false
    fun update(@Suppress("UNUSED_PARAMETER") context: Context) {}
}
