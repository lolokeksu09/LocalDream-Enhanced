package io.github.xororz.localdream.data

/**
 * Pure catalogue projection used by the Compose screen.
 *
 * Search/filter runs once, followed by the original stable pinned-first
 * ordering. Partition preserves the relative order within CPU and NPU tabs.
 * This class does not read Android preferences or mutate model state.
 */
internal data class ModelCatalogSections(
    val cpu: List<Model>,
    val npu: List<Model>,
)

internal object ModelCatalogSelector {
    fun select(
        models: List<Model>,
        pinnedIds: List<String>,
        query: String,
        filter: ModelCatalogFilter,
    ): ModelCatalogSections {
        val pinnedSet = pinnedIds.toHashSet()
        val matching = models.filter { model ->
            matchesModelCatalogFilter(
                filter = filter,
                isDownloaded = model.isDownloaded,
                isPinned = model.id in pinnedSet,
            ) && matchesModelSearch(
                query = query,
                name = model.name,
                description = model.description,
                id = model.id,
                baseUrl = model.baseUrl,
            )
        }
        val sorted = PinnedModels.sort(matching, pinnedIds)
        val (cpu, npu) = sorted.partition(Model::runOnCpu)
        return ModelCatalogSections(cpu, npu)
    }
}
