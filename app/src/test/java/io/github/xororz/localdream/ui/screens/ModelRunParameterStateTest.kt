package io.github.xororz.localdream.ui.screens

import io.github.xororz.localdream.data.GenerationDefaults
import io.github.xororz.localdream.data.GenerationPrefs
import io.github.xororz.localdream.data.Resolution
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunParameterStateTest {
    @Test fun firstOpenUsesModelDefaultsButPreservesLegacyFields() {
        val modelDefaults = GenerationDefaults(
            prompt = "special model prompt",
            negativePrompt = "special negative",
            steps = 7f,
            cfg = 2.5f,
            scheduler = "euler",
            denoiseStrength = 0.4f,
        )
        val persisted = GenerationPrefs(
            hasSaved = false,
            prompt = "old prompt",
            steps = 30f,
            seed = "123",
            batchCounts = 4,
            useOpenCL = true,
            aspectRatio = "3:2",
        )
        val restored = resolveModelRunSettings(
            prefs = persisted,
            defaults = modelDefaults,
            usesFixedCanvas = false,
            runOnCpu = true,
            isDit = false,
            useImg2img = true,
            isRemote = false,
        )
        assertEquals("special model prompt", restored.prompt)
        assertEquals("special negative", restored.negativePrompt)
        assertEquals(7f, restored.steps, 0.00001f)
        assertEquals(2.5f, restored.cfg, 0.00001f)
        assertEquals("euler", restored.scheduler)
        assertEquals("123", restored.seed)
        assertEquals(4, restored.batchCounts)
        assertTrue(restored.useOpenCL)
        assertEquals("3:2", restored.aspectRatio)
        assertEquals(256, restored.width)
        assertEquals(256, restored.height)
        assertTrue(restored.needsPersist)
    }

    @Test fun savedParametersTakePrecedenceAndDoNotWriteAgain() {
        val restored = resolveModelRunSettings(
            prefs = GenerationPrefs(
                hasSaved = true,
                prompt = "saved",
                negativePrompt = "saved negative",
                steps = 31f,
                cfg = 4f,
                width = 768,
                height = 512,
                scheduler = "saved-scheduler",
                denoiseStrength = 0.3f,
            ),
            defaults = GenerationDefaults(prompt = "different"),
            usesFixedCanvas = false,
            runOnCpu = false,
            isDit = false,
            useImg2img = true,
            isRemote = false,
        )
        assertEquals("saved", restored.prompt)
        assertEquals("saved negative", restored.negativePrompt)
        assertEquals(768, restored.width)
        assertEquals(512, restored.height)
        assertEquals(31f, restored.steps, 0.00001f)
        assertEquals("saved-scheduler", restored.scheduler)
        assertFalse(restored.needsPersist)
    }

    @Test fun fixedCanvasAndDisabledImg2imgFollowPreviousRules() {
        val restored = resolveModelRunSettings(
            prefs = GenerationPrefs(hasSaved = true, width = 512, height = 768, aspectRatio = "3:2"),
            defaults = GenerationDefaults(),
            usesFixedCanvas = true,
            runOnCpu = false,
            isDit = false,
            useImg2img = false,
            isRemote = false,
        )
        assertEquals(1024, restored.width)
        assertEquals(1024, restored.height)
        assertEquals("1:1", restored.aspectRatio)
        assertFalse(restored.needsPersist)
    }

    @Test fun remoteHostFallbackOnlyWhenItsPatchResolutionIsMissing() {
        val saved = GenerationPrefs(hasSaved = true, width = 768, height = 768)
        fun remote(available: List<Resolution>) = resolveModelRunSettings(
            prefs = saved,
            defaults = GenerationDefaults(),
            usesFixedCanvas = false,
            runOnCpu = false,
            isDit = false,
            useImg2img = true,
            isRemote = true,
            remoteResolutions = available,
        )
        assertEquals(512, remote(emptyList()).width)
        assertEquals(512, remote(emptyList()).height)
        assertEquals(768, remote(listOf(Resolution(768, 768))).width)
        assertEquals(768, remote(listOf(Resolution(768, 768))).height)
        assertFalse(remote(emptyList()).needsPersist)
    }

    @Test fun ditResolutionSnapsAndPersistsOnlyIfChanged() {
        val restored = resolveModelRunSettings(
            prefs = GenerationPrefs(hasSaved = true, width = 1023, height = 1023),
            defaults = GenerationDefaults(),
            usesFixedCanvas = false,
            runOnCpu = false,
            isDit = true,
            useImg2img = true,
            isRemote = false,
        )
        assertEquals(snapDitSize(1023f), restored.width)
        assertEquals(snapDitSize(1023f), restored.height)
        assertEquals(restored.width != 1023 || restored.height != 1023, restored.needsPersist)
    }

    @Test fun sizeAndBatchControlsPreserveOldClamps() {
        val state = ModelRunParameterState(512)
        state.setLegacySquareSize(138f)
        assertEquals(128, state.width)
        assertEquals(128, state.height)
        state.setLegacySquareSize(800f)
        assertEquals(512, state.width)
        assertEquals(512, state.height)

        state.setBatchCount(0f)
        assertEquals(1, state.batchCounts)
        state.setBatchCount(13f)
        assertEquals(10, state.batchCounts)

        state.setDitWidth(768f)
        assertEquals(snapDitSize(768f), state.width)
        assertEquals(inferAspectRatioString(state.width, state.height), state.aspectRatio)
        state.setDitHeight(896f)
        assertEquals(snapDitSize(896f), state.height)
        assertEquals(inferAspectRatioString(state.width, state.height), state.aspectRatio)
    }
}
