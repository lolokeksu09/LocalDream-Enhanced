package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GenerationQuickSettingsTest {
    @Test fun onlyExactPresetValuesAreMarkedSelected() {
        assertEquals(QuickSettingsPreset.FAST, selectedQuickSettingsPreset(8f, 4.5f))
        assertEquals(QuickSettingsPreset.QUALITY, selectedQuickSettingsPreset(20f, 7f))
        assertEquals(QuickSettingsPreset.DETAIL, selectedQuickSettingsPreset(30f, 7.5f))
        assertNull(selectedQuickSettingsPreset(10f, 5f))
        assertNull(selectedQuickSettingsPreset(25f, 7f))
    }

    @Test fun tinyFloatNoiseDoesNotClearSelection() {
        assertEquals(QuickSettingsPreset.QUALITY, selectedQuickSettingsPreset(20.004f, 7.004f))
        assertNull(selectedQuickSettingsPreset(20.02f, 7f))
    }
}
