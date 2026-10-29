package io.github.xororz.localdream.ui.screens

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Owns transient image-preparation work started from ModelRunScreen.
 *
 * A new selection invalidates the previous job. The monotonically increasing
 * token lets callers guard state updates from a cancelled job that finishes
 * late on a dispatcher thread.
 */
internal class ModelRunImageJobController {
    private var nextToken = 0L
    private var activeToken = 0L
    private var activeJob: Job? = null

    @Synchronized
    fun launch(
        scope: CoroutineScope,
        block: suspend (token: Long) -> Unit,
    ): Job {
        activeJob?.cancel()
        val token = ++nextToken
        activeToken = token
        val job = scope.launch {
            block(token)
        }
        activeJob = job
        job.invokeOnCompletion {
            synchronized(this) {
                if (activeJob === job) {
                    activeJob = null
                }
            }
        }
        return job
    }

    @Synchronized
    fun isCurrent(token: Long): Boolean = token == activeToken

    @Synchronized
    fun cancel() {
        activeToken = ++nextToken
        activeJob?.cancel()
        activeJob = null
    }
}
