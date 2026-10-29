package io.github.xororz.localdream.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelSearchTest {
    @Test
    fun searchMatchesVisibleMetadataCaseInsensitivelyAndKeepsBlankQueryUnfiltered() {
        val fields = mapOf(
            "name" to "Illustrious v16",
            "description" to "Anime image generation",
            "id" to "illustriousv16",
            "baseUrl" to "https://huggingface.co/example/illustrious",
        )

        fun matches(query: String) = matchesModelSearch(
            query = query,
            name = fields.getValue("name"),
            description = fields.getValue("description"),
            id = fields.getValue("id"),
            baseUrl = fields.getValue("baseUrl"),
        )

        assertTrue(matches("  ILLUSTRIOUS "))
        assertTrue(matches("anime"))
        assertTrue(matches("huggingface"))
        assertTrue(matches(""))
        assertFalse(matches("flux.2"))
    }
}
