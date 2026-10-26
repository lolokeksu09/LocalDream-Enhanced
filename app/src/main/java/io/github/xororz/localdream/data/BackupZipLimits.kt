package io.github.xororz.localdream.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Limit expanded bytes, not compressed ZIP sizes (ZIP bombs have tiny inputs). */
internal object BackupZipLimits {
    const val MAX_MANIFEST_BYTES = 16L * 1024 * 1024
    const val MAX_IMAGE_BYTES = 128L * 1024 * 1024
    const val MAX_ENTRIES = 20_000
    const val MAX_TOTAL_BYTES = 16L * 1024 * 1024 * 1024

    class Budget {
        private var total = 0L
        private var entries = 0

        fun entry() {
            entries++
            if (entries > MAX_ENTRIES) throw BackupFormatException("too many files in backup")
        }

        fun used(bytes: Int) {
            total += bytes
            if (total > MAX_TOTAL_BYTES) {
                throw BackupFormatException("backup exceeds allowed expanded size")
            }
        }
    }

    fun readManifest(input: InputStream, budget: Budget): ByteArray {
        val output = ByteArrayOutputStream()
        transfer(input, output, MAX_MANIFEST_BYTES, budget)
        return output.toByteArray()
    }

    fun copyImage(input: InputStream, output: OutputStream, budget: Budget) {
        transfer(input, output, MAX_IMAGE_BYTES, budget)
    }

    fun skipEntry(input: InputStream, budget: Budget, limit: Long = MAX_IMAGE_BYTES) {
        transfer(input, null, limit, budget)
    }

    private fun transfer(input: InputStream, output: OutputStream?, maxBytes: Long, budget: Budget) {
        var count = 0L
        val buffer = ByteArray(32 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            if (n == 0) continue
            count += n
            if (count > maxBytes) throw BackupFormatException("oversized ZIP entry")
            budget.used(n)
            output?.write(buffer, 0, n)
        }
    }
}
