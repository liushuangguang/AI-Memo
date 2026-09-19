package aifunc.top.ainote_app

import android.content.Context
import java.net.URI

internal data class CaptureAutomationStatus(
    val available: Boolean,
    val enabled: Boolean,
    val consentEpoch: Long?,
    val message: String?,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "autoAvailable" to available,
        "autoEnabled" to enabled,
        "autoConsentEpoch" to consentEpoch,
        "autoMessage" to message,
    )
}

internal object CaptureEndpointPolicy {
    private const val PRODUCTION = "https://aifunc.top"
    private const val DEBUG_LAN = "http://192.168.31.213:8080"

    fun acceptedBaseUrl(value: String, debug: Boolean): String? {
        val normalized = value.trim().removeSuffix("/")
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return null
        if (uri.userInfo != null || uri.query != null || uri.fragment != null ||
            uri.path.orEmpty().isNotEmpty()
        ) return null
        return when {
            normalized == PRODUCTION && uri.scheme == "https" &&
                uri.host == "aifunc.top" && uri.port == -1 -> PRODUCTION
            debug && normalized == DEBUG_LAN && uri.scheme == "http" &&
                uri.host == "192.168.31.213" && uri.port == 8080 -> DEBUG_LAN
            else -> null
        }
    }
}

internal object CaptureAutomationPolicy {
    fun normalizedDeviceId(value: String?): String? {
        val normalized = value?.trim() ?: return null
        if (normalized.length < 3 || normalized.lowercase() in setOf("null", "undefined", "unknown")) {
            return null
        }
        if (normalized.any { it.code <= 0x20 || it.code == 0x7f }) return null
        return normalized
    }

    fun modeForNewCandidate(source: String, autoEnabled: Boolean): CaptureHandlingMode =
        if (source == "screenshot" && autoEnabled) CaptureHandlingMode.AUTO
        else CaptureHandlingMode.CONFIRM

    fun modeForDetectedScreenshot(
        autoEnabled: Boolean,
        consentEpoch: Long?,
        observedAtMillis: Long?,
    ): CaptureHandlingMode = if (
        autoEnabled && consentEpoch != null && consentEpoch > 0L &&
        observedAtMillis != null && observedAtMillis > consentEpoch
    ) CaptureHandlingMode.AUTO else CaptureHandlingMode.CONFIRM

    /**
     * DATE_ADDED is an insertion timestamp and cannot prove when pixels were
     * captured. Require DATE_TAKEN for auto consent, and use the earlier valid
     * timestamp so a delayed MediaStore insertion can never cross the consent
     * boundary. Missing/ambiguous capture time therefore stays confirm-only.
     */
    fun conservativeCaptureTimeMillis(dateAddedSeconds: Long, dateTakenMillis: Long): Long? {
        val taken = dateTakenMillis.takeIf { it > 0L } ?: return null
        val added = dateAddedSeconds.takeIf { it > 0L }?.let { seconds ->
            if (seconds > Long.MAX_VALUE / 1_000L) Long.MAX_VALUE else seconds * 1_000L
        }
        return if (added == null) taken else minOf(taken, added)
    }

    fun canProcess(
        candidate: CaptureCandidate,
        guestMode: Boolean,
        endpointAccepted: Boolean,
        currentIdentityId: String?,
        permissionsGranted: Boolean,
    ): Boolean = candidate.handlingMode == CaptureHandlingMode.AUTO &&
        candidate.state == CaptureJobState.QUEUED &&
        guestMode && endpointAccepted && permissionsGranted &&
        normalizedDeviceId(currentIdentityId) == candidate.identityId

    fun canRetry(
        candidate: CaptureCandidate?,
        autoEnabled: Boolean,
        autoAvailable: Boolean,
        permissionsGranted: Boolean,
        serviceReady: Boolean,
    ): Boolean = candidate?.handlingMode == CaptureHandlingMode.AUTO &&
        candidate.state in setOf(CaptureJobState.FAILED, CaptureJobState.PAUSED) &&
        autoEnabled && autoAvailable && permissionsGranted && serviceReady
}

internal object CaptureAutoTransition {
    fun recoverAfterRestart(candidate: CaptureCandidate): CaptureCandidate =
        if (candidate.handlingMode == CaptureHandlingMode.AUTO &&
            candidate.state in setOf(CaptureJobState.OCR, CaptureJobState.UPLOADING)
        ) candidate.copy(state = CaptureJobState.QUEUED) else candidate

    fun explicitRetry(candidate: CaptureCandidate, identityId: String): CaptureCandidate? {
        if (candidate.handlingMode != CaptureHandlingMode.AUTO ||
            candidate.state !in setOf(CaptureJobState.FAILED, CaptureJobState.PAUSED)
        ) return null
        return candidate.copy(
            state = CaptureJobState.QUEUED,
            identityId = identityId,
            errorMessage = null,
        )
    }

    fun mayDeletePrivateCopy(candidate: CaptureCandidate, completionPersisted: Boolean): Boolean =
        candidate.state == CaptureJobState.COMPLETED &&
            !candidate.noteId.isNullOrBlank() && completionPersisted

    fun pause(candidate: CaptureCandidate, message: String): CaptureCandidate =
        if (candidate.handlingMode == CaptureHandlingMode.AUTO &&
            candidate.state !in setOf(CaptureJobState.FAILED, CaptureJobState.COMPLETED)
        ) candidate.copy(state = CaptureJobState.PAUSED, errorMessage = message) else candidate
}

internal object CaptureAutomationStore {
    private const val PREFS = "ainote_capture"
    private const val AUTO_ENABLED = "auto_generation_enabled"
    private const val AUTO_CONSENT_EPOCH = "auto_generation_consent_epoch"
    private const val DEVICE_ID = "auto_generation_device_id"

    fun provisionDeviceId(context: Context, rawDeviceId: String?): Boolean {
        val deviceId = CaptureAutomationPolicy.normalizedDeviceId(rawDeviceId) ?: return false
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(DEVICE_ID, deviceId).commit()
    }

    fun deviceId(context: Context): String? = CaptureAutomationPolicy.normalizedDeviceId(
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(DEVICE_ID, null),
    )

    fun status(context: Context): CaptureAutomationStatus {
        val endpointAccepted = CaptureEndpointPolicy.acceptedBaseUrl(
            BuildConfig.API_BASE_URL,
            BuildConfig.DEBUG,
        ) != null
        val identityPresent = deviceId(context) != null
        val available = BuildConfig.GUEST_MODE && endpointAccepted && identityPresent
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val enabled = available && prefs.getBoolean(AUTO_ENABLED, false)
        val epoch = prefs.getLong(AUTO_CONSENT_EPOCH, 0L).takeIf { it > 0L }
        val message = when {
            !BuildConfig.GUEST_MODE -> "当前登录版本暂不支持后台自动生成"
            !endpointAccepted -> "当前服务地址不支持后台自动生成"
            !identityPresent -> "设备身份尚未准备好，请重新打开应用"
            else -> null
        }
        return CaptureAutomationStatus(available, enabled, epoch, message)
    }

    @Synchronized
    fun setEnabled(context: Context, enabled: Boolean): CaptureAutomationStatus {
        val current = status(context)
        if (enabled && !current.available) return current
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val editor = prefs.edit().putBoolean(AUTO_ENABLED, enabled)
        if (enabled && !prefs.getBoolean(AUTO_ENABLED, false)) {
            editor.putLong(AUTO_CONSENT_EPOCH, System.currentTimeMillis())
        }
        check(editor.commit()) { "无法保存自动生成设置" }
        return status(context)
    }
}
