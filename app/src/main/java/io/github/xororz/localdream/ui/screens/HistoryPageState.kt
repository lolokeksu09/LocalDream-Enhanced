package io.github.xororz.localdream.ui.screens

import androidx.paging.LoadState

internal enum class HistoryPageState { LOADING, ERROR, EMPTY, CONTENT }

internal fun historyPageState(itemCount: Int, refresh: LoadState): HistoryPageState = when {
    itemCount > 0 -> HistoryPageState.CONTENT
    refresh is LoadState.Loading -> HistoryPageState.LOADING
    refresh is LoadState.Error -> HistoryPageState.ERROR
    else -> HistoryPageState.EMPTY
}
