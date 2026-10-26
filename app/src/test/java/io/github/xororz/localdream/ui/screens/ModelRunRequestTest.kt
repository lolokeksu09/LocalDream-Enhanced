package io.github.xororz.localdream.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunRequestTest {
    private fun normal(
        seed: String = "",
        hasImage: Boolean = false,
        hasMask: Boolean = false,
        hasReferenceImages: Boolean = false,
    ) = ModelRunRequest(
        prompt = "portrait", negativePrompt = "blur", steps = 22, cfg = 5.5f,
        seed = seed, width = 1024, height = 1024,
        effectiveWidth = 768, effectiveHeight = 512, denoiseStrength = 0.45f,
        useOpenCL = true, scheduler = "dpm", aspectRatio = "3:2",
        runOnCpu = false, backendHost = "192.168.1.5:8082",
        batchIndex = 2, hasReferenceImages = hasReferenceImages,
        hasImage = hasImage, hasMask = hasMask,
    )

    @Test fun normalRequestRetainsExactServiceExtrasAndImageFlags() {
        val extras = normal(
            seed = "1234", hasImage = true, hasMask = true, hasReferenceImages = true,
        ).extras()
        assertEquals("portrait", extras["prompt"])
        assertEquals("blur", extras["negative_prompt"])
        assertEquals(22, extras["steps"])
        assertEquals(5.5f, extras["cfg"])
        assertEquals(1234L, extras["seed"])
        assertEquals(1024, extras["width"])
        assertEquals(1024, extras["height"])
        assertEquals(768, extras["effective_width"])
        assertEquals(512, extras["effective_height"])
        assertEquals(0.45f, extras["denoise_strength"])
        assertEquals(true, extras["use_opencl"])
        assertEquals("dpm", extras["scheduler"])
        assertEquals("3:2", extras["aspect_ratio"])
        assertEquals(2, extras["batch_index"])
        assertEquals("192.168.1.5:8082", extras["backend_host"])
        assertEquals(true, extras["has_reference_images"])
        assertEquals(true, extras["has_image"])
        assertEquals(true, extras["has_mask"])
        assertFalse(extras.containsKey("ultrafix"))
    }

    @Test fun absentOrInvalidSeedIsNotForwardedToService() {
        assertFalse(normal(seed = "").extras().containsKey("seed"))
        assertFalse(normal(seed = "invalid").extras().containsKey("seed"))
        assertFalse(normal(seed = "9223372036854775808").extras().containsKey("seed"))
    }

    @Test fun maskFlagNeverAppearsWithoutBaseImage() {
        val extras = normal(hasImage = false, hasMask = true).extras()
        assertFalse(extras.containsKey("has_image"))
        assertFalse(extras.containsKey("has_mask"))
        assertFalse(extras.containsKey("has_reference_images"))
    }

    @Test fun ultrafixPreservesDistinctExtrasWithoutRegularGenerationFlags() {
        val request = normal(seed = "14", hasImage = true, hasMask = true).copy(
            prompt = "masterpiece, best quality, 4k resolution",
            useOpenCL = false, effectiveWidth = 2048, effectiveHeight = 1536,
            width = 2048, height = 1536,
            ultrafixTileSize = 1024,
        )
        val extras = request.extras()
        assertEquals(true, extras["ultrafix"])
        assertEquals(1024, extras["ultrafix_tile_size"])
        assertEquals(2048, extras["width"])
        assertEquals(1536, extras["height"])
        assertEquals(14L, extras["seed"])
        assertFalse(extras.containsKey("use_opencl"))
        assertFalse(extras.containsKey("aspect_ratio"))
        assertFalse(extras.containsKey("batch_index"))
        assertFalse(extras.containsKey("has_image"))
        assertFalse(extras.containsKey("has_mask"))
        assertTrue(request.isUltrafix)
        assertFalse(normal().isUltrafix)
    }

    @Test fun generatedHistoryUsesSnapshotRatherThanReReadingUiState() {
        val request = normal(seed = "12")
        val params = request.historyParameters()
        assertEquals("portrait", params.prompt)
        assertEquals("blur", params.negativePrompt)
        assertEquals(22, params.steps)
        assertEquals(5.5f, params.cfg)
        assertEquals(0L, params.seed)
        assertEquals(1024, params.width)
        assertEquals(1024, params.height)
        assertEquals(0.45f, params.denoiseStrength)
        assertEquals("dpm", params.scheduler)
    }
}
