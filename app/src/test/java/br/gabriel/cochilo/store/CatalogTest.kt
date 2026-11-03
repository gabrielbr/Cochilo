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

    /** Nothing is shown until Tatá signs a moment off; flipping one is a reviewed change here. */
    @Test fun nothing_is_approved_yet() {
        assertEquals(emptySet<Moment>(), catalog.approved)
    }

    @Test fun approval_is_read_per_moment() {
        val c = Catalog.parse("""{"version":1,"moments":[{"moment":"SIX_MONTHS","product":"tata_to","path":"/x/","approved":true},{"moment":"GONE","product":"p","path":"y/","approved":true}]}""")
        assertEquals(setOf(Moment.SIX_MONTHS), c.approved)
        assertEquals("https://tataslings.com.br/x/?utm_source=cochilo&utm_medium=app&utm_campaign=suggestion_six_months", c.url(Moment.SIX_MONTHS))
    }
}
