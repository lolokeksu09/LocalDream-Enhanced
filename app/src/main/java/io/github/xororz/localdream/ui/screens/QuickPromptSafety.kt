package io.github.xororz.localdream.ui.screens

internal fun requiresQuickPromptConfirmation(prompt: String, negative: String): Boolean {
    if (prompt.isBlank() && negative.isBlank()) return false
    return PromptFormat.values().none { format ->
        QuickPromptCategory.values().any { category ->
            quickPromptOptions(category, format).any {
                it.prompt == prompt && it.negativePrompt == negative
            }
        }
    }
}

internal fun appendQuickPrompt(current: String, addition: String): String {
    if (current.isBlank()) return addition
    if (addition.isBlank()) return current
    val existing = current.trimEnd()
    return existing + (if (existing.endsWith(",")) " " else ", ") + addition.trimStart()
}
