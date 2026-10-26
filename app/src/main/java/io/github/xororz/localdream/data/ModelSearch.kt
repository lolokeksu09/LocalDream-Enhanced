package io.github.xororz.localdream.data

/**
 * Case-insensitive model catalogue search. Searches the user-visible name,
 * description, stable model ID, and source URL without changing model order.
 */
internal fun matchesModelSearch(
    query: String,
    name: String,
    description: String,
    id: String,
    baseUrl: String,
): Boolean {
    val normalizedQuery = query.trim()
    if (normalizedQuery.isEmpty()) return true

    return sequenceOf(name, description, id, baseUrl)
        .any { field -> field.contains(normalizedQuery, ignoreCase = true) }
}
