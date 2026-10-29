package io.github.xororz.localdream.data

/**
 * Validates the relative paths used by history backups.
 *
 * Model IDs are user or repository supplied and may contain spaces, dots, or
 * unicode, so validation deliberately rejects only path separators, empty
 * traversal segments, and an invalid image filename.
 */
internal object HistoryBackupPath {
    private val imageName = Regex("\\d+\\.(png|jpg)")

    fun isSafeImagePath(path: String): Boolean {
        if (path.indexOf('\u0000') >= 0) return false
        val parts = path.split('/')
        if (parts.size != 3 || parts[0] != "history") return false
        val modelSegment = parts[1]
        return modelSegment.isNotEmpty() &&
            modelSegment != "." &&
            modelSegment != ".." &&
            modelSegment.indexOf('\\') < 0 &&
            imageName.matches(parts[2])
    }
}
