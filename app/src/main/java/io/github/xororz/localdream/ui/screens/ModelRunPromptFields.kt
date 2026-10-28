package io.github.xororz.localdream.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    onDismissTemplates: () -> Unit,
) {
    Text(
        text = stringResource(R.string.prompt_settings),
        style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
        color = androidx.compose.material3.MaterialTheme.colorScheme.primary,
    )
    Text(
        text = stringResource(R.string.quick_prompt_format_title),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
    )
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            PromptFormatChoice.AUTO to R.string.quick_prompt_format_auto,
            PromptFormatChoice.UNIVERSAL to R.string.quick_prompt_format_universal,
            PromptFormatChoice.TAGS to R.string.quick_prompt_format_tags,
            PromptFormatChoice.DESCRIPTION to R.string.quick_prompt_format_description,
        ).forEach { (choice, label) ->
            FilterChip(
                selected = promptFormatChoice == choice,
                onClick = { onPromptFormatChange(choice) },
                label = { Text(stringResource(label)) },
            )
        }
    }
    val effectiveLabel = when (effectivePromptFormat) {
        PromptFormat.UNIVERSAL -> R.string.quick_prompt_format_universal
        PromptFormat.TAGS -> R.string.quick_prompt_format_tags
        PromptFormat.DESCRIPTION -> R.string.quick_prompt_format_description
    }
    Text(
        text = stringResource(R.string.quick_prompt_format_active, stringResource(effectiveLabel)),
        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
    )
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
                    onApplyTemplates(preset.prompt, preset.negativePrompt)
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
