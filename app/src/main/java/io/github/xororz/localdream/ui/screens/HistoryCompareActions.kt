package io.github.xororz.localdream.ui.screens

/** Parameters are applied only when the compared result belongs to the open model. */
internal fun canReuseComparedSettings(currentModelId: String?, sourceModelId: String): Boolean =
    !currentModelId.isNullOrBlank() && currentModelId == sourceModelId
