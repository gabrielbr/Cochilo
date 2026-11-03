package br.gabriel.cochilo.backup

import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SealedTest {
    // Few iterations keep the tests fast; the real default is checked separately.
    private val fast = 1_000
    private val plain = Backup.toJson(null, listOf(Event(type = EventType.SLEEP, start = 1_000L, end = 2_000L)), 3_000L)

    @Test fun round_trip_gives_back_the_same_backup() {
        val sealed = Sealed.seal(plain, "lua cheia", fast)
        assertEquals(plain, Sealed.open(sealed, "lua cheia"))
        assertEquals(1, Backup.fromJson(Sealed.open(sealed, "lua cheia")).events.size)
    }

    @Test fun the_sealed_file_hides_the_entries() {
        val sealed = Sealed.seal(plain, "lua cheia", fast)
        assertTrue(Sealed.isSealed(sealed))
        assertFalse(Sealed.isSealed(plain))
        assertFalse(sealed.contains("SLEEP"))
        assertFalse(sealed.contains("cochilo-backup\""))
    }

    @Test fun a_wrong_passphrase_is_refused() {
        val sealed = Sealed.seal(plain, "lua cheia", fast)
        assertThrows(WrongPassphrase::class.java) { Sealed.open(sealed, "lua nova") }
    }

    @Test fun a_changed_file_is_refused() {
        val sealed = Sealed.seal(plain, "lua cheia", fast)
        val data = Regex("\"data\": \"([^\"]+)\"").find(sealed)!!.groupValues[1]
        val flipped = (if (data[0] == 'A') 'B' else 'A') + data.drop(1)
        assertThrows(WrongPassphrase::class.java) { Sealed.open(sealed.replace(data, flipped), "lua cheia") }
    }

    @Test fun each_seal_is_different() {
        assertNotEquals(Sealed.seal(plain, "lua cheia", fast), Sealed.seal(plain, "lua cheia", fast))
    }

    @Test fun too_short_a_passphrase_is_refused() {
        assertThrows(IllegalArgumentException::class.java) { Sealed.seal(plain, "abc", fast) }
    }

    @Test fun a_plain_backup_is_not_opened_as_sealed() {
        assertThrows(IllegalArgumentException::class.java) { Sealed.open(plain, "lua cheia") }
    }

    @Test fun the_default_strength_is_recorded_in_the_file() {
        val sealed = Sealed.seal(plain, "lua cheia")
        assertTrue(sealed.contains("\"iterations\": ${SealedFile.ITERATIONS}"))
        assertEquals(plain, Sealed.open(sealed, "lua cheia"))
    }
}
