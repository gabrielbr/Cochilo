package br.gabriel.cochilo.sync

import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Deletion
import br.gabriel.cochilo.data.Event
import kotlinx.serialization.Serializable

/** What one phone sends the other at a sync (docs/sync.md): the whole log, and what was deleted. */
@Serializable
data class SyncData(
    val version: Int = VERSION,
    val baby: Baby? = null,
    val events: List<Event>,
    val deletions: List<Deletion> = emptyList(),
) {
    companion object {
        /** Bumped with any change to what is sent; a partner on a newer version is asked to update. */
        const val VERSION = 1
    }
}

/**
 * What a sync changes on this phone: entries to write, ids to delete, deletions to remember, the
 * baby if this phone had none, and plain counts for the message ("3 new from Ana").
 */
data class SyncChanges(
    val upsert: List<Event>,
    val delete: List<String>,
    val deletions: List<Deletion>,
    val baby: Baby?,
    val added: Int,
    val updated: Int,
    val removed: Int,
) {
    val none get() = upsert.isEmpty() && delete.isEmpty() && baby == null
}

/**
 * The merge (pure Kotlin, tested in SyncMergeTest). Both phones run it on the same two sets and end
 * with the same log:
 * - an entry's newest edit wins (its `updatedAt`), like an import;
 * - a deletion wins over an entry it's newer than (or as new as); an entry edited after its
 *   deletion (Undo, or an edit on the other phone) stays;
 * - each id keeps its latest deletion.
 * The baby: this phone's own profile stays; a phone with none takes the partner's.
 */
object SyncMerge {
    fun changes(local: SyncData, remote: SyncData): SyncChanges {
        val deletions = (local.deletions + remote.deletions).groupBy { it.id }.mapValues { (_, d) -> d.maxBy { it.deletedAt } }
        val mine = local.events.associateBy { it.id }
        val upsert = mutableListOf<Event>()
        var added = 0
        var updated = 0
        for (e in remote.events) {
            val gone = deletions[e.id]
            if (gone != null && gone.deletedAt >= e.updatedAt) continue
            val old = mine[e.id]
            when {
                old == null -> { upsert += e; added++ }
                e.updatedAt > old.updatedAt -> { upsert += e; updated++ }
            }
        }
        val delete = local.events.filter { e -> deletions[e.id]?.let { it.deletedAt >= e.updatedAt } == true && upsert.none { it.id == e.id } }.map { it.id }
        val newDeletions = deletions.values.filter { d -> local.deletions.none { it.id == d.id && it.deletedAt >= d.deletedAt } }
        return SyncChanges(
            upsert, delete, newDeletions,
            baby = if (local.baby == null) remote.baby else null,
            added = added, updated = updated, removed = delete.size,
        )
    }
}
