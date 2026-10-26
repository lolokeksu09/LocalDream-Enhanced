package io.github.xororz.localdream.ui.screens

import android.content.Context
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.xororz.localdream.R

private data class SavedPromptTemplate(
    val name: String,
    val prompt: String,
    val negativePrompt: String,
    val updatedAt: Long,
)

private object PromptTemplateStore {
    private const val PREFS_NAME = "localdream_prompt_templates"
    private const val TEMPLATES_KEY = "templates"

    fun load(context: Context): List<SavedPromptTemplate> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(TEMPLATES_KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val name = item.optString("name").trim()
                    if (name.isBlank()) continue
                    add(
                        SavedPromptTemplate(
                            name = name,
                            prompt = item.optString("prompt"),
                            negativePrompt = item.optString("negativePrompt"),
                            updatedAt = item.optLong("updatedAt", 0L),
                        ),
                    )
                }
            }.sortedByDescending(SavedPromptTemplate::updatedAt)
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun save(
        context: Context,
        name: String,
        prompt: String,
        negativePrompt: String,
    ) {
        val normalizedName = name.trim()
        require(normalizedName.isNotEmpty())
        val templates = load(context).toMutableList()
        templates.removeAll { it.name.equals(normalizedName, ignoreCase = true) }
        templates.add(
            0,
            SavedPromptTemplate(
                name = normalizedName,
                prompt = prompt,
                negativePrompt = negativePrompt,
                updatedAt = System.currentTimeMillis(),
            ),
        )
        val array = JSONArray()
        templates.forEach { template ->
            array.put(
                JSONObject().apply {
                    put("name", template.name)
                    put("prompt", template.prompt)
                    put("negativePrompt", template.negativePrompt)
                    put("updatedAt", template.updatedAt)
                },
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TEMPLATES_KEY, array.toString())
            .apply()
    }

    fun delete(context: Context, name: String) {
        val templates = load(context).filterNot { it.name == name }
        val array = JSONArray()
        templates.forEach { template ->
            array.put(
                JSONObject().apply {
                    put("name", template.name)
                    put("prompt", template.prompt)
                    put("negativePrompt", template.negativePrompt)
                    put("updatedAt", template.updatedAt)
                },
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(TEMPLATES_KEY, array.toString())
            .apply()
    }
}

@Composable
internal fun PromptTemplatesDialog(
    currentPrompt: String,
    currentNegativePrompt: String,
    onApply: (prompt: String, negativePrompt: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var templates by remember { mutableStateOf(PromptTemplateStore.load(context)) }
    var templateName by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SavedPromptTemplate?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.prompt_templates)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    stringResource(R.string.prompt_templates_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = templateName,
                        onValueChange = {
                            templateName = it
                            nameError = false
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(stringResource(R.string.prompt_template_name)) },
                        singleLine = true,
                        isError = nameError,
                    )
                    Button(
                        onClick = {
                            if (templateName.isBlank()) {
                                nameError = true
                            } else {
                                PromptTemplateStore.save(
                                    context,
                                    templateName,
                                    currentPrompt,
                                    currentNegativePrompt,
                                )
                                templates = PromptTemplateStore.load(context)
                                templateName = ""
                                nameError = false
                            }
                        },
                        enabled = currentPrompt.isNotBlank() || currentNegativePrompt.isNotBlank(),
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
                if (nameError) {
                    Text(
                        stringResource(R.string.prompt_template_name_required),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (templates.isEmpty()) {
                    Text(
                        stringResource(R.string.no_prompt_templates),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(templates, key = SavedPromptTemplate::name) { template ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        template.name,
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    if (template.prompt.isNotBlank()) {
                                        Text(
                                            template.prompt,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    if (template.negativePrompt.isNotBlank()) {
                                        Text(
                                            stringResource(R.string.negative_prompt) +
                                                ": " + template.negativePrompt,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                    ) {
                                        TextButton(
                                            onClick = {
                                                onApply(template.prompt, template.negativePrompt)
                                            },
                                        ) {
                                            Text(stringResource(R.string.apply_prompt_template))
                                        }
                                        TextButton(
                                            onClick = { pendingDelete = template },
                                            colors = ButtonDefaults.textButtonColors(
                                                contentColor = MaterialTheme.colorScheme.error,
                                            ),
                                        ) {
                                            Text(stringResource(R.string.delete_prompt_template))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )

    pendingDelete?.let { template ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.delete_prompt_template)) },
            text = {
                Text(stringResource(R.string.prompt_template_delete_confirm, template.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        PromptTemplateStore.delete(context, template.name)
                        templates = PromptTemplateStore.load(context)
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(R.string.delete_prompt_template))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}
