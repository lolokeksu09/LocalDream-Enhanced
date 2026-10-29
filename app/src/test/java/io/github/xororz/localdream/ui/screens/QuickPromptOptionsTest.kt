package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPromptOptionsTest {
    @Test fun everyFormatAndCategoryCyclesThroughDistinctPrompts() {
        for (format in PromptFormat.values()) {
            for (category in QuickPromptCategory.values()) {
                val options = quickPromptOptions(category, format)
                assertEquals(96, options.size)
                assertEquals(options.size, options.map { it.prompt }.distinct().size)
                var current = ""
                for (expected in options) {
                    val next = nextQuickPrompt(category, current, format)
                    assertEquals(expected, next)
                    assertNotEquals(current, next.prompt)
                    assertTrue(next.negativePrompt.isNotBlank())
                    current = next.prompt
                }
                assertEquals(options.first(), nextQuickPrompt(category, current, format))
            }
        }
    }

    @Test fun manualSelectionOverridesMetadataAndAutomaticDefaults() {
        assertEquals(PromptFormat.TAGS, resolvePromptFormat(PromptFormatChoice.TAGS, "description", true))
        assertEquals(PromptFormat.UNIVERSAL, resolvePromptFormat(PromptFormatChoice.UNIVERSAL, "tags", false))
        assertEquals(PromptFormat.DESCRIPTION, resolvePromptFormat(PromptFormatChoice.AUTO, null, true))
        assertEquals(PromptFormat.TAGS, resolvePromptFormat(PromptFormatChoice.AUTO, "tags", false))
        assertEquals(PromptFormat.UNIVERSAL, resolvePromptFormat(PromptFormatChoice.AUTO, "unknown", false))
        assertEquals(PromptFormatChoice.AUTO, promptFormatChoice("invalid"))
        assertEquals(PromptFormatChoice.DESCRIPTION, promptFormatChoice("description"))
    }

    @Test fun formatChangeStartsASeparateCycle() {
        val tagPrompt = nextQuickPrompt(QuickPromptCategory.ANIME, "", PromptFormat.TAGS).prompt
        val sentence = nextQuickPrompt(QuickPromptCategory.ANIME, tagPrompt, PromptFormat.DESCRIPTION)
        assertEquals(quickPromptOptions(QuickPromptCategory.ANIME, PromptFormat.DESCRIPTION).first(), sentence)
        assertTrue(sentence.prompt.startsWith("A scene showing "))
        assertNotEquals(tagPrompt, sentence.prompt)
    }
}
