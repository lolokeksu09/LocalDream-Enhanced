package io.github.xororz.localdream.data

import java.io.File
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ModelArchiveInstallerTest {
    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun emptyExtractionLeavesInstalledModelUntouched() {
        val stagingDir = tmp.newFolder("staging")
        val modelDir = tmp.newFolder("model")
        val backupDir = File(tmp.root, "backup")
        File(modelDir, "weights.bin").writeText("known-good-model")

        assertThrows(IOException::class.java) {
            ModelArchiveInstaller.install(stagingDir, modelDir, backupDir)
        }

        assertEquals("known-good-model", File(modelDir, "weights.bin").readText())
        assertFalse(backupDir.exists())
    }

    @Test
    fun validExtractionReplacesModelAndRemovesBackup() {
        val stagingDir = tmp.newFolder("staging")
        val modelDir = tmp.newFolder("model")
        val backupDir = File(tmp.root, "backup")
        File(stagingDir, "weights.bin").writeText("new-model")
        File(modelDir, "weights.bin").writeText("old-model")

        val backupRemoved = ModelArchiveInstaller.install(stagingDir, modelDir, backupDir)

        assertTrue(backupRemoved)
        assertEquals("new-model", File(modelDir, "weights.bin").readText())
        assertFalse(File(modelDir, "old-only.bin").exists())
        assertFalse(stagingDir.exists())
        assertFalse(backupDir.exists())
    }
}
