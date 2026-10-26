package io.github.xororz.localdream.ui.screens

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * Snapshot-backed presentation state for a single model run screen.
 *
 * No service, repository, navigation or disk I/O belongs here. The screen
 * retains ownership of those side effects; this holder keeps visual state
 * transitions consistent across progress, completion, cancellation and errors.
 */
internal class ModelRunDisplayState(modelId: String) {
    var currentBitmap by mutableStateOf<Bitmap?>(null)
    var intermediateBitmap by mutableStateOf<Bitmap?>(null)
    var imageVersion by mutableIntStateOf(0)
    var generationParams by mutableStateOf<GenerationParameters?>(null)
    var generationParamsModelId by mutableStateOf(modelId)

    var returnedSeed by mutableStateOf<Long?>(null)
    var isRunning by mutableStateOf(false)
    var progress by mutableFloatStateOf(0f)
    var errorMessage by mutableStateOf<String?>(null)
    var generationStartTime by mutableStateOf<Long?>(null)

    fun onProgress(value: Float, intermediate: Bitmap?, nowMillis: Long) {
        if (generationStartTime == null) generationStartTime = nowMillis
        progress = value
        isRunning = true
        if (intermediate != null) intermediateBitmap = intermediate
    }

    /** Completion does not clear isRunning: a multi-image batch may continue. */
    fun onComplete(seed: Long?) {
        intermediateBitmap = null
        if (seed != null) returnedSeed = seed
        progress = 0f
    }

    fun onError(message: String) {
        intermediateBitmap = null
        errorMessage = message
        isRunning = false
        progress = 0f
        generationStartTime = null
    }

    fun onIdle() {
        isRunning = false
        progress = 0f
    }

    fun onInterrupt() {
        intermediateBitmap = null
        isRunning = false
        progress = 0f
        generationStartTime = null
    }

    fun onExit() {
        currentBitmap = null
        generationParams = null
        isRunning = false
        progress = 0f
        errorMessage = null
        generationStartTime = null
    }

    fun presentResult(bitmap: Bitmap, params: GenerationParameters, modelId: String) {
        currentBitmap = bitmap
        generationParams = params
        generationParamsModelId = modelId
        imageVersion++
    }

    fun elapsedLabel(nowMillis: Long): String? =
        generationStartTime?.let { formatGenerationDuration(nowMillis - it) }
}

/** Same visible timing as the previous run screen, independent of Android UI. */
internal fun formatGenerationDuration(elapsedMillis: Long): String {
    val duration = elapsedMillis
    return when {
        duration < 1_000L -> "${duration}ms"
        duration < 60_000L -> String.format(Locale.US, "%.1fs", duration / 1_000.0)
        else -> String.format(
            Locale.US,
            "%dm%ds",
            duration / 60_000L,
            (duration % 60_000L) / 1_000L,
        )
    }
}
