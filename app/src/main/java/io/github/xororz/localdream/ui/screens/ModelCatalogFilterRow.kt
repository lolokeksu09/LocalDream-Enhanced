package io.github.xororz.localdream.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.R
import io.github.xororz.localdream.data.ModelCatalogFilter

@Composable
internal fun ModelCatalogFilterRow(
    selected: ModelCatalogFilter,
    onSelected: (ModelCatalogFilter) -> Unit,
) {
    val items = listOf(
        ModelCatalogFilter.ALL to R.string.catalog_filter_all,
        ModelCatalogFilter.INSTALLED to R.string.catalog_filter_installed,
        ModelCatalogFilter.AVAILABLE to R.string.catalog_filter_available,
        ModelCatalogFilter.PINNED to R.string.catalog_filter_pinned,
    )
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { (filter, title) ->
            FilterChip(
                selected = filter == selected,
                onClick = { onSelected(filter) },
                label = { Text(stringResource(title)) },
            )
        }
    }
}
