package aifunc.top.ainote_app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotCandidateFilterTest {
    private val now = 1_800_000_000_000L
    private val filter = ScreenshotCandidateFilter { now }

    @Test
    fun acceptsRecentScreenshotUriOnlyOnce() {
        val added = now / 1_000L
        assertTrue(filter.shouldAccept(
            "content://media/external/images/media/42",
            "Screenshot_20260906.png",
            "Screenshots",
            added,
            1_024,
        ))
        assertFalse(filter.shouldAccept(
            "content://media/external/images/media/42",
            "Screenshot_20260906.png",
            "Screenshots",
            added,
            1_024,
        ))
    }

    @Test
    fun rejectsOrdinaryOrStaleImages() {
        assertFalse(filter.shouldAccept(
            "content://media/external/images/media/1",
            "holiday.png",
            "Camera",
            now / 1_000L,
            1_024,
        ))
        assertFalse(filter.shouldAccept(
            "content://media/external/images/media/2",
            "截图.png",
            "截屏",
            (now - 60_000L) / 1_000L,
            1_024,
        ))
    }
}
