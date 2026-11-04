package br.gabriel.cochilo.wear

/**
 * What the phone and the watch say to each other (0.53.0, docs/wear.md). Shared by both apps (the
 * watch app compiles this file too), pure, so it's tested without a phone.
 *
 * - The phone sends [State] to the watch whenever the log changes, on [STATE].
 * - The watch sends one of [Action]'s names on [ACTION] when a tile button is tapped, and an
 *   empty [ASK] when its tile is shown and it has nothing yet.
 *
 * Only to a watch connected directly (Bluetooth), never through the cloud: the baby's data never
 * leaves the home.
 */
object WearWire {
    const val STATE = "/cochilo/state"
    const val ACTION = "/cochilo/action"
    const val ASK = "/cochilo/ask"

    enum class Action { START_SLEEP, WOKE_UP, FEED_LEFT, FEED_RIGHT, FINISH_FEED }

    data class State(
        val hasBaby: Boolean,
        val asleep: Boolean,
        /** When the sleep began, or the last one ended. */
        val since: Long?,
        /** The breast feeding now ("BREAST_LEFT" / "BREAST_RIGHT"), if any. */
        val feeding: String?,
        /** The breast used last, for the Last mark. */
        val lastBreast: String?,
        /** Awake in the night: the button reads Back to sleep. */
        val nightWaking: Boolean,
    )

    private const val V = "1"

    fun encode(s: State): ByteArray = listOf(
        V, flag(s.hasBaby), flag(s.asleep), s.since?.toString() ?: "", s.feeding ?: "", s.lastBreast ?: "", flag(s.nightWaking),
    ).joinToString("|").toByteArray()

    /** Null for anything this version can't read. */
    fun decode(b: ByteArray): State? {
        val p = String(b).split("|")
        if (p.size < 7 || p[0] != V) return null
        return State(
            hasBaby = p[1] == "1", asleep = p[2] == "1", since = p[3].toLongOrNull(),
            feeding = p[4].ifEmpty { null }, lastBreast = p[5].ifEmpty { null }, nightWaking = p[6] == "1",
        )
    }

    fun action(b: ByteArray): Action? = Action.entries.firstOrNull { it.name == String(b) }

    private fun flag(v: Boolean) = if (v) "1" else "0"
}
