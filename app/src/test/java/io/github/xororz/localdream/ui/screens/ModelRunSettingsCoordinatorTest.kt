package io.github.xororz.localdream.ui.screens

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunSettingsCoordinatorTest {
    private fun main(seed: String) = ModelRunSettingsSnapshot(
        modelId = "model-a",
        prompt = "test prompt",
        negativePrompt = "negative",
        steps = 20f,
        cfg = 7f,
        seed = seed,
        width = 512,
        height = 512,
        denoiseStrength = 0.6f,
        useOpenCL = false,
        batchCounts = 1,
        scheduler = "dpm",
        aspectRatio = "1:1",
    )

    private fun ultra(steps: Float) =
        ModelRunUltrafixSnapshot("model-a", steps, 4, true)

    @Test fun lastMainSnapshotWinsRapidEdits() = runBlocking {
        val saved = mutableListOf<ModelRunSettingsSnapshot>()
        val coordinator = ModelRunSettingsCoordinator(
            scope = this,
            saveMain = { saved.add(it) },
            saveUltrafix = {},
            dispatcher = Dispatchers.Unconfined,
            mainDelayMillis = 25L,
        )
        coordinator.scheduleMain(main("old"))
        coordinator.scheduleMain(main("new"))
        delay(65L)
        assertEquals(listOf("new"), saved.map { it.seed })
        assertEquals("test prompt", saved.single().prompt)
    }

    @Test fun ultrafixAndMainHaveIndependentDebounceLanes() = runBlocking {
        val savedMain = mutableListOf<String>()
        val savedUltrafix = mutableListOf<Float>()
        val coordinator = ModelRunSettingsCoordinator(
            scope = this,
            saveMain = { savedMain.add(it.seed) },
            saveUltrafix = { savedUltrafix.add(it.steps) },
            dispatcher = Dispatchers.Unconfined,
            mainDelayMillis = 35L,
            ultrafixDelayMillis = 20L,
        )
        coordinator.scheduleMain(main("one"))
        coordinator.scheduleUltrafix(ultra(10f))
        coordinator.scheduleMain(main("two"))
        coordinator.scheduleUltrafix(ultra(12f))
        delay(85L)
        assertEquals(listOf("two"), savedMain)
        assertEquals(listOf(12f), savedUltrafix)
    }

    @Test fun resetWritesImmediatelyAndDiscardsPendingStaleValues() = runBlocking {
        val saved = mutableListOf<String>()
        val coordinator = ModelRunSettingsCoordinator(
            scope = this,
            saveMain = { saved.add(it.seed) },
            saveUltrafix = {},
            dispatcher = Dispatchers.Unconfined,
            mainDelayMillis = 90L,
        )
        coordinator.scheduleMain(main("stale"))
        coordinator.writeMainNow(main("reset"))
        delay(110L)
        assertEquals(listOf("reset"), saved)
        coordinator.scheduleMain(main("cancelled"))
        coordinator.cancel()
        delay(110L)
        assertEquals(listOf("reset"), saved)
        assertTrue(saved.isNotEmpty())
    }
}
