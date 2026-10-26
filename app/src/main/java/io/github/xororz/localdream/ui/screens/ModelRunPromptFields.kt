package io.github.xororz.localdream.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    showTemplates: Boolean,
    onShowTemplates: () -> Unit,
    onApplyTemplates: (String, String) -> Unit,
    onDismissTemplates: () -> Unit,
) {
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = onShowTemplates) {
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
