package io.github.xororz.localdream.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.R

@Composable
internal fun GenerationQuickSettings(
    steps: Float,
    cfg: Float,
    onApply: (steps: Float, cfg: Float) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.quick_settings_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = steps <= 10f && cfg <= 5f,
                onClick = { onApply(8f, 4.5f) },
                label = { Text(stringResource(R.string.quick_settings_fast)) },
            )
            FilterChip(
                selected = steps in 15f..25f && cfg in 6f..8f,
                onClick = { onApply(20f, 7f) },
                label = { Text(stringResource(R.string.quick_settings_quality)) },
            )
            FilterChip(
                selected = steps >= 26f && cfg >= 7f,
                onClick = { onApply(30f, 7.5f) },
                label = { Text(stringResource(R.string.quick_settings_detail)) },
            )
        }
    }
}
