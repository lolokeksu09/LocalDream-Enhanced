package io.github.xororz.localdream.data

import java.io.File
import java.io.IOException

/**
 * Installs a fully extracted model without deleting the currently installed
 * version until the replacement has been validated and can be moved into place.
 *
 * [stagingDir], [modelDir], and [backupDir] must be on the same filesystem so
 * directory renames can commit the replacement without copying gigabytes.
 *
 * Returns false only when the new model is installed but the old backup could
 * not be removed. The installed model is still usable in that case.
 */
internal object ModelArchiveInstaller {
    /**
     * Repairs an interrupted replacement before another download starts.
     * If the model directory is missing, the backup is the only known-good copy
     * and must be restored before any scratch cleanup can remove other files.
     */
    @Throws(IOException::class)
    fun recover(modelDir: File, backupDir: File) {
        if (!backupDir.exists()) return

        if (modelDir.exists()) {
            if (!backupDir.deleteRecursively()) {
                throw IOException("Cannot clear stale model backup: ${backupDir.absolutePath}")
            }
        } else if (!backupDir.renameTo(modelDir)) {
            throw IOException("Cannot restore previous model from ${backupDir.absolutePath}")
        }
    }

    @Throws(IOException::class)
    fun install(stagingDir: File, modelDir: File, backupDir: File): Boolean {
        // Restore a previous version first, even when this extraction is invalid.
        recover(modelDir, backupDir)

        if (!stagingDir.isDirectory) {
            throw IOException("Model extraction directory is missing")
        }
        val hasPayload = stagingDir.walkTopDown().any { it.isFile && it.length() > 0L }
        if (!hasPayload) {
            throw IOException("Archive contains no non-empty model files")
        }

        val hadPreviousModel = modelDir.exists()
        if (hadPreviousModel && !modelDir.renameTo(backupDir)) {
            throw IOException("Cannot preserve the currently installed model")
        }

        try {
            if (!stagingDir.renameTo(modelDir)) {
                throw IOException("Cannot install the extracted model")
            }
        } catch (error: Exception) {
            if (hadPreviousModel && backupDir.exists() && !modelDir.exists()) {
                if (!backupDir.renameTo(modelDir)) {
                    error.addSuppressed(
                        IOException("Previous model backup remains at ${backupDir.absolutePath}"),
                    )
                }
            }
            throw error
        }

        return !backupDir.exists() || backupDir.deleteRecursively()
    }
}
