package io.github.xororz.localdream.ui.screens

import io.github.xororz.localdream.data.GenerationPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Immutable persistence inputs. All values are captured on the UI thread,
 * rather than reading mutable Compose state later from a background thread.
 */
internal data class ModelRunSettingsSnapshot(
    val modelId: String,
    val prompt: String,
    val negativePrompt: String,
    val steps: Float,
    val cfg: Float,
    val seed: String,
    val width: Int,
    val height: Int,
    val denoiseStrength: Float,
    val useOpenCL: Boolean,
    val batchCounts: Int,
    val scheduler: String,
    val aspectRatio: String,
) {
    suspend fun write(preferences: GenerationPreferences) {
        preferences.saveAllFields(
            modelId = modelId,
            prompt = prompt,
            negativePrompt = negativePrompt,
            steps = steps,
            cfg = cfg,
            seed = seed,
            width = width,
            height = height,
            denoiseStrength = denoiseStrength,
            useOpenCL = useOpenCL,
            batchCounts = batchCounts,
            scheduler = scheduler,
            aspectRatio = aspectRatio,
        )
    }
}

internal data class ModelRunUltrafixSnapshot(
    val modelId: String,
    val steps: Float,
    val denoiseSteps: Int,
    val qualityDenoise: Boolean,
) {
    suspend fun write(preferences: GenerationPreferences) {
        preferences.saveUltrafixParams(modelId, steps, denoiseSteps, qualityDenoise)
    }
}

/**
 * Independent, latest-wins debounce lanes for normal and UltraFix settings.
 * The reset action bypasses debounce and cancels stale pending writes.
 * DataStore already serializes writes, no new persistence format is involved.
 */
internal class ModelRunSettingsCoordinator(
    private val scope: CoroutineScope,
    private val saveMain: suspend (ModelRunSettingsSnapshot) -> Unit,
    private val saveUltrafix: suspend (ModelRunUltrafixSnapshot) -> Unit,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val mainDelayMillis: Long = 1_000L,
    private val ultrafixDelayMillis: Long = 500L,
) {
    private var mainJob: Job? = null
    private var ultrafixJob: Job? = null

    fun scheduleMain(snapshot: ModelRunSettingsSnapshot) {
        mainJob?.cancel()
        mainJob = scope.launch(dispatcher) {
            delay(mainDelayMillis)
            saveMain(snapshot)
        }
    }

    fun scheduleUltrafix(snapshot: ModelRunUltrafixSnapshot) {
        ultrafixJob?.cancel()
        ultrafixJob = scope.launch(dispatcher) {
            delay(ultrafixDelayMillis)
            saveUltrafix(snapshot)
        }
    }

    suspend fun writeMainNow(snapshot: ModelRunSettingsSnapshot) {
        mainJob?.cancelAndJoin()
        mainJob = null
        withContext(dispatcher) { saveMain(snapshot) }
    }

    fun cancel() {
        mainJob?.cancel()
        mainJob = null
        ultrafixJob?.cancel()
        ultrafixJob = null
    }
}
