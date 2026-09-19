package aifunc.top.ainote_app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureAutomationPolicyTest {
    @Test
    fun consentOnlyChangesModeOfFutureScreenshotCandidates() {
        val historical = CaptureCandidate("old", "screenshot")
        val futureMode = CaptureAutomationPolicy.modeForNewCandidate("screenshot", autoEnabled = true)

        assertEquals(CaptureHandlingMode.CONFIRM, historical.handlingMode)
        assertEquals(CaptureHandlingMode.AUTO, futureMode)
        assertEquals(CaptureHandlingMode.CONFIRM,
            CaptureAutomationPolicy.modeForNewCandidate("screenshot", autoEnabled = false))
    }

    @Test
    fun delayedPreConsentMediaEventRemainsConfirm() {
        val consentEpoch = 1_000_500L
        val delayedPreConsent = CaptureAutomationPolicy.conservativeCaptureTimeMillis(
            dateAddedSeconds = 1_002L,
            dateTakenMillis = 1_000_000L,
        )
        val genuinePostConsent = CaptureAutomationPolicy.conservativeCaptureTimeMillis(
            dateAddedSeconds = 1_002L,
            dateTakenMillis = 1_001_000L,
        )

        assertEquals(
            CaptureHandlingMode.CONFIRM,
            CaptureAutomationPolicy.modeForDetectedScreenshot(true, consentEpoch, delayedPreConsent),
        )
        assertEquals(
            CaptureHandlingMode.CONFIRM,
            CaptureAutomationPolicy.modeForDetectedScreenshot(true, consentEpoch, 1_000_500L),
        )
        assertEquals(
            CaptureHandlingMode.AUTO,
            CaptureAutomationPolicy.modeForDetectedScreenshot(true, consentEpoch, genuinePostConsent),
        )
        assertEquals(
            CaptureHandlingMode.CONFIRM,
            CaptureAutomationPolicy.modeForDetectedScreenshot(
                true,
                consentEpoch,
                CaptureAutomationPolicy.conservativeCaptureTimeMillis(1_002L, 0L),
            ),
        )
    }

    @Test
    fun persistedAutoCandidateKeepsIdentityAndStableRequestId() {
        val original = CaptureCandidate(
            id = "stable-request-id",
            source = "screenshot",
            handlingMode = CaptureHandlingMode.AUTO,
            autoConsentEpoch = 100L,
            identityId = "guest-device",
        )

        val restored = CaptureCandidate.fromJson(original.toJson())

        assertEquals("stable-request-id", restored?.id)
        assertEquals("guest-device", restored?.identityId)
        assertTrue(CaptureAutomationPolicy.canProcess(
            restored!!,
            guestMode = true,
            endpointAccepted = true,
            currentIdentityId = "guest-device",
            permissionsGranted = true,
        ))
    }

    @Test
    fun shareAndPickerNeverBecomeAutomatic() {
        assertEquals(CaptureHandlingMode.CONFIRM,
            CaptureAutomationPolicy.modeForNewCandidate("share", autoEnabled = true))
        assertEquals(CaptureHandlingMode.CONFIRM,
            CaptureAutomationPolicy.modeForNewCandidate("picker", autoEnabled = true))
    }

    @Test
    fun endpointAllowlistIsExactAndLanExceptionIsDebugOnly() {
        assertEquals("https://aifunc.top",
            CaptureEndpointPolicy.acceptedBaseUrl("https://aifunc.top", debug = false))
        assertNull(CaptureEndpointPolicy.acceptedBaseUrl("http://aifunc.top", debug = true))
        assertNull(CaptureEndpointPolicy.acceptedBaseUrl("https://aifunc.top:443", debug = false))
        assertNull(CaptureEndpointPolicy.acceptedBaseUrl("https://evil.test", debug = true))
        assertNull(CaptureEndpointPolicy.acceptedBaseUrl("http://192.168.31.213:8080", debug = false))
        assertEquals("http://192.168.31.213:8080",
            CaptureEndpointPolicy.acceptedBaseUrl("http://192.168.31.213:8080", debug = true))
    }

    @Test
    fun nonGuestIdentityChangeAndPermissionLossBlockProcessing() {
        val candidate = CaptureCandidate(
            id = "stable-id",
            source = "screenshot",
            handlingMode = CaptureHandlingMode.AUTO,
            identityId = "guest-device",
        )
        assertTrue(CaptureAutomationPolicy.canProcess(candidate, true, true, "guest-device", true))
        assertFalse(CaptureAutomationPolicy.canProcess(candidate, false, true, "guest-device", true))
        assertFalse(CaptureAutomationPolicy.canProcess(candidate, true, true, "other-device", true))
        assertFalse(CaptureAutomationPolicy.canProcess(candidate, true, true, "guest-device", false))
    }

    @Test
    fun interruptedUploadRecoversWithSameRequestId() {
        val interrupted = CaptureCandidate(
            id = "stable-request-id",
            source = "screenshot",
            handlingMode = CaptureHandlingMode.AUTO,
            state = CaptureJobState.UPLOADING,
            identityId = "guest-device",
        )
        val recovered = CaptureAutoTransition.recoverAfterRestart(interrupted)

        assertEquals(CaptureJobState.QUEUED, recovered.state)
        assertEquals("stable-request-id", recovered.id)
    }

    @Test
    fun failureRequiresExplicitRetryAndKeepsStableRequestId() {
        val failed = CaptureCandidate(
            id = "stable-request-id",
            source = "screenshot",
            handlingMode = CaptureHandlingMode.AUTO,
            state = CaptureJobState.FAILED,
            identityId = "guest-device",
            errorMessage = "network",
        )
        assertEquals(failed, CaptureAutoTransition.recoverAfterRestart(failed))
        val retried = CaptureAutoTransition.explicitRetry(failed, "guest-device")
        assertEquals(CaptureJobState.QUEUED, retried?.state)
        assertEquals("stable-request-id", retried?.id)
        assertNull(CaptureAutoTransition.explicitRetry(retried!!, "guest-device"))
    }

    @Test
    fun privateCopyDeletionRequiresPersistedExactNoteId() {
        val uploaded = CaptureCandidate(
            id = "request-id",
            source = "screenshot",
            handlingMode = CaptureHandlingMode.AUTO,
            state = CaptureJobState.COMPLETED,
            noteId = "note-id",
        )
        assertFalse(CaptureAutoTransition.mayDeletePrivateCopy(uploaded, completionPersisted = false))
        assertTrue(CaptureAutoTransition.mayDeletePrivateCopy(uploaded, completionPersisted = true))
        assertFalse(CaptureAutoTransition.mayDeletePrivateCopy(
            uploaded.copy(noteId = null),
            completionPersisted = true,
        ))
    }

    @Test
    fun explicitStopPausesTransientStatesWithoutChangingRequestId() {
        for (state in listOf(CaptureJobState.OCR, CaptureJobState.UPLOADING)) {
            val active = CaptureCandidate(
                id = "request-$state",
                source = "screenshot",
                handlingMode = CaptureHandlingMode.AUTO,
                state = state,
            )
            val paused = CaptureAutoTransition.pause(active, "stopped")
            assertEquals(CaptureJobState.PAUSED, paused.state)
            assertEquals(active.id, paused.id)
        }
    }

    @Test
    fun retryRequiresRunnablePrerequisitesAndExplicitFailedState() {
        val failed = CaptureCandidate(
            id = "request-id",
            source = "screenshot",
            handlingMode = CaptureHandlingMode.AUTO,
            state = CaptureJobState.FAILED,
        )
        assertTrue(CaptureAutomationPolicy.canRetry(failed, true, true, true, true))
        assertFalse(CaptureAutomationPolicy.canRetry(failed, false, true, true, true))
        assertFalse(CaptureAutomationPolicy.canRetry(failed, true, false, true, true))
        assertFalse(CaptureAutomationPolicy.canRetry(failed, true, true, false, true))
        assertFalse(CaptureAutomationPolicy.canRetry(failed, true, true, true, false))
        assertFalse(CaptureAutomationPolicy.canRetry(
            failed.copy(state = CaptureJobState.QUEUED), true, true, true, true,
        ))
    }

    @Test
    fun exactCompletionSelectionNeverFallsBackToAnotherNote() {
        val completions = listOf(
            CaptureCompletion("candidate-y", "note-y"),
            CaptureCompletion("candidate-z", "note-z"),
        )
        assertNull(CaptureCompletionSelector.select(completions, "note-x"))
        assertEquals("note-y", CaptureCompletionSelector.select(completions, null)?.noteId)
        assertEquals("note-z", CaptureCompletionSelector.select(completions, "note-z")?.noteId)
    }
}
