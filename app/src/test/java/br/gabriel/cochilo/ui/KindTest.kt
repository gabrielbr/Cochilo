package br.gabriel.cochilo.ui

import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.ui.kit.kindBadgeSize
import br.gabriel.cochilo.ui.kit.leads
import br.gabriel.cochilo.ui.theme.CTokens.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sleep leads: it is the one kind a size up (AGENTS.md, docs/flows.md "Sleep leads"). */
class KindTest {
    @Test fun onlySleepLeads() {
        assertEquals(listOf(EventType.SLEEP), EventType.entries.filter { it.leads })
    }

    @Test fun sleepsRoundIsBigger() {
        assertEquals(Size.kindBadgeLead, kindBadgeSize(EventType.SLEEP))
        EventType.entries.filter { it != EventType.SLEEP }.forEach { assertEquals(Size.kindBadge, kindBadgeSize(it)) }
        assertTrue(Size.kindBadgeLead > Size.kindBadge)
    }
}
