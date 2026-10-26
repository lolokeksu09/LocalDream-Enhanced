package io.github.xororz.localdream.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemotePairingKeyTest {
    @Test fun newSessionKeysAreValidAndDistinct() {
        val a = RemotePairingKey.generate()
        val b = RemotePairingKey.generate()
        assertTrue(a.matches(Regex("[0-9A-F]{32}")))
        assertTrue(b.matches(Regex("[0-9A-F]{32}")))
        assertNotEquals(a, b)
    }

    @Test fun normalizationRejectsMalformedOrShortKeys() {
        assertNull(RemotePairingKey.normalize(""))
        assertNull(RemotePairingKey.normalize("123456"))
        assertNull(RemotePairingKey.normalize("GG".repeat(16)))
        assertEquals(
            "0123456789ABCDEF0123456789ABCDEF",
            RemotePairingKey.normalize(" 0123456789abcdef0123456789abcdef "),
        )
    }

    @Test fun onlyExactSessionKeyAuthenticates() {
        val key = "0123456789ABCDEF0123456789ABCDEF"
        assertTrue(RemotePairingKey.matches(key, key))
        assertFalse(RemotePairingKey.matches(key, null))
        assertFalse(RemotePairingKey.matches(key, key.dropLast(1)))
        assertFalse(RemotePairingKey.matches(key, "0123456789ABCDEF0123456789ABCDE0"))
    }
}
