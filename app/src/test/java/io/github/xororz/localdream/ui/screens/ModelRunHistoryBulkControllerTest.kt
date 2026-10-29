package io.github.xororz.localdream.ui.screens

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunHistoryBulkControllerTest {
    @Test fun selectionIsDeduplicatedAndExitsWhenEmpty() {
        val controller = ModelRunHistoryBulkController()
        controller.startSelection(3L)
        assertTrue(controller.isSelectionMode)
        controller.toggle(4L)
        assertEquals(listOf(3L, 4L), controller.selectedIds.toList())
        controller.toggle(3L)
        controller.toggle(4L)
        assertFalse(controller.isSelectionMode)
        assertTrue(controller.selectedIds.isEmpty())
        controller.startSelection(1L)
        controller.selectAll(listOf(4L, 4L, 2L))
        assertEquals(listOf(4L, 2L), controller.selectedIds.toList())
        assertTrue(controller.shouldClearSelectAll(2))
        controller.clearSelection()
        assertFalse(controller.isSelectionMode)
    }

    @Test fun exportAccountsForMissingFilesAndFailedWrites() = runBlocking {
        val controller = ModelRunHistoryBulkController()
        controller.startSelection(1L)
        controller.selectAll(listOf(1L, 2L, 3L, 4L))
        controller.beginSave(4)
        val attempted = mutableListOf<Long>()
        val result = controller.export(
            ids = listOf(1L, 2L, 3L, 4L),
            resolve = { listOf(1L, 3L, 4L) },
            idOf = { it },
            save = { id ->
                attempted += id
                if (id == 3L) error("MediaStore failed")
                id == 1L
            },
        )
        assertEquals(listOf(1L, 3L, 4L), attempted)
        assertEquals(HistoryBulkSummary(1, 3), result)
        assertEquals(4, controller.saveCurrent)
        assertEquals(3, controller.saveFailed)
        assertFalse(controller.isSaving)
        assertTrue(controller.selectedIds.isEmpty())
    }

    @Test fun deletingUsesTheExistingHistoryManagerTransaction() = runBlocking {
        val controller = ModelRunHistoryBulkController()
        controller.startSelection(8L)
        val result = controller.delete(
            ids = listOf(8L, 9L, 10L),
            resolve = { ids -> ids.filterNot { it == 9L } },
            deleteResolved = { items ->
                assertEquals(listOf(8L, 10L), items)
                2
            },
        )
        assertEquals(HistoryBulkSummary(2, 1), result)
        assertFalse(controller.isSelectionMode)
    }

    @Test fun cancelledExportCleansUpProgressWithoutSavingMoreItems() = runBlocking {
        val controller = ModelRunHistoryBulkController()
        controller.startSelection(1L)
        controller.beginSave(2)
        val started = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val visited = mutableListOf<Long>()
        val job = launch {
            controller.export(
                ids = listOf(1L, 2L),
                resolve = { listOf(1L, 2L) },
                idOf = { it },
                save = { id ->
                    visited += id
                    started.complete(Unit)
                    gate.await()
                    true
                },
            )
        }
        started.await()
        job.cancelAndJoin()
        assertEquals(listOf(1L), visited)
        assertFalse(controller.isSaving)
        assertFalse(controller.isSelectionMode)
    }
}
