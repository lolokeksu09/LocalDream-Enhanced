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
    @Throws(IOException::class)
    fun install(stagingDir: File, modelDir: File, backupDir: File): Boolean {
        val extractedFiles = stagingDir.listFiles()
            ?.filter { it.isFile && it.length() > 0L }
            .orEmpty()
        if (extractedFiles.isEmpty()) {
            throw IOException("Archive contains no non-empty model files")
        }

        if (backupDir.exists() && !backupDir.deleteRecursively()) {
            throw IOException("Cannot clear the previous model backup")
        }

        val hadPreviousModel = modelDir.exists()
        if (hadPreviousModel && !modelDir.renameTo(backupDir)) {
            throw IOException("Cannot preserve the currently installed model")
        }

        try {
            if (!stagingDir.renameTo(modelDir)) {
                throw IOException("Cannot install the extracted model")
            }
        } catch (error: IOException) {
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
