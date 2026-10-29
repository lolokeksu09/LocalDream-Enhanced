package io.github.xororz.localdream.ui.screens

import io.github.xororz.localdream.data.GenerationMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunPresentationTest {
    @Test fun generationModePrecedenceMatchesExistingBehavior() {
        fun mode(ultra: Boolean = false, edit: Boolean = false, base: Boolean = false,
                 refs: Boolean = false, inpaint: Boolean = false) =
            resolveGenerationMode(ultra, edit, base, refs, inpaint)
        assertEquals(GenerationMode.TXT2IMG, mode())
        assertEquals(GenerationMode.IMG2IMG, mode(base = true))
        assertEquals(GenerationMode.INPAINT, mode(base = true, inpaint = true))
        assertEquals(GenerationMode.EDIT, mode(edit = true, refs = true))
        assertEquals(GenerationMode.EDIT, mode(edit = true, base = true, inpaint = true))
        assertEquals(GenerationMode.INPAINT, mode(edit = true, inpaint = true))
        assertEquals(GenerationMode.ULTRAFIX, mode(ultra = true, edit = true, refs = true))
    }

    @Test fun embeddingMatchesAreNormalizedAndPrefixFirst() {
        val names = listOf("some fine_embedding", "Fine-Embedding", "fine embedding 2", "unrelated")
        val matches = matchingEmbeddingSuggestions(names, " FINE EMBEDDING ")
        assertEquals(
            listOf("Fine-Embedding", "fine embedding 2", "some fine_embedding"),
            matches.map { it.replacementTag },
        )
        assertTrue(matchingEmbeddingSuggestions(names, " ").isEmpty())
        assertTrue(matchingEmbeddingSuggestions(names, "no-match").isEmpty())
        assertEquals(1, matchingEmbeddingSuggestions(names, "fine_embedding", 1).size)
    }
}
