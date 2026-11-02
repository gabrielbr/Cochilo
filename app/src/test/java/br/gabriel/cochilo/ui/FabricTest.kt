package br.gabriel.cochilo.ui

import br.gabriel.cochilo.ui.kit.Fabric
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FabricTest {
    @Test fun savedKeyComesBack() {
        Fabric.entries.forEach { assertEquals(it, Fabric.of(it.key)) }
    }

    @Test fun unknownOrMissingIsPlain() {
        assertEquals(Fabric.PLAIN, Fabric.of(null))
        assertEquals(Fabric.PLAIN, Fabric.of("lavanda-that-does-not-exist"))
    }

    @Test fun plainHasNoTextureAndEveryOtherHasOne() {
        assertNull(Fabric.PLAIN.texture)
        Fabric.entries.filter { it != Fabric.PLAIN }.forEach { assertNotNull(it.key, it.texture) }
    }

    @Test fun keysAreUnique() {
        assertEquals(Fabric.entries.size, Fabric.entries.map { it.key }.toSet().size)
    }
}
