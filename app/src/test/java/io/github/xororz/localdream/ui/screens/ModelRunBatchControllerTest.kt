package io.github.xororz.localdream.ui.screens

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunBatchControllerTest {
    @Test fun explicitSeedOverridesBatchCount() {
        assertEquals(1, effectiveGenerationBatchCount("1234", 10))
        assertEquals(1, effectiveGenerationBatchCount("0", 5))
        assertEquals(5, effectiveGenerationBatchCount("  ", 5))
        assertEquals(3, effectiveGenerationBatchCount("", 3))
    }

    @Test fun executesBatchesSequentiallyAndResetsVisibleIndex() = runBlocking {
        val controller = ModelRunBatchController(this)
        val events = mutableListOf<String>()
        val job = controller.launchBatch(
            count = 3,
            beforeFirst = { events += "prepare" },
            submit = { index ->
                assertEquals(index + 1, controller.currentIndex)
                events += "submit:$index"
            },
            awaitCompletion = { events += "wait" },
            onFinished = { events += "finished" },
        )
        job.join()
        assertEquals(
            listOf(
                "prepare", "submit:0", "wait",
                "submit:1", "wait",
                "submit:2", "wait", "finished",
            ),
            events,
        )
        assertEquals(0, controller.currentIndex)
    }

    @Test fun cancellationPreventsFurtherRequestsAndCompletionCallback() = runBlocking {
        val controller = ModelRunBatchController(this)
        val firstSubmitted = CompletableDeferred<Unit>()
        val gate = CompletableDeferred<Unit>()
        val submitted = mutableListOf<Int>()
        var finished = false
        val job = controller.launchBatch(
            count = 4,
            beforeFirst = {},
            submit = { index ->
                submitted += index
                if (index == 0) firstSubmitted.complete(Unit)
            },
            awaitCompletion = { gate.await() },
            onFinished = { finished = true },
        )
        firstSubmitted.await()
        assertEquals(1, controller.currentIndex)
        controller.cancel()
        job.join()
        assertEquals(listOf(0), submitted)
        assertEquals(0, controller.currentIndex)
        assertFalse(finished)
        assertTrue(job.isCancelled)
    }

    @Test fun seedModesProtectAgainstInvalidOrReservedValues() {
        assertEquals(SeedMode.RANDOM, seedMode(""))
        assertEquals(SeedMode.RANDOM, seedMode("  "))
        assertEquals(SeedMode.FIXED, seedMode("0"))
        assertEquals(SeedMode.FIXED, seedMode("9223372036854775807"))
        assertEquals(SeedMode.INVALID, seedMode("-1"))
        assertEquals(SeedMode.INVALID, seedMode("abc"))
        assertEquals(SeedMode.INVALID, seedMode("9223372036854775808"))
    }

    @Test fun lockPrefersDisplayedResultAndFallsBackToNewSeed() {
        assertEquals("123", seedForLock(123L) { 777L })
        assertEquals("777", seedForLock(null) { 777L })
        assertEquals("777", seedForLock(-1L) { 777L })
    }
}
