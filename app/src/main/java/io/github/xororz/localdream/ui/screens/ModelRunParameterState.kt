package io.github.xororz.localdream.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.xororz.localdream.data.GenerationDefaults
import io.github.xororz.localdream.data.GenerationPrefs
import io.github.xororz.localdream.data.Resolution
import kotlin.math.roundToInt

/**
 * Compose-owned parameters, independent of screen widgets, DataStore and service lifecycle.
 * The screen remains responsible for when parameter changes are persisted.
 */
internal class ModelRunParameterState(initialSize: Int) {
    var cfg by mutableFloatStateOf(GenerationDefaults.GLOBAL.cfg)
    var steps by mutableFloatStateOf(GenerationDefaults.GLOBAL.steps)
    var seed by mutableStateOf(GenerationDefaults.GLOBAL.seed)
    var denoiseStrength by mutableFloatStateOf(GenerationDefaults.GLOBAL.denoiseStrength)
    var useOpenCL by mutableStateOf(false)
    var batchCounts by mutableIntStateOf(GenerationDefaults.GLOBAL.batchCounts)
    var scheduler by mutableStateOf(GenerationDefaults.GLOBAL.scheduler)
    var aspectRatio by mutableStateOf(GenerationDefaults.GLOBAL.aspectRatio)
    var width by mutableIntStateOf(initialSize)
    var height by mutableIntStateOf(initialSize)

    fun setLegacySquareSize(value: Float) {
        val newSize = ((value / 64).roundToInt() * 64).coerceIn(128, 512)
        width = newSize
        height = newSize
    }

    fun setDitWidth(value: Float) {
        width = snapDitSize(value)
        aspectRatio = inferAspectRatioString(width, height)
    }

    fun setDitHeight(value: Float) {
        height = snapDitSize(value)
        aspectRatio = inferAspectRatioString(width, height)
    }

    fun setBatchCount(value: Float) {
        batchCounts = value.roundToInt().coerceIn(1, 10)
    }

    fun restore(settings: ResolvedModelRunSettings) {
        cfg = settings.cfg
        steps = settings.steps
        seed = settings.seed
        denoiseStrength = settings.denoiseStrength
        useOpenCL = settings.useOpenCL
        batchCounts = settings.batchCounts
        scheduler = settings.scheduler
        aspectRatio = settings.aspectRatio
        width = settings.width
        height = settings.height
    }
}

internal data class ResolvedModelRunSettings(
    val prompt: String,
    val negativePrompt: String,
    val cfg: Float,
    val steps: Float,
    val seed: String,
    val denoiseStrength: Float,
    val useOpenCL: Boolean,
    val batchCounts: Int,
    val scheduler: String,
    val aspectRatio: String,
    val width: Int,
    val height: Int,
    val needsPersist: Boolean,
)

/** One pure resolver for the model's persisted values, defaults and remote capabilities. */
internal fun resolveModelRunSettings(
    prefs: GenerationPrefs,
    defaults: GenerationDefaults,
    usesFixedCanvas: Boolean,
    runOnCpu: Boolean,
    isDit: Boolean,
    useImg2img: Boolean,
    isRemote: Boolean,
    remoteResolutions: List<Resolution> = emptyList(),
): ResolvedModelRunSettings {
    val firstRun = !prefs.hasSaved
    val baseSize = defaultGenerationSize(
        usesFixedCanvas = false,
        runOnCpu = runOnCpu,
        isDit = isDit,
    )
    fun restoredSize(savedSize: Int): Int = when {
        usesFixedCanvas -> 1024
        savedSize == -1 -> baseSize
        isDit -> snapDitSize(savedSize.toFloat())
        else -> savedSize
    }
    var width = restoredSize(prefs.width)
    var height = restoredSize(prefs.height)

    // Local settings can contain a patch resolution absent from a remote host.
    if (isRemote && !usesFixedCanvas && !runOnCpu && !isDit) {
        val available = listOf(Resolution(512, 512)) + remoteResolutions
        if (available.none { it.width == width && it.height == height }) {
            width = 512
            height = 512
        }
    }

    return ResolvedModelRunSettings(
        prompt = if (firstRun) defaults.prompt else prefs.prompt,
        negativePrompt = if (firstRun) defaults.negativePrompt else prefs.negativePrompt,
        cfg = if (firstRun) defaults.cfg else prefs.cfg,
        steps = if (firstRun) defaults.steps else prefs.steps,
        // Legacy semantics: seed, batch count and OpenCL always use stored values.
        seed = prefs.seed,
        denoiseStrength = if (firstRun) defaults.denoiseStrength else prefs.denoiseStrength,
        useOpenCL = prefs.useOpenCL,
        batchCounts = prefs.batchCounts,
        scheduler = if (firstRun) defaults.scheduler else prefs.scheduler,
        aspectRatio = if (useImg2img) prefs.aspectRatio else "1:1",
        width = width,
        height = height,
        needsPersist = firstRun ||
            (isDit && (prefs.width != width || prefs.height != height)),
    )
}
