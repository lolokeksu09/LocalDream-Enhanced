package io.github.xororz.localdream.ui.screens

import android.content.Context
import android.content.Intent
import io.github.xororz.localdream.service.BackgroundGenerationService
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Immutable snapshot of one native-generation request.
 * Keeps the service's exact extra names and value types in one place.
 */
internal data class ModelRunRequest(
    val prompt: String,
    val negativePrompt: String,
    val steps: Int,
    val cfg: Float,
    val seed: String,
    val width: Int,
    val height: Int,
    val effectiveWidth: Int,
    val effectiveHeight: Int,
    val denoiseStrength: Float,
    val useOpenCL: Boolean,
    val scheduler: String,
    val aspectRatio: String,
    val runOnCpu: Boolean,
    val backendHost: String?,
    val batchIndex: Int? = null,
    val hasReferenceImages: Boolean = false,
    val hasImage: Boolean = false,
    val hasMask: Boolean = false,
    val ultrafixTileSize: Int? = null,
) {
    val isUltrafix: Boolean get() = ultrafixTileSize != null

    /** A pure representation so JVM tests cover the Android service contract. */
    fun extras(): Map<String, Any> = buildMap {
        put("prompt", prompt)
        put("negative_prompt", negativePrompt)
        put("steps", steps)
        put("cfg", cfg)
        seed.toLongOrNull()?.let { put("seed", it) }
        put("width", width)
        put("height", height)
        put("effective_width", effectiveWidth)
        put("effective_height", effectiveHeight)
        put("denoise_strength", denoiseStrength)
        put("scheduler", scheduler)
        backendHost?.let { put("backend_host", it) }
        if (isUltrafix) {
            put("ultrafix", true)
            put("ultrafix_tile_size", checkNotNull(ultrafixTileSize))
        } else {
            put("use_opencl", useOpenCL)
            put("aspect_ratio", aspectRatio)
            batchIndex?.let { put("batch_index", it) }
            if (hasReferenceImages) put("has_reference_images", true)
            if (hasImage) {
                put("has_image", true)
                if (hasMask) put("has_mask", true)
            }
        }
    }

    fun serviceIntent(context: Context): Intent =
        Intent(context, BackgroundGenerationService::class.java).apply {
            for ((name, value) in extras()) {
                when (value) {
                    is String -> putExtra(name, value)
                    is Int -> putExtra(name, value)
                    is Long -> putExtra(name, value)
                    is Float -> putExtra(name, value)
                    is Boolean -> putExtra(name, value)
                    else -> error("Unsupported generation extra: $name")
                }
            }
        }

    fun historyParameters(): GenerationParameters = GenerationParameters(
        steps = steps,
        cfg = cfg,
        seed = 0,
        prompt = prompt,
        negativePrompt = negativePrompt,
        generationTime = "",
        width = width,
        height = height,
        runOnCpu = runOnCpu,
        denoiseStrength = denoiseStrength,
        useOpenCL = useOpenCL,
        scheduler = scheduler,
    )
}

/** Reference images are written once per batch, before the first service start. */
internal suspend fun saveModelRunReferences(directory: File, images: List<String>) {
    if (images.isEmpty()) return
    withContext(Dispatchers.IO) {
        val array = JSONArray()
        images.forEach { array.put(it) }
        File(directory, "dit_references.json").writeText(array.toString())
    }
}
