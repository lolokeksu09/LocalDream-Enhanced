package io.github.xororz.localdream.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogSelectorTest {
    private val models = listOf(
        Model("cpu-a", "CPU Alpha", "portrait", "https://example.com/a", runOnCpu = true, isDownloaded = true),
        Model("npu-a", "NPU Alpha", "landscape", "https://example.com/b", isDownloaded = true),
        Model("cpu-b", "CPU Beta", "anime", "https://example.com/c", runOnCpu = true),
        Model("npu-b", "NPU Beta", "illustration", "https://example.com/d", isDownloaded = true),
        Model("cpu-c", "CPU Gamma", "anime", "https://example.com/e", runOnCpu = true, isDownloaded = true),
    )
    private val pins = listOf("cpu-c", "npu-b", "cpu-a")

    @Test fun allRetainsStableOrderWithinTabsAfterPinnedFirstSort() {
        val sections = ModelCatalogSelector.select(models, pins, "", ModelCatalogFilter.ALL)
        assertEquals(listOf("cpu-c", "cpu-a", "cpu-b"), sections.cpu.map(Model::id))
        assertEquals(listOf("npu-b", "npu-a"), sections.npu.map(Model::id))
        assertEquals("cpu-a", models.first().id) // input unchanged
    }

    @Test fun installedAvailableAndPinnedFiltersRemainIndependentPerTab() {
        val installed = ModelCatalogSelector.select(models, pins, "", ModelCatalogFilter.INSTALLED)
        assertEquals(listOf("cpu-c", "cpu-a"), installed.cpu.map(Model::id))
        assertEquals(listOf("npu-b", "npu-a"), installed.npu.map(Model::id))

        val available = ModelCatalogSelector.select(models, pins, "", ModelCatalogFilter.AVAILABLE)
        assertEquals(listOf("cpu-b"), available.cpu.map(Model::id))
        assertTrue(available.npu.isEmpty())

        val pinned = ModelCatalogSelector.select(models, pins, "", ModelCatalogFilter.PINNED)
        assertEquals(listOf("cpu-c", "cpu-a"), pinned.cpu.map(Model::id))
        assertEquals(listOf("npu-b"), pinned.npu.map(Model::id))
    }

    @Test fun searchIncludesMetadataAndComposesWithFilters() {
        val anime = ModelCatalogSelector.select(models, pins, "  ANIME ", ModelCatalogFilter.ALL)
        assertEquals(listOf("cpu-c", "cpu-b"), anime.cpu.map(Model::id))
        assertTrue(anime.npu.isEmpty())

        val filtered = ModelCatalogSelector.select(models, pins, "https://example.com/e", ModelCatalogFilter.PINNED)
        assertEquals(listOf("cpu-c"), filtered.cpu.map(Model::id))

        val noMatch = ModelCatalogSelector.select(models, pins, "not-present", ModelCatalogFilter.ALL)
        assertTrue(noMatch.cpu.isEmpty() && noMatch.npu.isEmpty())
    }

    @Test fun whenNoModelsBothTabsAreEmpty() {
        val sections = ModelCatalogSelector.select(emptyList(), pins, "", ModelCatalogFilter.ALL)
        assertTrue(sections.cpu.isEmpty() && sections.npu.isEmpty())
    }
}
