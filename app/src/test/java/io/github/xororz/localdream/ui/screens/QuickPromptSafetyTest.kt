package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPromptSafetyTest {
    @Test fun protectsUserTextAndAllowsUnmodifiedPresetsToCycle() {
        assertFalse(requiresQuickPromptConfirmation("", ""))
        assertTrue(requiresQuickPromptConfirmation("my character", ""))
        assertTrue(requiresQuickPromptConfirmation("", "my negative"))
        val preset = quickPromptOptions(QuickPromptCategory.ANIME, PromptFormat.TAGS).first()
        assertFalse(requiresQuickPromptConfirmation(preset.prompt, preset.negativePrompt))
        assertTrue(requiresQuickPromptConfirmation(preset.prompt + ", edited", preset.negativePrompt))
        assertTrue(requiresQuickPromptConfirmation(preset.prompt, "edited negative"))
    }

    @Test fun appendPreservesExistingTextAndAvoidsDuplicateComma() {
        assertEquals("person, anime", appendQuickPrompt("person", "anime"))
        assertEquals("person, anime", appendQuickPrompt("person, ", "anime"))
        assertEquals("anime", appendQuickPrompt("  ", "anime"))
        assertEquals("person", appendQuickPrompt("person", ""))
    }
}
