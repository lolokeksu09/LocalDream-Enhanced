package io.github.xororz.localdream.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryBackupPathTest {
    @Test fun acceptsGeneratedPathsWithHumanReadableModelIds() {
        assertTrue(HistoryBackupPath.isSafeImagePath("history/RealVis XL v5/1712345678.png"))
        assertTrue(HistoryBackupPath.isSafeImagePath("history/qwen.image_2-1/1712345678.jpg"))
    }

    @Test fun rejectsTraversalAndNestedPaths() {
        assertFalse(HistoryBackupPath.isSafeImagePath("history/../1712345678.png"))
        assertFalse(HistoryBackupPath.isSafeImagePath("history/./1712345678.png"))
        assertFalse(HistoryBackupPath.isSafeImagePath("history/model/sub/1712345678.png"))
        assertFalse(HistoryBackupPath.isSafeImagePath("history/model/../1712345678.png"))
    }

    @Test fun rejectsNonImageOrMalformedNames() {
        assertFalse(HistoryBackupPath.isSafeImagePath("history/model/1712345678.gif"))
        assertFalse(HistoryBackupPath.isSafeImagePath("history/model/image.png"))
        assertFalse(HistoryBackupPath.isSafeImagePath("/history/model/1712345678.png"))
    }
}
