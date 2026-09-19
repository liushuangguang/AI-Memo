package aifunc.top.ainote_app

import java.util.Locale

internal class ScreenshotCandidateFilter(
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    private val recent = LinkedHashMap<String, Long>()

    @Synchronized
    fun shouldAccept(
        uri: String,
        displayName: String?,
        relativePath: String?,
        dateAddedSeconds: Long,
        size: Long,
    ): Boolean {
        val lowerName = displayName.orEmpty().lowercase(Locale.ROOT)
        val lowerPath = relativePath.orEmpty().lowercase(Locale.ROOT)
        val screenshotLike = SCREENSHOT_MARKERS.any {
            lowerName.contains(it) || lowerPath.contains(it)
        }
        if (!screenshotLike || size <= 0L) return false

        val now = nowMillis()
        val ageMillis = now - dateAddedSeconds * 1_000L
        if (ageMillis !in -2_000L..RECENT_WINDOW_MILLIS) return false

        recent.entries.removeIf { now - it.value > DEDUP_WINDOW_MILLIS }
        val signature = "$uri|$size|$dateAddedSeconds"
        if (recent.containsKey(signature)) return false
        recent[signature] = now
        while (recent.size > MAX_RECENT) {
            recent.remove(recent.entries.first().key)
        }
        return true
    }

    companion object {
        private const val RECENT_WINDOW_MILLIS = 15_000L
        private const val DEDUP_WINDOW_MILLIS = 10 * 60_000L
        private const val MAX_RECENT = 32
        private val SCREENSHOT_MARKERS = listOf(
            "screenshot", "screen_shot", "screen-shot", "截屏", "截图", "スクリーンショット",
        )
    }
}
