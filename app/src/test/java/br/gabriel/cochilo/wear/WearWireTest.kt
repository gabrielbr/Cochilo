package br.gabriel.cochilo.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WearWireTest {
    @Test fun state_goes_across_and_back() {
        val s = WearWire.State(true, false, 1_700_000_000_000L, "BREAST_LEFT", "BREAST_RIGHT", true)
        assertEquals(s, WearWire.decode(WearWire.encode(s)))
        val empty = WearWire.State(false, false, null, null, null, false)
        assertEquals(empty, WearWire.decode(WearWire.encode(empty)))
    }

    @Test fun anything_else_is_refused() {
        assertNull(WearWire.decode("2|1|1||||0".toByteArray()))
        assertNull(WearWire.decode("garbage".toByteArray()))
        assertNull(WearWire.action("DELETE_ALL".toByteArray()))
        assertEquals(WearWire.Action.WOKE_UP, WearWire.action("WOKE_UP".toByteArray()))
    }
}
