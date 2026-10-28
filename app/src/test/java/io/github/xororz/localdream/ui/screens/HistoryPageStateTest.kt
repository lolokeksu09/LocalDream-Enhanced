package io.github.xororz.localdream.ui.screens

import androidx.paging.LoadState
import org.junit.Assert.assertEquals
import org.junit.Test

class HistoryPageStateTest {
    @Test fun initialLoadingErrorAndEmptyAreDistinct() {
        assertEquals(HistoryPageState.LOADING, historyPageState(0, LoadState.Loading))
        assertEquals(HistoryPageState.ERROR, historyPageState(0, LoadState.Error(IllegalStateException())))
        assertEquals(HistoryPageState.EMPTY, historyPageState(0, LoadState.NotLoading(false)))
    }

    @Test fun existingItemsRemainVisibleDuringRefreshAndRefreshFailure() {
        assertEquals(HistoryPageState.CONTENT, historyPageState(2, LoadState.Loading))
        assertEquals(HistoryPageState.CONTENT, historyPageState(2, LoadState.Error(IllegalStateException())))
    }
}
