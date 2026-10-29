package io.github.xororz.localdream.ui.screens

import io.github.xororz.localdream.data.GenerationMode
import io.github.xororz.localdream.data.TagMatchType
import io.github.xororz.localdream.data.TagSuggestion

/**
 * Pure generation-mode selection shared by history persistence and parameter export.
 * Precedence is unchanged: UltraFix > edit > inpaint > img2img > txt2img.
 */
internal fun resolveGenerationMode(
    wasUltrafix: Boolean,
    supportsReferenceEditing: Boolean,
    hasBaseImage: Boolean,
    hasReferenceImages: Boolean,
    isInpaintMode: Boolean,
): GenerationMode = when {
    wasUltrafix -> GenerationMode.ULTRAFIX
    supportsReferenceEditing && (hasBaseImage || hasReferenceImages) -> GenerationMode.EDIT
    isInpaintMode -> GenerationMode.INPAINT
    hasBaseImage -> GenerationMode.IMG2IMG
    else -> GenerationMode.TXT2IMG
}

/** Embedding autocomplete: normalize separators and rank prefix matches first. */
internal fun matchingEmbeddingSuggestions(
    names: List<String>,
    query: String,
    limit: Int = 5,
): List<TagSuggestion> {
    if (names.isEmpty() || limit <= 0) return emptyList()
    val q = query.trim().lowercase().replace(' ', '_').replace('-', '_')
    if (q.isEmpty()) return emptyList()
    val prefix = mutableListOf<TagSuggestion>()
    val contains = mutableListOf<TagSuggestion>()
    for (name in names) {
        val normalized = name.lowercase().replace(' ', '_').replace('-', '_')
        val idx = normalized.indexOf(q)
        if (idx < 0) continue
        val suggestion = TagSuggestion(
            replacementTag = name,
            primaryText = name,
            secondaryText = null,
            matchType = TagMatchType.Embedding,
            category = 0,
            postCount = 0,
            score = 0,
        )
        if (idx == 0) prefix += suggestion else contains += suggestion
    }
    return (prefix + contains).take(limit)
}
