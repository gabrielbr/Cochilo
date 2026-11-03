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
data class Catalog(val version: Int, val moments: List<Entry>) {
    @Serializable
    data class Entry(val moment: String, val product: String, val path: String, val approved: Boolean)

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
