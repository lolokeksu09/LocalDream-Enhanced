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

internal enum class QuickSettingsPreset {
    FAST,
    QUALITY,
    DETAIL,
}

internal fun selectedQuickSettingsPreset(steps: Float, cfg: Float): QuickSettingsPreset? = when {
    steps.isCloseTo(8f) && cfg.isCloseTo(4.5f) -> QuickSettingsPreset.FAST
    steps.isCloseTo(20f) && cfg.isCloseTo(7f) -> QuickSettingsPreset.QUALITY
    steps.isCloseTo(30f) && cfg.isCloseTo(7.5f) -> QuickSettingsPreset.DETAIL
    else -> null
}

private fun Float.isCloseTo(expected: Float): Boolean = kotlin.math.abs(this - expected) < 0.01f

@Composable
internal fun GenerationQuickSettings(
    steps: Float,
    cfg: Float,
    onApply: (steps: Float, cfg: Float) -> Unit,
) {
    val selected = selectedQuickSettingsPreset(steps, cfg)
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
                selected = selected == QuickSettingsPreset.FAST,
                onClick = { onApply(8f, 4.5f) },
                label = { Text(stringResource(R.string.quick_settings_fast)) },
            )
            FilterChip(
                selected = selected == QuickSettingsPreset.QUALITY,
                onClick = { onApply(20f, 7f) },
                label = { Text(stringResource(R.string.quick_settings_quality)) },
            )
            FilterChip(
                selected = selected == QuickSettingsPreset.DETAIL,
                onClick = { onApply(30f, 7.5f) },
                label = { Text(stringResource(R.string.quick_settings_detail)) },
            )
        }
    }
}
