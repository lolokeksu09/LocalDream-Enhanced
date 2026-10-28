package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuickPromptOptionsTest {
    @Test fun everyCategoryCyclesThroughDistinctPromptsBeforeRepeating() {
        for (category in QuickPromptCategory.values()) {
            val options = quickPromptOptions(category)
            assertTrue(options.size >= 90)
            assertEquals(options.size, options.map { it.prompt }.distinct().size)
            var current = ""
            for (expected in options) {
                val next = nextQuickPrompt(category, current)
                assertEquals(expected, next)
                assertNotEquals(current, next.prompt)
                assertTrue(next.negativePrompt.isNotBlank())
                current = next.prompt
            }
            assertEquals(options.first(), nextQuickPrompt(category, current))
        }
    }

    @Test fun unknownOrOtherCategoryPromptStartsAtFirstOption() {
        val otherPrompt = quickPromptOptions(QuickPromptCategory.ANIME).first().prompt
        assertEquals(
            quickPromptOptions(QuickPromptCategory.REALISM).first(),
            nextQuickPrompt(QuickPromptCategory.REALISM, otherPrompt),
        )
        assertEquals(
            quickPromptOptions(QuickPromptCategory.ANIME).first(),
            nextQuickPrompt(QuickPromptCategory.ANIME, "my own prompt"),
        )
    }
}
