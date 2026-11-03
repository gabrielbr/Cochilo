package br.gabriel.cochilo.ui.kit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArcTest {
    @Test fun the_column_bows_deepest_in_the_middle_and_not_at_the_ends() {
        val bows = (0 until 8).map { arcOffset(it, 8, 36) }
        assertEquals(0, bows.first()); assertEquals(0, bows.last())
        assertTrue(bows.all { it in 0..36 })
        assertTrue(bows[3] >= bows[1] && bows[4] >= bows[6])
    }

    @Test fun short_menus_stay_straight() {
        assertEquals(listOf(0, 0), (0 until 2).map { arcOffset(it, 2, 36) })
    }
}
