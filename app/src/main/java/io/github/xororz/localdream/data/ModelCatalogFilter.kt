package io.github.xororz.localdream.data

/** Model catalogue filters operate on a model's existing install and pin state. */
internal enum class ModelCatalogFilter {
    ALL,
    INSTALLED,
    AVAILABLE,
    PINNED,
}

internal fun matchesModelCatalogFilter(
    filter: ModelCatalogFilter,
    isDownloaded: Boolean,
    isPinned: Boolean,
): Boolean = when (filter) {
    ModelCatalogFilter.ALL -> true
    ModelCatalogFilter.INSTALLED -> isDownloaded
    ModelCatalogFilter.AVAILABLE -> !isDownloaded
    ModelCatalogFilter.PINNED -> isPinned
}
