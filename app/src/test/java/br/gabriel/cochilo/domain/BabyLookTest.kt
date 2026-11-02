package br.gabriel.cochilo.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BabyLookTest {
    @Test fun roundTrips() {
        for (skin in 1..BabyLook.SKIN_TONES) for (h in Hair.entries) for (c in HairColor.entries) for (g in BabyGender.entries) {
            val look = BabyLook(skin, h, c, g)
            assertEquals(look, BabyLook.decode(look.encode()))
        }
    }

    @Test fun emptyOrBrokenIsTheDefault() {
        assertEquals(BabyLook(), BabyLook.decode(null))
        assertEquals(BabyLook(), BabyLook.decode(""))
        assertEquals(BabyLook(), BabyLook.decode("nonsense"))
        assertEquals(BabyLook(), BabyLook.decode("hair=PURPLE;gender=?"))
    }

    @Test fun skinIsClampedToTheSixTones() {
        assertEquals(1, BabyLook.decode("skin=0").skin)
        assertEquals(6, BabyLook.decode("skin=9").skin)
    }

    @Test fun unknownKeysAreIgnored_soNewerChoicesStillRead() {
        assertEquals(BabyLook(skin = 5), BabyLook.decode("skin=5;freckles=yes"))
    }
}
