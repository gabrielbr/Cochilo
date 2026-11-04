package br.gabriel.cochilo.sync

import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Deletion
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {
    private fun e(id: String, updated: Long, start: Long = 1000L) = Event(id = id, type = EventType.FEED, start = start, updatedAt = updated)

    /** Applies [c] to [d], as the app does. */
    private fun apply(d: SyncData, c: SyncChanges): SyncData {
        val byId = d.events.associateBy { it.id }.toMutableMap()
        c.upsert.forEach { byId[it.id] = it }
        c.delete.forEach { byId.remove(it) }
        val dels = (d.deletions + c.deletions).groupBy { it.id }.map { (_, v) -> v.maxBy { it.deletedAt } }
        return d.copy(events = byId.values.sortedBy { it.id }, deletions = dels.sortedBy { it.id }, baby = d.baby ?: c.baby)
    }

    @Test fun new_entries_go_both_ways_and_both_phones_end_the_same() {
        val a = SyncData(events = listOf(e("a1", 10), e("both", 10)))
        val b = SyncData(events = listOf(e("b1", 10), e("both", 20)))
        val ca = SyncMerge.changes(a, b)
        val cb = SyncMerge.changes(b, a)
        assertEquals(1, ca.added); assertEquals(1, ca.updated)
        assertEquals(1, cb.added); assertEquals(0, cb.updated)
        assertEquals(apply(a, ca).events, apply(b, cb).events)
    }

    @Test fun a_deletion_on_one_phone_deletes_on_the_other() {
        val a = SyncData(events = emptyList(), deletions = listOf(Deletion("x", 50)))
        val b = SyncData(events = listOf(e("x", 10)))
        val cb = SyncMerge.changes(b, a)
        assertEquals(listOf("x"), cb.delete)
        assertEquals(1, cb.removed)
        // And the deleted entry doesn't come back to the phone that deleted it.
        assertTrue(SyncMerge.changes(a, b).upsert.isEmpty())
    }

    @Test fun an_edit_after_the_deletion_wins_undo_or_the_other_parent() {
        val a = SyncData(events = emptyList(), deletions = listOf(Deletion("x", 50)))
        val b = SyncData(events = listOf(e("x", 60)))
        assertTrue(SyncMerge.changes(b, a).delete.isEmpty())
        assertEquals(listOf("x"), SyncMerge.changes(a, b).upsert.map { it.id })
    }

    @Test fun nothing_new_is_nothing() {
        val a = SyncData(events = listOf(e("a", 10)))
        assertTrue(SyncMerge.changes(a, a).none)
    }

    @Test fun the_baby_comes_only_to_a_phone_that_has_none() {
        val baby = Baby(name = "Dante", birthDate = 0)
        assertEquals(baby, SyncMerge.changes(SyncData(events = emptyList()), SyncData(baby = baby, events = emptyList())).baby)
        assertNull(SyncMerge.changes(SyncData(baby = baby.copy(name = "D"), events = emptyList()), SyncData(baby = baby, events = emptyList())).baby)
    }
}
