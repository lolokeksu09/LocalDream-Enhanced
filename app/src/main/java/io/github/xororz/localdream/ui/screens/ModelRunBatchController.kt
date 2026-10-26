package io.github.xororz.localdream.ui.screens

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import io.github.xororz.localdream.service.BackgroundGenerationService
import io.github.xororz.localdream.service.BackgroundGenerationService.GenerationState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.coroutineContext

/**
 * Owns the active generation coroutine and the visible 1-based batch index.
 * The screen still creates each request, while this class serializes requests,
 * waits for the actual service teardown, and resets state between iterations.
 */
internal class ModelRunBatchController(private val scope: CoroutineScope) {
    var currentIndex by mutableIntStateOf(0)
        private set

    private var activeJob: Job? = null

    /** UltraFix shares the same cancellation slot as ordinary generation. */
    fun launchSingle(block: suspend CoroutineScope.() -> Unit) {
        cancel()
        activeJob = scope.launch(block = block)
    }

    fun launchBatch(
        count: Int,
        beforeFirst: suspend () -> Unit,
        submit: suspend (index: Int) -> Unit,
        onFinished: () -> Unit,
        awaitCompletion: suspend () -> Unit = { awaitTerminalAndStop() },
    ): Job {
        require(count > 0) { "Batch count must be positive" }
        cancel()
        val job = scope.launch {
            try {
                beforeFirst()
                repeat(count) { index ->
                    coroutineContext.ensureActive()
                    currentIndex = index + 1
                    submit(index)
                    awaitCompletion()
                }
                currentIndex = 0
                onFinished()
            } finally {
                if (!coroutineContext.isActive) currentIndex = 0
            }
        }
        activeJob = job
        return job
    }

    /** Stops outstanding preparation/waits before the service is stopped. */
    fun cancel() {
        activeJob?.cancel()
        activeJob = null
        currentIndex = 0
    }

    /** Shared by UltraFix and batch generation. Avoid starting a new job before teardown. */
    suspend fun awaitTerminalAndStop() {
        BackgroundGenerationService.generationState.first { state ->
            state is GenerationState.Complete || state is GenerationState.Error
        }
        val start = System.currentTimeMillis()
        val stopped = withTimeoutOrNull(5_000L) {
            BackgroundGenerationService.isServiceRunning.first { !it }
        }
        if (stopped == null) {
            Log.w(TAG, "Timed out waiting for generation service to stop")
        }
        Log.d(TAG, "service teardown wait: ${System.currentTimeMillis() - start}ms")
        coroutineContext.ensureActive()
        BackgroundGenerationService.resetState()
    }

    private companion object {
        const val TAG = "ModelRunBatchController"
    }
}

/** An explicit seed has always meant a single deterministic generation. */
internal fun effectiveGenerationBatchCount(seed: String, requestedCount: Int): Int =
    if (seed.isNotBlank()) 1 else requestedCount
