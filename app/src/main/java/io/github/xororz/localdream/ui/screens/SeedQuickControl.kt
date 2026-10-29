package io.github.xororz.localdream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.R
import kotlin.random.Random

internal enum class SeedMode { RANDOM, FIXED, INVALID }

internal fun seedMode(seed: String): SeedMode = when {
    seed.isBlank() -> SeedMode.RANDOM
    seed.toLongOrNull()?.let { it >= 0L } == true -> SeedMode.FIXED
    else -> SeedMode.INVALID
}

/** Reuse the displayed result when possible; generate a valid seed only on demand. */
internal fun seedForLock(displayedSeed: Long?, generate: () -> Long): String =
    (displayedSeed?.takeIf { it >= 0L } ?: generate()).toString()

@Composable
internal fun SeedQuickControl(seed: String, displayedSeed: Long?, onSeedChange: (String) -> Unit) {
    val mode = seedMode(seed)
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.seed_quick_title), style = MaterialTheme.typography.labelLarge)
        when (mode) {
            SeedMode.RANDOM -> {
                Text(
                    stringResource(if (displayedSeed != null && displayedSeed >= 0L) R.string.seed_random_last_hint else R.string.seed_random_new_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = {
                    onSeedChange(seedForLock(displayedSeed) { Random.nextInt(Int.MAX_VALUE).toLong() })
                }) {
                    Text(stringResource(R.string.seed_lock))
                }
            }
            SeedMode.FIXED -> {
                Text(
                    stringResource(R.string.seed_fixed_value, seed),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.seed_fixed_batch_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = { onSeedChange("") }) {
                    Text(stringResource(R.string.seed_make_random))
                }
            }
            SeedMode.INVALID -> {
                Text(stringResource(R.string.seed_invalid), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { onSeedChange("") }) {
                    Text(stringResource(R.string.seed_clear))
                }
            }
        }
    }
}
