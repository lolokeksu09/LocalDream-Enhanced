package io.github.xororz.localdream.ui.screens

import io.github.xororz.localdream.data.GenerationMode
import org.junit.Assert.assertEquals
import org.junit.Test

class ModelRunCompletionTest {
    @Test
    fun resultMetadataUsesReturnedDimensionsAndSeed() {
        val requested = GenerationParameters(
            steps = 20,
            cfg = 6f,
            seed = null,
            prompt = "prompt",
            negativePrompt = "negative",
            generationTime = "",
            width = 1024,
            height = 1024,
            runOnCpu = false,
        )

        val completed = completeModelRunParameters(
            requested = requested,
            resultSeed = 42L,
            generationTime = "1.2 s",
            resultWidth = 896,
            resultHeight = 768,
            runOnCpu = true,
            mode = GenerationMode.IMG2IMG,
        )

        assertEquals(42L, completed.seed)
        assertEquals("1.2 s", completed.generationTime)
        assertEquals(896, completed.width)
        assertEquals(768, completed.height)
        assertEquals(true, completed.runOnCpu)
        assertEquals(GenerationMode.IMG2IMG, completed.mode)
        assertEquals("prompt", completed.prompt)
        assertEquals(20, completed.steps)
    }
}
