package br.gabriel.cochilo.data

import br.gabriel.cochilo.domain.otherBreast

/**
 * The entry actions the app and the notification's buttons share, so a tap on "Woke up" in the
 * notification does exactly what the button on Today does.
 */
class Entries(private val events: EventDao) {

    /**
     * Starts a sleep now, unless one is already running (only one sleep runs at a time). Nothing
     * else is asked: where it happened is asked when the baby wakes (details after, 0.28.0).
     */
    suspend fun startSleep(now: Long = System.currentTimeMillis(), zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): Event? {
        val all = events.all()
        if (all.any { it.type == EventType.SLEEP && it.end == null }) return null
        // Nap or the night (docs/nights.md): guessed from an open night, the baby's own bedtime and
        // the hour; one tap on Today changes it. During a night waking this is "back to sleep".
        val sleeps = all.filter { it.type == EventType.SLEEP }.sortedBy { it.start }
        val bedtime = br.gabriel.cochilo.domain.Predictor(zone).bedtimeMinute(sleeps, now)
        val night = br.gabriel.cochilo.domain.Nights.guess(all, now, zone, bedtime)
        return Event(type = EventType.SLEEP, start = now, updatedAt = now, night = night)
            .also { events.upsert(it) }
    }

    /**
     * Up for the day (docs/nights.md): the night is over. A running night sleep ends now; the
     * night's last sleep is marked, so its end is the morning wake and the next sleep is a nap.
     */
    suspend fun upForDay(now: Long = System.currentTimeMillis()) {
        wakeUp(now)
        val last = events.all().filter { it.type == EventType.SLEEP && it.end != null }.maxByOrNull { it.start } ?: return
        if (last.upForDay != true) events.upsert(last.copy(upForDay = true, night = last.night ?: true, updatedAt = now))
    }

    /** Nap or the night, changed by hand (one tap on Today). */
    suspend fun setNight(id: String, night: Boolean, now: Long = System.currentTimeMillis()) {
        val e = events.byId(id) ?: return
        events.upsert(e.copy(night = night, upForDay = if (night) e.upForDay else null, updatedAt = now))
    }

    /** Ends every running sleep now. */
    suspend fun wakeUp(now: Long = System.currentTimeMillis()) {
        events.all().filter { it.type == EventType.SLEEP && it.end == null }
            .forEach { events.upsert(it.copy(end = now, updatedAt = now)) }
    }

    /** A routine (nap or bedtime, [kind]) begins now: the window reminder's Start routine. */
    suspend fun logRoutine(kind: String, now: Long = System.currentTimeMillis()): Event =
        Event(type = EventType.ROUTINE, start = now, kind = kind, updatedAt = now).also { events.upsert(it) }

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
