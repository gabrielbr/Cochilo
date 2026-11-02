package br.gabriel.cochilo.data

import br.gabriel.cochilo.domain.otherBreast

/**
 * The entry actions the app and the notification's buttons share, so a tap on "Woke up" in the
 * notification does exactly what the button on Today does.
 */
class Entries(private val events: EventDao) {

    /**
     * Starts a sleep now, unless one is already running (only one sleep runs at a time), in the
     * place used last (remember actions: crib, arms, sling…), so every button remembers it.
     */
    suspend fun startSleep(now: Long = System.currentTimeMillis()): Event? {
        val all = events.all()
        if (all.any { it.type == EventType.SLEEP && it.end == null }) return null
        return Event(type = EventType.SLEEP, start = now, kind = br.gabriel.cochilo.domain.Places.last(all), updatedAt = now)
            .also { events.upsert(it) }
    }

    /** Ends every running sleep now. */
    suspend fun wakeUp(now: Long = System.currentTimeMillis()) {
        events.all().filter { it.type == EventType.SLEEP && it.end == null }
            .forEach { events.upsert(it.copy(end = now, updatedAt = now)) }
    }

    /** Ends a running timer now. */
    suspend fun finish(id: String, now: Long = System.currentTimeMillis()) {
        val e = events.byId(id) ?: return
        if (e.end == null) events.upsert(e.copy(end = now, updatedAt = now))
    }

    /**
     * Starts a breastfeed on [side] now (the widget's Left / Right). If one is already running on
     * that side, nothing changes; on the other side, it switches (that side ends, this one starts).
     */
    suspend fun startBreastfeed(side: String, now: Long = System.currentTimeMillis()): Event? {
        val running = events.all().filter { it.type == EventType.FEED && it.end == null && otherBreast(it.kind) != null }.maxByOrNull { it.start }
        if (running != null) return if (running.kind == side) null else switchSide(running.id, now)?.second
        return Event(type = EventType.FEED, start = now, kind = side, updatedAt = now).also { events.upsert(it) }
    }

    /** During a breastfeed: ends this side now and starts the other. Returns (ended, started). */
    suspend fun switchSide(id: String, now: Long = System.currentTimeMillis()): Pair<Event, Event>? {
        val e = events.byId(id) ?: return null
        val other = otherBreast(e.kind) ?: return null
        if (e.end != null) return null
        val ended = e.copy(end = now, updatedAt = now)
        val next = Event(type = EventType.FEED, start = now, kind = other, updatedAt = now)
        events.upsert(ended)
        events.upsert(next)
        return ended to next
    }
}
