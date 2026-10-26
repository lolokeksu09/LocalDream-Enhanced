package io.github.xororz.localdream.ui.screens

/**
 * Prevents a late backend health callback from updating a newer screen state.
 *
 * LaunchedEffect cancellation normally handles this path, but health checks can
 * cross dispatcher boundaries. The generation token makes the callback guard
 * explicit and cheap.
 */
internal class ModelRunBackendReadinessGuard {
    private var generation = 0L

    @Synchronized
    fun begin(): Long = ++generation

    @Synchronized
    fun isCurrent(token: Long): Boolean = token == generation

    @Synchronized
    fun cancel() {
        generation++
    }
}
