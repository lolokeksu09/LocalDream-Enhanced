package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunCleanupGateTest {
    @Test
    fun onlyTheFirstTeardownPathRunsCleanup() {
        val gate = ModelRunCleanupGate()

        assertTrue(gate.enter())
        assertFalse(gate.enter())
        assertFalse(gate.enter())
    }
}
