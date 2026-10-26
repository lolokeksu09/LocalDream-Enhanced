package io.github.xororz.localdream.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.xororz.localdream.data.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Result-page selection state, independent from persistence and navigation.
 *
 * Stitch eligibility tracks specific saved history rows, not Bitmap identity:
 * tapping an older thumbnail must never mark an unrelated result stitchable.
 */
internal class ModelRunHistorySelection {
    var displayedHistoryId by mutableStateOf<Long?>(null)
        private set

    private var stitchableIds by mutableStateOf<Set<Long>>(emptySet())

    val isDisplayedStitchable: Boolean
        get() = displayedHistoryId?.let { it in stitchableIds } ?: false

    /** Called after a new bitmap replaces the result, before its save completes. */
    fun beginGeneratedResult() {
        stitchableIds = emptySet()
        displayedHistoryId = null
    }

    /** The save callback is responsible only for marking the completed item. */
    fun onGenerationSaved(id: Long, wasUltrafix: Boolean) {
        if (!wasUltrafix) stitchableIds = setOf(id)
        displayedHistoryId = id
    }

    /** Imported UltraFix inputs are not saved history rows. */
    fun onImportedImage() = beginGeneratedResult()

    /** History thumbnails keep eligibility only when their IDs match. */
    fun showHistoryItem(id: Long) {
        displayedHistoryId = id
    }

    /** An upscale inherits stitching eligibility only from an eligible source. */
    fun onUpscaleSaved(id: Long, sourceWasStitchable: Boolean) {
        displayedHistoryId = id
        if (sourceWasStitchable) stitchableIds = stitchableIds + id
    }

    /** Never mutate favorite state optimistically; Room is the source of truth. */
    fun requestedFavoriteChange(observedFavorite: Boolean?): Pair<Long, Boolean>? {
        val id = displayedHistoryId ?: return null
        val favorite = observedFavorite ?: return null
        return id to !favorite
    }

    fun displayHistoryResult(
        item: HistoryItem,
        bitmap: Bitmap,
        displayState: ModelRunDisplayState,
    ) {
        displayState.presentResult(bitmap, item.params, item.modelId)
        showHistoryItem(item.id)
    }
}

/** IO-only decode, deliberately kept separate from snapshot state and the Room DB. */
internal suspend fun decodeModelRunHistoryBitmap(item: HistoryItem): Bitmap? =
    withContext(Dispatchers.IO) { BitmapFactory.decodeFile(item.imageFile.absolutePath) }
