package io.github.xororz.localdream.ui.screens

/**
 * Immutable values shared by every request in one generation batch.
 * Keeping this snapshot outside the Composable callback prevents later UI
 * changes from altering a request that is already being submitted.
 */
internal data class ModelRunBatchRequestInputs(
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
    val hasImage: Boolean,
    val hasMask: Boolean,
)

internal fun buildModelRunBatchRequest(
    inputs: ModelRunBatchRequestInputs,
    index: Int,
    hasReferences: Boolean,
): ModelRunRequest = ModelRunRequest(
    prompt = inputs.prompt,
    negativePrompt = inputs.negativePrompt,
    steps = inputs.steps,
    cfg = inputs.cfg,
    seed = inputs.seed,
    width = inputs.width,
    height = inputs.height,
    effectiveWidth = inputs.effectiveWidth,
    effectiveHeight = inputs.effectiveHeight,
    denoiseStrength = inputs.denoiseStrength,
    useOpenCL = inputs.useOpenCL,
    scheduler = inputs.scheduler,
    aspectRatio = inputs.aspectRatio,
    runOnCpu = inputs.runOnCpu,
    backendHost = inputs.backendHost,
    batchIndex = index,
    hasReferenceImages = hasReferences,
    hasImage = inputs.hasImage,
    hasMask = inputs.hasMask,
)
