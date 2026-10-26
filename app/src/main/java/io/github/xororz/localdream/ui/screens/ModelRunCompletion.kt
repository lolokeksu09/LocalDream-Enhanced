package io.github.xororz.localdream.ui.screens

import android.graphics.Bitmap
import io.github.xororz.localdream.data.GenerationMode
import io.github.xororz.localdream.data.HistoryItem
import io.github.xororz.localdream.data.HistoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Converts the request snapshot into the metadata of the actual result.
 *
 * The returned bitmap dimensions are authoritative because fixed-canvas
 * backends can crop the requested canvas before returning an image.
 */
internal fun completeModelRunParameters(
    requested: GenerationParameters,
    resultSeed: Long?,
    generationTime: String,
    resultWidth: Int,
    resultHeight: Int,
    runOnCpu: Boolean,
    mode: GenerationMode,
): GenerationParameters = requested.copy(
    seed = resultSeed,
    generationTime = generationTime,
    width = resultWidth,
    height = resultHeight,
    runOnCpu = runOnCpu,
    mode = mode,
)

/** Persists a completed result off the main thread and returns its history row. */
internal suspend fun saveModelRunCompletion(
    historyManager: HistoryManager,
    modelId: String,
    bitmap: Bitmap,
    params: GenerationParameters,
    mode: GenerationMode,
): HistoryItem? = withContext(Dispatchers.IO) {
    historyManager.saveGeneratedImage(
        modelId = modelId,
        bitmap = bitmap,
        params = params,
        mode = mode,
    )
}
