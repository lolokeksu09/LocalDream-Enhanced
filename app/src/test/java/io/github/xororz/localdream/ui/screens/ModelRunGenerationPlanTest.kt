package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunGenerationPlanTest {
    @Test
    fun batchRequestKeepsImageMaskReferencesAndIndexFlags() {
        val inputs = ModelRunBatchRequestInputs(
            prompt = "p",
            negativePrompt = "n",
            steps = 24,
            cfg = 5f,
            seed = "123",
            width = 768,
            height = 512,
            effectiveWidth = 768,
            effectiveHeight = 512,
            denoiseStrength = 0.6f,
            useOpenCL = false,
            scheduler = "dpm",
            aspectRatio = "3:2",
            runOnCpu = false,
            backendHost = "http://127.0.0.1:8000",
            hasImage = true,
            hasMask = true,
        )

        val request = buildModelRunBatchRequest(inputs, index = 2, hasReferences = true)

        assertEquals(2, request.batchIndex)
        assertTrue(request.hasReferenceImages)
        assertTrue(request.hasImage)
        assertTrue(request.hasMask)
        assertEquals("123", request.seed)
        assertEquals(768, request.width)
    }
}
