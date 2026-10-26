package io.github.xororz.localdream.remote

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class RemoteRequestFramingTest {
    @Test fun onlyAllowedRoutesAreForwarded() {
        assertEquals("POST" to "/generate", RemoteRequestFraming.parseRequestLine("POST /generate HTTP/1.1"))
        assertEquals("GET" to "/health", RemoteRequestFraming.parseRequestLine("GET /health HTTP/1.1"))
        assertNull(RemoteRequestFraming.parseRequestLine("POST /health HTTP/1.1"))
        assertNull(RemoteRequestFraming.parseRequestLine("GET /generate HTTP/1.1"))
        assertNull(RemoteRequestFraming.parseRequestLine("GET /admin HTTP/1.1"))
        assertNull(RemoteRequestFraming.parseRequestLine("POST /generate?x=1 HTTP/1.1"))
    }

    @Test fun rejectsAmbiguousOrOversizedBodies() {
        assertNull(RemoteRequestFraming.parseLength("-1"))
        assertNull(RemoteRequestFraming.parseLength("12abc"))
        assertNull(RemoteRequestFraming.parseLength("99999999999999999999"))
        assertNull(RemoteRequestFraming.parseLength((RemoteRequestFraming.MAX_BODY_BYTES + 1).toString()))
        assertTrue(RemoteRequestFraming.validBody("POST", 13))
        assertTrue(RemoteRequestFraming.validBody("GET", 0))
        assertTrue(!RemoteRequestFraming.validBody("POST", 0))
    }

    @Test fun uploadsExactlyDeclaredLengthWithoutWaitingForEof() {
        val output = ByteArrayOutputStream()
        val input = ByteArrayInputStream("helloEXTRA".toByteArray())
        RemoteRequestFraming.copyExactly(input, output, 5)
        assertArrayEquals("hello".toByteArray(), output.toByteArray())
        assertEquals(5, input.available())
    }

    @Test fun truncatedUploadFailsClosed() {
        try {
            RemoteRequestFraming.copyExactly(ByteArrayInputStream(byteArrayOf(1)), ByteArrayOutputStream(), 3)
            fail("Expected truncated body rejection")
        } catch (_: IOException) {
            // expected
        }
    }
}
