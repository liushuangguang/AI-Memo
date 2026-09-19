package aifunc.top.ainote_app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturePermissionStatusTest {
    @Test
    fun allThreePermissionFamiliesAreRequired() {
        assertTrue(CapturePermissionStatus(true, true, true).allGranted)
        assertFalse(CapturePermissionStatus(false, true, true).allGranted)
        assertFalse(CapturePermissionStatus(true, false, true).allGranted)
        assertFalse(CapturePermissionStatus(true, true, false).allGranted)
    }

    @Test
    fun overlayDenialHasSpecificRecoveryMessage() {
        val status = CapturePermissionStatus(
            imageGranted = true,
            notificationsGranted = true,
            overlayGranted = false,
        )

        assertTrue(status.denialMessage().contains("显示在其他应用上层"))
    }
}
