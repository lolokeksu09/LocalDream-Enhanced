package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunDisplayStateTest {
    @Test fun progressKeepsOriginalStartTimeAndRunningState() {
        val state = ModelRunDisplayState("model-1")
        assertNull(state.generationStartTime)
        state.onProgress(value = 0.25f, intermediate = null, nowMillis = 10_000L)
        assertTrue(state.isRunning)
        assertEquals(0.25f, state.progress, 0.0001f)
        assertEquals(10_000L, state.generationStartTime)
        state.onProgress(value = 0.8f, intermediate = null, nowMillis = 11_000L)
        assertEquals(10_000L, state.generationStartTime)
        assertEquals("1.0s", state.elapsedLabel(11_000L))
    }

    @Test fun completeDoesNotEndAnOngoingBatch() {
        val state = ModelRunDisplayState("model-2")
        state.onProgress(0.7f, null, 1_000L)
        state.onComplete(42L)
        assertTrue(state.isRunning) // The batch controller decides when to end the batch.
        assertEquals(0f, state.progress, 0.0001f)
        assertEquals(42L, state.returnedSeed)
        state.onComplete(null)
        assertNull(state.returnedSeed)
        state.onIdle()
        assertFalse(state.isRunning)
    }

    @Test fun errorResetsProgressAndTimerAndCanBeDismissed() {
        val state = ModelRunDisplayState("model-3")
        state.onProgress(0.75f, null, 500L)
        state.onError("backend failed")
        assertFalse(state.isRunning)
        assertEquals(0f, state.progress, 0.0001f)
        assertEquals("backend failed", state.errorMessage)
        assertNull(state.generationStartTime)
        state.errorMessage = null
        state.onProgress(0.2f, null, 1_000L)
        assertTrue(state.isRunning)
    }

    @Test fun interruptAndExitClearOnlyTheOriginalFields() {
        val state = ModelRunDisplayState("model-4")
        state.onProgress(0.5f, null, 300L)
        state.errorMessage = "previous failure"
        state.onInterrupt()
        assertFalse(state.isRunning)
        assertEquals("previous failure", state.errorMessage) // Interrupt historically retains it.
        assertNull(state.generationStartTime)
        assertEquals("model-4", state.generationParamsModelId)
        assertEquals(0, state.imageVersion)
        state.onExit()
        assertNull(state.errorMessage)
        assertNull(state.currentBitmap)
        assertNull(state.generationParams)
    }

    @Test fun durationLabelsMatchPreviousScreenFormatting() {
        assertEquals("900ms", formatGenerationDuration(900L))
        assertEquals("1.2s", formatGenerationDuration(1_200L))
        assertEquals("59.9s", formatGenerationDuration(59_900L))
        assertEquals("1m0s", formatGenerationDuration(60_000L))
        assertEquals("2m5s", formatGenerationDuration(125_000L))
    }
}
