package io.github.xororz.localdream.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException

/**
 * Owns history selection and bulk-operation progress without touching Room or MediaStore.
 * The screen provides existing data access and image-export callbacks.
 */
internal class ModelRunHistoryBulkController {
    var isSelectionMode by mutableStateOf(false)
        private set
    val selectedIds = mutableStateListOf<Long>()

    var isSaving by mutableStateOf(false)
        private set
    var saveTotal by mutableIntStateOf(0)
        private set
    var saveCurrent by mutableIntStateOf(0)
        private set
    var saveFailed by mutableIntStateOf(0)
        private set

    fun startSelection(id: Long) {
        isSelectionMode = true
        selectedIds.clear()
        selectedIds.add(id)
    }

    fun toggle(id: Long) {
        if (id in selectedIds) {
            selectedIds.remove(id)
            if (selectedIds.isEmpty()) isSelectionMode = false
        } else {
            selectedIds.add(id)
        }
    }

    fun clearSelection() {
        selectedIds.clear()
        isSelectionMode = false
    }

    fun selectAll(ids: List<Long>) {
        selectedIds.clear()
        selectedIds.addAll(ids.distinct())
    }

    fun shouldClearSelectAll(totalCount: Int): Boolean =
        totalCount > 0 && selectedIds.size >= totalCount

    fun beginSave(total: Int) {
        check(!isSaving) { "Bulk save already in progress" }
        require(total > 0) { "Empty bulk save" }
        saveTotal = total
        saveCurrent = 0
        saveFailed = 0
        isSaving = true
    }

    /**
     * Exactly one progress update per selected ID, including missing records.
     * The result reports attempted exports, not the count returned by the database.
     */
    suspend fun <T> export(
        ids: List<Long>,
        resolve: suspend (List<Long>) -> List<T>,
        idOf: (T) -> Long,
        save: suspend (T) -> Boolean,
    ): HistoryBulkSummary {
        check(isSaving && ids.size == saveTotal)
        try {
            val items = resolve(ids)
            val byId = items.associateBy(idOf)
            for (id in ids) {
                val item = byId[id]
                val success = if (item != null) {
                    try {
                        save(item)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        false
                    }
                } else {
                    false
                }
                saveCurrent++
                if (!success) saveFailed++
            }
            return HistoryBulkSummary(saveTotal - saveFailed, saveFailed)
        } finally {
            isSaving = false
            clearSelection()
        }
    }

    suspend fun <T> delete(
        ids: List<Long>,
        resolve: suspend (List<Long>) -> List<T>,
        deleteResolved: suspend (List<T>) -> Int,
    ): HistoryBulkSummary {
        val resolved = resolve(ids)
        val deleted = deleteResolved(resolved).coerceIn(0, ids.size)
        clearSelection()
        return HistoryBulkSummary(deleted, ids.size - deleted)
    }
}

internal data class HistoryBulkSummary(val succeeded: Int, val failed: Int)
