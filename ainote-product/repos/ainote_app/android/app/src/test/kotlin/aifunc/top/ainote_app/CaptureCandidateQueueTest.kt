package aifunc.top.ainote_app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureCandidateQueueTest {
    @Test
    fun preservesFifoUntilExplicitAcknowledgement() {
        val queue = CaptureCandidateQueue(capacity = 3)
        val first = CaptureCandidate("first", "screenshot", uri = "content://first")
        val second = CaptureCandidate("second", "share", uri = "content://second")

        assertTrue(queue.upsert(first))
        assertTrue(queue.upsert(second))
        assertEquals("first", queue.peek()?.id)
        assertEquals("first", queue.peek()?.id)

        assertTrue(queue.acknowledge("first"))
        assertEquals("second", queue.peek()?.id)
        assertTrue(queue.acknowledge("second"))
        assertNull(queue.peek())
    }

    @Test
    fun updateKeepsPositionAndCapacityNeverEvictsPendingCandidate() {
        val queue = CaptureCandidateQueue(capacity = 2)
        assertTrue(queue.upsert(CaptureCandidate("first", "screenshot", uri = "content://first")))
        assertTrue(queue.upsert(CaptureCandidate("second", "share", uri = "content://second")))
        assertTrue(queue.upsert(CaptureCandidate(
            "first",
            "screenshot",
            localPath = "private/first.png",
            mimeType = "image/png",
        )))
        assertFalse(queue.upsert(CaptureCandidate("third", "picker")))

        assertEquals(listOf("first", "second"), queue.snapshot().map { it.id })
        assertEquals("private/first.png", queue.peek()?.localPath)
    }
}
