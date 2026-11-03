package br.gabriel.cochilo.store

import br.gabriel.cochilo.domain.Suggestions.Moment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class CatalogTest {
    private val catalog = Catalog.parse(File("src/main/assets/tata/catalog.json").readText())

    @Test fun every_moment_has_a_product_and_a_shop_page() {
        for (m in Moment.entries) {
            assertTrue("$m has a product", catalog.productOf[m] != null)
            val url = catalog.url(m)!!
            assertTrue(url, url.startsWith("https://tataslings.com.br/") && url.endsWith("utm_campaign=suggestion_${m.name.lowercase()}"))
        }
    }

    /**
     * A moment shows only once Tatá signs it off; flipping one is a reviewed change here. Step 4
     * (in-app-marketing.md §12) turns them on one at a time: naps in arms first (0.36.0).
     */
    @Test fun only_the_moments_tata_signed_off_are_approved() {
        assertEquals(setOf(Moment.ARMS_NAPS), catalog.approved)
    }

    @Test fun every_age_band_has_a_note_entry_none_approved_most_without_a_product() {
        for (b in br.gabriel.cochilo.domain.AgeNotes.Band.entries) assertTrue("$b", catalog.notes.any { it.band == b.name })
        assertEquals(emptySet<br.gabriel.cochilo.domain.AgeNotes.Band>(), catalog.approvedNotes)
        assertEquals(1, catalog.notes.count { it.product != null })
        assertEquals("https://tataslings.com.br/tata-facil-sling/?utm_source=cochilo&utm_medium=app&utm_campaign=note_m1_2", catalog.noteUrl(br.gabriel.cochilo.domain.AgeNotes.Band.M1_2))
        assertEquals(null, catalog.noteUrl(br.gabriel.cochilo.domain.AgeNotes.Band.M3_4))
    }

    @Test fun approval_is_read_per_moment() {
        val c = Catalog.parse("""{"version":1,"moments":[{"moment":"SIX_MONTHS","product":"tata_to","path":"/x/","approved":true},{"moment":"GONE","product":"p","path":"y/","approved":true}]}""")
        assertEquals(setOf(Moment.SIX_MONTHS), c.approved)
        assertEquals("https://tataslings.com.br/x/?utm_source=cochilo&utm_medium=app&utm_campaign=suggestion_six_months", c.url(Moment.SIX_MONTHS))
    }
}
