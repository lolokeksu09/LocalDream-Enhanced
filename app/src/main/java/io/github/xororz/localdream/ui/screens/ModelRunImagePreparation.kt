package io.github.xororz.localdream.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.ImageDecoder
import android.graphics.Rect
import android.net.Uri
import android.util.Log
import androidx.core.graphics.scale
import java.io.File
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class PreparedModelRunCrop(
    val bitmap: Bitmap,
    val cropRect: Rect,
    val payload: String,
)

internal data class PreparedModelRunImage(
    val bitmap: Bitmap,
    val payload: String,
)

internal data class ModelRunCropArea(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int,
)

/** Pure center-crop geometry; preserves the previous img2img rounding. */
internal fun calculateModelRunCenterCrop(
    sourceWidth: Int,
    sourceHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
): ModelRunCropArea {
    require(sourceWidth > 0 && sourceHeight > 0 && targetWidth > 0 && targetHeight > 0)
    val srcRatio = sourceWidth.toFloat() / sourceHeight.toFloat()
    val dstRatio = targetWidth.toFloat() / targetHeight.toFloat()
    if (kotlin.math.abs(srcRatio - dstRatio) < 1e-3f) {
        return ModelRunCropArea(0, 0, sourceWidth, sourceHeight)
    }
    val (cropW, cropH) = if (srcRatio > dstRatio) {
        Pair((sourceHeight * dstRatio).toInt(), sourceHeight)
    } else {
        Pair(sourceWidth, (sourceWidth / dstRatio).toInt())
    }
    return ModelRunCropArea(
        left = (sourceWidth - cropW) / 2,
        top = (sourceHeight - cropH) / 2,
        width = cropW,
        height = cropH,
    )
}

/** Decode the precise original crop region to prevent stitching offsets. */
internal suspend fun prepareModelRunCrop(
    context: Context,
    originalUri: Uri?,
    fallbackBitmap: Bitmap,
    requestedRect: Rect,
    usesFixedCanvas: Boolean,
    aspectRatio: String,
    canvasWidth: Int,
    canvasHeight: Int,
): PreparedModelRunCrop = withContext(Dispatchers.IO) {
    val target = computeAspectTargetSize(usesFixedCanvas, aspectRatio)
    val targetW = target?.first ?: canvasWidth
    val targetH = target?.second ?: canvasHeight
    var clampedRect = requestedRect
    val freshCropped: Bitmap? = try {
        originalUri?.let { uri ->
            context.contentResolver.openInputStream(uri)?.use { input ->
                @Suppress("DEPRECATION")
                val decoder = BitmapRegionDecoder.newInstance(input, false)
                    ?: throw IllegalStateException("BitmapRegionDecoder.newInstance returned null")
                try {
                    val left = requestedRect.left.coerceAtLeast(0)
                    val top = requestedRect.top.coerceAtLeast(0)
                    val right = requestedRect.right.coerceAtMost(decoder.width)
                    val bottom = requestedRect.bottom.coerceAtMost(decoder.height)
                    if (right > left && bottom > top) {
                        val region = snapInpaintCropRect(
                            rect = Rect(left, top, right, bottom),
                            imageWidth = decoder.width,
                            imageHeight = decoder.height,
                            tolerance = (2f * context.resources.displayMetrics.density)
                                .roundToInt().coerceAtLeast(1),
                        )
                        clampedRect = region
                        decoder.decodeRegion(region, BitmapFactory.Options())
                    } else {
                        null
                    }
                } finally {
                    decoder.recycle()
                }
            }
        }
    } catch (cancelled: kotlinx.coroutines.CancellationException) {
        throw cancelled
    } catch (e: Exception) {
        Log.w("ModelRunScreen", "BitmapRegionDecoder failed, fall back to cropify bitmap: " + e.message)
        null
    }

    val source = freshCropped ?: fallbackBitmap
    val scaled = withContext(Dispatchers.Default) {
        if (source.width != targetW || source.height != targetH) {
            source.scale(targetW, targetH)
        } else {
            source
        }
    }
    val payload = encodeModelRunCanvasPng(scaled, canvasWidth, canvasHeight)
    PreparedModelRunCrop(scaled, clampedRect, payload)
}

/** Center-crops a result image and independently encodes the backend upload. */
internal suspend fun prepareModelRunBitmapUpload(
    bitmap: Bitmap,
    targetWidth: Int,
    targetHeight: Int,
    canvasWidth: Int,
    canvasHeight: Int,
): PreparedModelRunImage {
    val resized = withContext(Dispatchers.Default) {
        val area = calculateModelRunCenterCrop(
            bitmap.width, bitmap.height, targetWidth, targetHeight,
        )
        val cropped = if (area.width == bitmap.width && area.height == bitmap.height) {
            bitmap
        } else {
            Bitmap.createBitmap(bitmap, area.left, area.top, area.width, area.height)
        }
        if (cropped.width != targetWidth || cropped.height != targetHeight) {
            cropped.scale(targetWidth, targetHeight)
        } else {
            cropped.copy(Bitmap.Config.ARGB_8888, false)
        }
    }
    val payload = withContext(Dispatchers.IO) {
        encodeModelRunCanvasPng(resized, canvasWidth, canvasHeight)
    }
    return PreparedModelRunImage(resized, payload)
}

/** Mask and base image must align on the same full-size generation canvas. */
internal fun prepareModelRunMaskPayload(
    mask: Bitmap,
    originalBase64: String,
    canvasWidth: Int,
    canvasHeight: Int,
): String = if (mask.width == canvasWidth && mask.height == canvasHeight) {
    originalBase64
} else {
    encodeModelRunCanvasPng(mask, canvasWidth, canvasHeight)
}

private fun encodeModelRunCanvasPng(
    bitmap: Bitmap,
    canvasWidth: Int,
    canvasHeight: Int,
): String = if (bitmap.width == canvasWidth && bitmap.height == canvasHeight) {
    bitmapToBase64Png(bitmap)
} else {
    bitmapToBase64Png(padBitmapToCanvas(bitmap, canvasWidth, canvasHeight))
}

/** The native service still expects tmp.txt and mask.txt in private storage. */
internal suspend fun saveModelRunImagePayload(
    directory: File,
    fileName: String,
    payload: String,
) {
    require(fileName == "tmp.txt" || fileName == "mask.txt")
    withContext(Dispatchers.IO) {
        File(directory, fileName).writeText(payload)
    }
}

/**
 * Returns null for an image that fits the 1 MP budget, otherwise the same
 * rounded downscale dimensions previously passed to ImageDecoder.
 */
internal fun modelRunReferenceTargetSize(
    width: Int,
    height: Int,
    maxPixels: Long = 1024L * 1024L,
): Pair<Int, Int>? {
    require(width > 0 && height > 0 && maxPixels > 0L)
    val pixels = width.toLong() * height
    if (pixels <= maxPixels) return null
    val scale = sqrt(maxPixels.toDouble() / pixels)
    return Pair(
        (width * scale).roundToInt().coerceAtLeast(1),
        (height * scale).roundToInt().coerceAtLeast(1),
    )
}

/** Native-edit references retain the existing software decoding and 1MP limit. */
internal suspend fun decodeModelRunEditImage(
    context: Context,
    uri: Uri,
): PreparedModelRunImage = withContext(Dispatchers.IO) {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        val dimensions = modelRunReferenceTargetSize(info.size.width, info.size.height)
        if (dimensions != null) {
            decoder.setTargetSize(dimensions.first, dimensions.second)
        }
    }
    PreparedModelRunImage(bitmap, bitmapToBase64Jpeg(bitmap, 95))
}
