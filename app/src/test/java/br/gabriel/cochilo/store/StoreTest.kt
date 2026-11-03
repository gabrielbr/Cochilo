package br.gabriel.cochilo.store

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoreTest {
    @Test fun the_about_link_is_the_home_with_its_campaign() {
        assertEquals("https://tataslings.com.br/?utm_source=cochilo&utm_medium=app&utm_campaign=about", Store.about)
    }

    @Test fun a_fabric_links_to_the_shops_search_for_its_sling() {
        val url = Store.fabric("Rosa Chá", "rosa_cha")
        assertEquals("https://tataslings.com.br/search/?q=sling+Rosa+Ch%C3%A1&utm_source=cochilo&utm_medium=app&utm_campaign=fabric_rosa_cha", url)
    }

    @Test fun the_safety_sheet_links_to_her_guide_not_the_shop() {
        assertTrue(Store.slingGuide.startsWith("https://tataslings.com.br/tudo-sobre-o-tata-facil-sling/"))
        assertTrue(Store.slingGuide.endsWith("utm_campaign=ticks_guide"))
    }

    @Test fun every_link_is_https_and_tagged() {
        for (u in listOf(Store.about, Store.slingGuide, Store.fabric("Balão", "balao"))) {
            assertTrue(u.startsWith("https://tataslings.com.br/"))
            assertTrue(u.contains("utm_source=cochilo&utm_medium=app&utm_campaign="))
        }
    }
}
