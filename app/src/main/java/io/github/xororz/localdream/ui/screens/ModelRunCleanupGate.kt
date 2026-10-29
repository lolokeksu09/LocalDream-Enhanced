package io.github.xororz.localdream.ui.screens

/**
 * Makes screen teardown idempotent.
 *
 * Compose disposal and lifecycle destruction can both reach the same cleanup
 * lambda. Only the first caller may perform destructive teardown.
 */
internal class ModelRunCleanupGate {
    private var entered = false

    @Synchronized
    fun enter(): Boolean {
        if (entered) return false
        entered = true
        return true
    }
}
