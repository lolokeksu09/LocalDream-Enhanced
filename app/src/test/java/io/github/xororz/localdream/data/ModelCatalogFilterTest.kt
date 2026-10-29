package io.github.xororz.localdream.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogFilterTest {
    @Test fun allIncludesInstalledAndAvailable() {
        assertTrue(matchesModelCatalogFilter(ModelCatalogFilter.ALL, true, false))
        assertTrue(matchesModelCatalogFilter(ModelCatalogFilter.ALL, false, false))
    }

    @Test fun installedAndAvailableAreDisjoint() {
        assertTrue(matchesModelCatalogFilter(ModelCatalogFilter.INSTALLED, true, false))
        assertFalse(matchesModelCatalogFilter(ModelCatalogFilter.INSTALLED, false, false))
        assertTrue(matchesModelCatalogFilter(ModelCatalogFilter.AVAILABLE, false, false))
        assertFalse(matchesModelCatalogFilter(ModelCatalogFilter.AVAILABLE, true, false))
    }

    @Test fun pinnedOnlyIncludesPinnedModelsRegardlessOfInstallStatus() {
        assertTrue(matchesModelCatalogFilter(ModelCatalogFilter.PINNED, true, true))
        assertTrue(matchesModelCatalogFilter(ModelCatalogFilter.PINNED, false, true))
        assertFalse(matchesModelCatalogFilter(ModelCatalogFilter.PINNED, true, false))
    }
}
