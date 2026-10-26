package io.github.xororz.localdream.ui.screens

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelRunImageJobControllerTest {
    @Test
    fun latestJobCancelsPreviousAndInvalidatesStaleToken() = runBlocking {
        val controller = ModelRunImageJobController()
        val firstStarted = CompletableDeferred<Unit>()
        val firstGate = CompletableDeferred<Unit>()

        val first = controller.launch(this) { token ->
            assertTrue(controller.isCurrent(token))
            firstStarted.complete(Unit)
            firstGate.await()
        }
        firstStarted.await()

        var secondToken = 0L
        val second = controller.launch(this) { token ->
            secondToken = token
        }
        second.join()
        first.join()

        assertTrue(first.isCancelled)
        assertTrue(secondToken > 0L)
        assertTrue(controller.isCurrent(secondToken))

        controller.cancel()
        assertFalse(controller.isCurrent(secondToken))
    }
}
