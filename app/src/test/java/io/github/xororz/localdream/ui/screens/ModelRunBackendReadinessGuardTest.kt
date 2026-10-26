package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunBackendReadinessGuardTest {
    @Test
    fun aNewCheckInvalidatesThePreviousCallback() {
        val guard = ModelRunBackendReadinessGuard()
        val first = guard.begin()
        val second = guard.begin()

        assertFalse(guard.isCurrent(first))
        assertTrue(guard.isCurrent(second))

        guard.cancel()

        assertFalse(guard.isCurrent(second))
    }
}
