package io.github.xororz.localdream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import io.github.xororz.localdream.R
import io.github.xororz.localdream.data.HistoryItem
import io.github.xororz.localdream.data.HistoryManager

/** Loads both selected records in selection order, including records outside loaded pages. */
@Composable
internal fun HistoryComparisonDialog(
    historyManager: HistoryManager,
    selectedIds: List<Long>,
    currentModelId: String?,
    onSelectResult: (HistoryItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val items by produceState<List<HistoryItem>?>(initialValue = null, historyManager, selectedIds) {
        value = if (selectedIds.size == 2 && selectedIds.distinct().size == 2) {
            historyManager.getItems(selectedIds)
        } else {
            emptyList()
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 24.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.history_compare_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
                }
                val loaded = items
                when {
                    loaded == null -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    loaded.size != 2 -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.history_compare_load_failed))
                    }
                    else -> {
                        val first = loaded[0]
                        val second = loaded[1]
                        Column(
                            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CompareImage(first, stringResource(R.string.history_compare_first), currentModelId, { onSelectResult(first) }, Modifier.weight(1f))
                                CompareImage(second, stringResource(R.string.history_compare_second), currentModelId, { onSelectResult(second) }, Modifier.weight(1f))
                            }
                            Text(stringResource(R.string.history_compare_hint), style = MaterialTheme.typography.bodySmall)
                            CompareRow(stringResource(R.string.history_compare_model), first.modelId, second.modelId)
                            CompareRow(stringResource(R.string.history_compare_prompt), first.params.prompt, second.params.prompt)
                            CompareRow(stringResource(R.string.history_compare_negative), first.params.negativePrompt, second.params.negativePrompt)
                            CompareRow(stringResource(R.string.history_compare_steps), first.params.steps.toString(), second.params.steps.toString())
                            CompareRow("CFG", first.params.cfg.toString(), second.params.cfg.toString())
                            CompareRow("Seed", first.params.seed?.toString(), second.params.seed?.toString())
                            CompareRow(stringResource(R.string.history_compare_size), "${first.params.width}×${first.params.height}", "${second.params.width}×${second.params.height}")
                            CompareRow(stringResource(R.string.history_compare_scheduler), first.params.scheduler, second.params.scheduler)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompareImage(item: HistoryItem, label: String, currentModelId: String?, onSelect: () -> Unit, modifier: Modifier = Modifier) {
    var failed by remember(item.imageFile) { mutableStateOf(false) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(
            modifier = Modifier.fillMaxWidth().height(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (failed) {
                Text(stringResource(R.string.history_compare_image_missing), style = MaterialTheme.typography.bodySmall)
            } else {
                AsyncImage(
                    model = item.imageFile,
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    onError = { failed = true },
                )
            }
        }
        val canReuse = canReuseComparedSettings(currentModelId, item.modelId)
        val actionText = stringResource(if (canReuse) R.string.history_compare_use_settings else R.string.history_compare_view_parameters)
        val actionDescription = stringResource(
            if (canReuse) R.string.history_compare_use_description else R.string.history_compare_view_description,
            label,
        )
        TextButton(
            onClick = onSelect,
            modifier = Modifier.fillMaxWidth().semantics { contentDescription = actionDescription },
        ) {
            Text(actionText, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CompareRow(label: String, first: String?, second: String?) {
    val status = when {
        first == null || second == null -> R.string.history_compare_unknown
        first == second -> R.string.history_compare_same
        else -> R.string.history_compare_different
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            Text(stringResource(status), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val unavailable = stringResource(R.string.history_compare_unavailable)
            val empty = stringResource(R.string.history_compare_empty)
            Text(first?.ifBlank { empty } ?: unavailable, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(second?.ifBlank { empty } ?: unavailable, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        }
    }
}
