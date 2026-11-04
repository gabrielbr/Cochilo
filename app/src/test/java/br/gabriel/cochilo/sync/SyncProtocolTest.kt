package br.gabriel.cochilo.sync

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/** Two phones over pipes: pairing, then a sync both ways, as on the family's Wi-Fi. */
class SyncProtocolTest {
    private fun wires(): Pair<Wire, Wire> {
        val aOut = PipedOutputStream(); val bIn = PipedInputStream(aOut, 1 shl 20)
        val bOut = PipedOutputStream(); val aIn = PipedInputStream(bOut, 1 shl 20)
        return Wire(aIn, aOut) to Wire(bIn, bOut)
    }

    private val pool = Executors.newFixedThreadPool(2)
    private fun <T> both(a: () -> T, b: () -> T): Pair<T, T> {
        val fa = pool.submit(Callable { a() })
        val fb = pool.submit(Callable { b() })
        return fa.get(20, TimeUnit.SECONDS) to fb.get(20, TimeUnit.SECONDS)
    }

    private fun pair(): Pair<Paired?, Paired?> {
        val (a, b) = wires()
        var sasA = ""; var sasB = ""
        val r = both(
            { Pairing.run(a, "Gabriel", guest = true) { sas, _ -> sasA = sas; true } },
            { Pairing.run(b, "Ana", guest = false) { sas, _ -> sasB = sas; true } },
        )
        assertEquals(sasA, sasB)
        return r
    }

    @Test fun pairing_gives_both_phones_the_same_key_and_each_the_others_name() {
        val (a, b) = pair()
        assertNotNull(a); assertNotNull(b)
        assertArrayEquals(a!!.key, b!!.key)
        assertEquals("Ana", a.partnerName)
        assertEquals("Gabriel", b.partnerName)
    }

    @Test fun a_parent_saying_no_pairs_nothing() {
        val (a, b) = wires()
        val fa = pool.submit(Callable { Pairing.run(a, "Gabriel", guest = true) { _, _ -> false } })
        val fb = pool.submit(Callable { runCatching { Pairing.run(b, "Ana", guest = false) { _, _ -> true } } })
        assertNull(fa.get(20, TimeUnit.SECONDS))
        // The other side never gets a valid confirmation: it fails or is left waiting, never paired.
        a.send(ByteArray(32))
        assertTrue(fb.get(20, TimeUnit.SECONDS).getOrNull() == null)
    }

    @Test fun sync_sends_each_phones_log_to_the_other() {
        val (pa, pb) = pair()
        val (a, b) = wires()
        val la = SyncData(events = listOf(Event(id = "a", type = EventType.FEED, start = 1, updatedAt = 1)))
        val lb = SyncData(events = listOf(Event(id = "b", type = EventType.SLEEP, start = 2, updatedAt = 2)))
        val (gotA, gotB) = both({ SyncSession.asClient(a, pa!!.key) { la } }, { SyncSession.asServer(b, pb!!.key) { lb } })
        assertEquals(lb, gotA)
        assertEquals(la, gotB)
    }

    @Test fun a_phone_without_the_key_is_refused() {
        val (pa, _) = pair()
        val (a, b) = wires()
        val stranger = SyncCrypto.nonce(32)
        val r = both(
            { runCatching { SyncSession.asClient(a, stranger) { SyncData(events = emptyList()) } } },
            // As in the app: a refusal closes the connection, so the stranger's wait ends.
            { runCatching { SyncSession.asServer(b, pa!!.key) { SyncData(events = emptyList()) } }.also { b.close() } },
        )
        assertTrue(r.second.exceptionOrNull() is SyncRefused)
    }
}
