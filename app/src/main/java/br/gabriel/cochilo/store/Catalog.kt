package br.gabriel.cochilo.store

import br.gabriel.cochilo.domain.Suggestions.Moment
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The suggestions' catalog (`assets/tata/catalog.json`, docs/tata-slings/in-app-marketing.md §5):
 * which product each moment points to, where in the shop, and whether Tatá approved it. It ships
 * inside each release; the app never fetches it.
 */
@Serializable
data class Catalog(val version: Int, val moments: List<Entry>, val notes: List<Note> = emptyList()) {
    @Serializable
    data class Entry(val moment: String, val product: String, val path: String, val approved: Boolean)

    /** A note by age (§3.1): most have no product; a few link to one. */
    @Serializable
    data class Note(val band: String, val approved: Boolean, val product: String? = null, val path: String? = null)

    private fun note(b: br.gabriel.cochilo.domain.AgeNotes.Band) = notes.firstOrNull { it.band == b.name }

    /** The age notes Tatá wrote or approved. */
    val approvedNotes: Set<br.gabriel.cochilo.domain.AgeNotes.Band>
        get() = br.gabriel.cochilo.domain.AgeNotes.Band.entries.filter { note(it)?.approved == true }.toSet()

    fun noteProduct(b: br.gabriel.cochilo.domain.AgeNotes.Band): String? = note(b)?.product

    /** The shop link a note ends with, if any, tagged `note_<band>`. */
    fun noteUrl(b: br.gabriel.cochilo.domain.AgeNotes.Band): String? =
        note(b)?.path?.let { Store.page(it, "note_${b.name.lowercase()}") }

    private fun entry(m: Moment) = moments.firstOrNull { it.moment == m.name }

    /** The moments Tatá approved (and that this app version knows). */
    val approved: Set<Moment> get() = Moment.entries.filter { entry(it)?.approved == true }.toSet()

    val productOf: Map<Moment, String> get() = Moment.entries.mapNotNull { m -> entry(m)?.let { m to it.product } }.toMap()

    /** The shop link for [m], tagged with its campaign, or null. */
    fun url(m: Moment): String? = entry(m)?.let { Store.page(it.path, "suggestion_${m.name.lowercase()}") }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String): Catalog = json.decodeFromString(serializer(), text)
        val EMPTY = Catalog(0, emptyList())
    }
}
