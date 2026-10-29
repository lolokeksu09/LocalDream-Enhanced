package io.github.xororz.localdream.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.R

/** Composable-only prompt editor. State ownership and persistence stay with the run screen. */
@Composable
internal fun ModelRunPromptFields(
    promptField: PromptFieldController,
    negativePromptField: PromptFieldController,
    autocompleteAvailable: Boolean,
    promptFormatChoice: PromptFormatChoice,
    effectivePromptFormat: PromptFormat,
    onPromptFormatChange: (PromptFormatChoice) -> Unit,
    showTemplates: Boolean,
    onShowTemplates: () -> Unit,
    onApplyTemplates: (String, String) -> Unit,
    onApplyQuickPrompt: (String, String) -> Unit,
    onDismissTemplates: () -> Unit,
) {
    var pendingQuickPrompt by remember { mutableStateOf<QuickPrompt?>(null) }
    var lastQuickChange by remember { mutableStateOf<QuickPromptUndo?>(null) }
    fun applyQuickPrompt(prompt: String, negative: String) {
        lastQuickChange = QuickPromptUndo(
            previous = QuickPrompt(promptField.text, negativePromptField.text),
            applied = QuickPrompt(prompt, negative),
        )
        onApplyQuickPrompt(prompt, negative)
    }

    val effectiveLabel = when (effectivePromptFormat) {
        PromptFormat.UNIVERSAL -> R.string.quick_prompt_format_universal
        PromptFormat.TAGS -> R.string.quick_prompt_format_tags
        PromptFormat.DESCRIPTION -> R.string.quick_prompt_format_description
    }
    ControlledPromptTagTextField(
        controller = promptField,
        autocompleteAvailable = autocompleteAvailable,
        modifier = Modifier.fillMaxWidth(),
        label = {
            PromptCountLabel(
                label = stringResource(R.string.image_prompt),
                count = promptField.tokenCount,
                max = promptField.tokenMax,
                showCount = promptField.text.isNotEmpty(),
            )
        },
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.quick_prompt_styles_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.quick_prompt_format_active, stringResource(effectiveLabel)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        var menuExpanded by remember { mutableStateOf(false) }
        Box {
            TextButton(onClick = { menuExpanded = true }) {
                Text(stringResource(when (promptFormatChoice) {
                    PromptFormatChoice.AUTO -> R.string.quick_prompt_format_auto
                    PromptFormatChoice.UNIVERSAL -> R.string.quick_prompt_format_universal
                    PromptFormatChoice.TAGS -> R.string.quick_prompt_format_tags
                    PromptFormatChoice.DESCRIPTION -> R.string.quick_prompt_format_description
                }))
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                listOf(
                    PromptFormatChoice.AUTO to R.string.quick_prompt_format_auto,
                    PromptFormatChoice.UNIVERSAL to R.string.quick_prompt_format_universal,
                    PromptFormatChoice.TAGS to R.string.quick_prompt_format_tags,
                    PromptFormatChoice.DESCRIPTION to R.string.quick_prompt_format_description,
                ).forEach { (choice, label) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            onPromptFormatChange(choice)
                            menuExpanded = false
                        },
                    )
                }
            }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            QuickPromptCategory.PORTRAIT to R.string.quick_preset_portrait,
            QuickPromptCategory.ANIME to R.string.quick_preset_anime,
            QuickPromptCategory.REALISM to R.string.quick_preset_realism,
        ).forEach { (category, label) ->
            FilterChip(
                onClick = {
                    val preset = nextQuickPrompt(category, promptField.text, effectivePromptFormat)
                    if (requiresQuickPromptConfirmation(promptField.text, negativePromptField.text)) {
                        pendingQuickPrompt = preset
                    } else {
                        applyQuickPrompt(preset.prompt, preset.negativePrompt)
                    }
                },
                selected = false,
                label = { Text(stringResource(label)) },
            )
        }
    }
    ControlledPromptTagTextField(
        controller = negativePromptField,
        autocompleteAvailable = autocompleteAvailable,
        modifier = Modifier.fillMaxWidth(),
        label = {
            PromptCountLabel(
                label = stringResource(R.string.negative_prompt),
                count = negativePromptField.tokenCount,
                max = negativePromptField.tokenMax,
                showCount = negativePromptField.text.isNotEmpty(),
            )
        },
    )
    lastQuickChange?.let { change ->
        if (change.canUndo(promptField.text, negativePromptField.text)) {
            TextButton(onClick = {
                onApplyQuickPrompt(change.previous.prompt, change.previous.negativePrompt)
                lastQuickChange = null
            }) {
                Text(stringResource(R.string.quick_prompt_undo))
            }
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
    ) {
        androidx.compose.material3.FilledTonalButton(onClick = onShowTemplates) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.width(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.prompt_templates))
        }
    }
    pendingQuickPrompt?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingQuickPrompt = null },
            title = { Text(stringResource(R.string.quick_prompt_confirm_title)) },
            text = { Text(stringResource(R.string.quick_prompt_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    applyQuickPrompt(
                        appendQuickPrompt(promptField.text, preset.prompt),
                        appendQuickPrompt(negativePromptField.text, preset.negativePrompt),
                    )
                    pendingQuickPrompt = null
                }) { Text(stringResource(R.string.quick_prompt_add)) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { pendingQuickPrompt = null }) {
                        Text(stringResource(R.string.quick_prompt_cancel))
                    }
                    TextButton(onClick = {
                        applyQuickPrompt(preset.prompt, preset.negativePrompt)
                        pendingQuickPrompt = null
                    }) { Text(stringResource(R.string.quick_prompt_replace)) }
                }
            },
        )
    }
    if (showTemplates) {
        PromptTemplatesDialog(
            currentPrompt = promptField.text,
            currentNegativePrompt = negativePromptField.text,
            onApply = onApplyTemplates,
            onDismiss = onDismissTemplates,
        )
    }
}

@Composable
private fun PromptCountLabel(label: String, count: Int, max: Int, showCount: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        if (showCount) {
            Spacer(Modifier.width(6.dp))
            Text(if (max == 0) "$count/∞" else "$count/$max")
        }
    }
}
