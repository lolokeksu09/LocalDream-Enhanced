package io.github.xororz.localdream.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class BackupZipLimitsTest {
    @Test fun allowedManifestIsReadExactly() {
        val content = "valid JSON".toByteArray()
        assertArrayEquals(content, BackupZipLimits.readManifest(ByteArrayInputStream(content), BackupZipLimits.Budget()))
    }

    @Test fun boundedImageCopiesData() {
        val data = ByteArray(12345) { it.toByte() }
        val out = ByteArrayOutputStream()
        BackupZipLimits.copyImage(ByteArrayInputStream(data), out, BackupZipLimits.Budget())
        assertArrayEquals(data, out.toByteArray())
    }

    @Test fun tooManyEntriesAreRejected() {
        val budget = BackupZipLimits.Budget()
        repeat(BackupZipLimits.MAX_ENTRIES) { budget.entry() }
        try {
            budget.entry()
            fail("Expected archive entry limit")
        } catch (_: BackupFormatException) {
            // expected
        }
    }

    @Test fun largeManifestFailsBeforeAllocation() {
        val payload = ByteArray((BackupZipLimits.MAX_MANIFEST_BYTES + 1).toInt())
        try {
            BackupZipLimits.readManifest(ByteArrayInputStream(payload), BackupZipLimits.Budget())
            fail("Expected manifest cap")
        } catch (_: BackupFormatException) {
            // expected
        }
    }
}
