package io.github.xororz.localdream.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteTlsPairingTest {
    @Test fun pairingEncodesSecretAndPinnedCertificateHash() {
        val pair = RemoteTls.Pairing("A".repeat(32), "B".repeat(64))
        assertEquals(pair, RemoteTls.parse("  " + pair.encode().lowercase() + " "))
    }

    @Test fun rejectsLegacyUnpinnedAndMalformedCodes() {
        assertNull(RemoteTls.parse("A".repeat(32)))
        assertNull(RemoteTls.parse("A".repeat(32) + "." + "B".repeat(63)))
        assertNull(RemoteTls.parse("Z".repeat(32) + "." + "B".repeat(64)))
        assertNull(RemoteTls.parse(""))
    }
}
