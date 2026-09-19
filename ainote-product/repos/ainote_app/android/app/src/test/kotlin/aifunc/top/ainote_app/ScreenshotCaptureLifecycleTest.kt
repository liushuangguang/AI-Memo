package aifunc.top.ainote_app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotCaptureLifecycleTest {
    @Test
    fun redeliveredServiceCanRecoverStartupWithoutFlutterBridge() {
        val gate = ScreenshotWatchStartupGate()

        assertTrue(gate.prepareForServiceStart())
        assertTrue(gate.markReady())
        assertTrue(gate.isReady)
        assertFalse(gate.prepareForServiceStart())
    }

    @Test
    fun deliveryDoesNotBecomeReadyUntilServiceInitializationCompletes() {
        val gate = ScreenshotWatchStartupGate()
        var completed: Result<Unit>? = null

        assertTrue(gate.begin { completed = it })
        assertFalse(gate.isReady)
        assertEquals(null, completed)

        assertTrue(gate.markReady())

        assertTrue(gate.isReady)
        assertTrue(completed?.isSuccess == true)
    }

    @Test
    fun initializationFailureNeverReportsReadyAndCanBeRetried() {
        val gate = ScreenshotWatchStartupGate()
        var first: Result<Unit>? = null
        var second: Result<Unit>? = null

        assertTrue(gate.begin { first = it })
        gate.markFailed(SecurityException("permission changed"))

        assertFalse(gate.isReady)
        assertTrue(first?.isFailure == true)
        assertTrue(gate.begin { second = it })
        assertEquals(null, second)
    }

    @Test
    fun aStopRequestedDuringStartupCannotBeResurrectedAsReady() {
        val gate = ScreenshotWatchStartupGate()
        var completion: Result<Unit>? = null

        assertTrue(gate.begin { completion = it })
        gate.markStopped()

        assertFalse(gate.markReady())
        assertFalse(gate.isReady)
        assertTrue(completion?.isFailure == true)
    }

    @Test
    fun closedOverlayIsShownAgainForPendingHeadWhenANewerCandidateArrives() {
        val pendingIds = mutableListOf("A")
        var overlayVisible = false
        var showAttempts = 0
        var failures = 0
        val coordinator = ScreenshotPendingUiCoordinator(
            hasPending = { pendingIds.isNotEmpty() },
            showOverlay = {
                showAttempts += 1
                overlayVisible = true
                true
            },
            removeOverlay = { overlayVisible = false },
            updateNotification = {},
            onOverlayFailure = { failures += 1 },
        )

        assertTrue(coordinator.synchronize(activityForeground = false))
        overlayVisible = false // User closes A without acknowledging it.
        pendingIds += "B"
        assertTrue(coordinator.synchronize(activityForeground = false))

        assertEquals("A", pendingIds.first())
        assertTrue(overlayVisible)
        assertEquals(2, showAttempts)
        assertEquals(0, failures)
    }

    @Test
    fun foregroundUsesFlutterOnlyAndAcknowledgementReconcilesNextPendingItem() {
        val pendingIds = mutableListOf("A", "B")
        var overlayVisible = true
        var showAttempts = 0
        var removals = 0
        val notificationStates = mutableListOf<Boolean>()
        val coordinator = ScreenshotPendingUiCoordinator(
            hasPending = { pendingIds.isNotEmpty() },
            showOverlay = {
                showAttempts += 1
                // Already visible is an idempotent success.
                overlayVisible = true
                true
            },
            removeOverlay = {
                removals += 1
                overlayVisible = false
            },
            updateNotification = notificationStates::add,
            onOverlayFailure = { throw AssertionError("overlay should not fail") },
        )

        assertTrue(coordinator.synchronize(activityForeground = true))
        assertFalse(overlayVisible)
        assertEquals(0, showAttempts)
        assertEquals(listOf(true), notificationStates)

        pendingIds.removeAt(0)
        assertTrue(coordinator.onCandidateAcknowledged(activityForeground = false))
        assertTrue(overlayVisible)
        assertEquals("B", pendingIds.first())

        pendingIds.removeAt(0)
        assertTrue(coordinator.onCandidateAcknowledged(activityForeground = false))
        assertFalse(overlayVisible)
        assertEquals(listOf(true, true, false), notificationStates)
        assertEquals(3, removals)
    }
}
