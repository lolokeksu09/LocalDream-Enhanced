package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunHistorySelectionTest {
    @Test fun newGenerationClearsOldSelectionUntilDatabaseSaveCompletes() {
        val state = ModelRunHistorySelection()
        state.onGenerationSaved(id = 17L, wasUltrafix = false)
        assertEquals(17L, state.displayedHistoryId)
        assertTrue(state.isDisplayedStitchable)

        state.beginGeneratedResult()
        assertNull(state.displayedHistoryId)
        assertFalse(state.isDisplayedStitchable)
        state.onGenerationSaved(id = 19L, wasUltrafix = false)
        assertEquals(19L, state.displayedHistoryId)
        assertTrue(state.isDisplayedStitchable)
    }

    @Test fun switchingToUnrelatedHistoryItemNeverMakesItStitchable() {
        val state = ModelRunHistorySelection()
        state.onGenerationSaved(101L, wasUltrafix = false)
        state.showHistoryItem(102L)
        assertFalse(state.isDisplayedStitchable)
        state.showHistoryItem(101L)
        assertTrue(state.isDisplayedStitchable)
    }

    @Test fun upscaleInheritsEligibilityOnlyFromEligibleSource() {
        val state = ModelRunHistorySelection()
        state.onGenerationSaved(10L, wasUltrafix = false)
        val originalEligible = state.isDisplayedStitchable
        state.onUpscaleSaved(11L, originalEligible)
        assertEquals(11L, state.displayedHistoryId)
        assertTrue(state.isDisplayedStitchable)

        state.showHistoryItem(20L)
        val unrelatedEligible = state.isDisplayedStitchable
        state.onUpscaleSaved(21L, unrelatedEligible)
        assertEquals(21L, state.displayedHistoryId)
        assertFalse(state.isDisplayedStitchable)
    }

    @Test fun ultrafixOrImportedImageIsNotEligibleForStitching() {
        val state = ModelRunHistorySelection()
        state.onGenerationSaved(31L, wasUltrafix = false)
        state.onImportedImage()
        assertNull(state.displayedHistoryId)
        assertFalse(state.isDisplayedStitchable)
        state.onGenerationSaved(32L, wasUltrafix = true)
        assertEquals(32L, state.displayedHistoryId)
        assertFalse(state.isDisplayedStitchable)
    }

    @Test fun favoriteCommandNeedsDisplayedRowAndObservedDatabaseValue() {
        val state = ModelRunHistorySelection()
        assertNull(state.requestedFavoriteChange(true))
        state.showHistoryItem(5L)
        assertNull(state.requestedFavoriteChange(null))
        assertEquals(5L to true, state.requestedFavoriteChange(false))
        assertEquals(5L to false, state.requestedFavoriteChange(true))
        assertEquals(5L, state.displayedHistoryId)
    }
}
