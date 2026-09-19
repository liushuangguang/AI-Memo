package aifunc.top.ainote_app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.util.Locale

/**
 * Android system speech recognition and TTS bridge for voice discussion.
 *
 * This class never uploads or logs recognized text. Speech recognition and TTS are delegated to
 * providers installed on the device. [disposeSession] releases per-discussion activity while
 * [destroy] is reserved for Activity teardown.
 */
class VoiceBridge private constructor(
    private val activity: Activity,
    messenger: BinaryMessenger,
) : MethodChannel.MethodCallHandler, TextToSpeech.OnInitListener {
    private val channel = MethodChannel(messenger, CHANNEL_NAME)
    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var destroyed = false
    private var activeSessionId = -1L
    private var pendingPermissionResult: MethodChannel.Result? = null
    private var pendingPermissionSessionId: Long? = null

    init {
        channel.setMethodCallHandler(this)
        textToSpeech = TextToSpeech(activity.applicationContext, this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        if (destroyed) {
            result.error("bridge_destroyed", "Voice bridge is no longer available", null)
            return
        }
        val sessionId = call.argument<Number>("sessionId")?.toLong()
        if (sessionId == null) {
            result.error("voice_session_required", "Voice session id is required", null)
            return
        }
        val isCurrent = claimSession(sessionId)
        when (call.method) {
            "capabilities" -> result.success(capabilities())
            "requestMicrophonePermission" -> {
                if (isCurrent) requestMicrophonePermission(sessionId, result) else result.success(false)
            }
            "startListening" -> {
                if (isCurrent) startListening(sessionId, result) else result.success(null)
            }
            "stopListening" -> {
                if (isCurrent) recognizer?.stopListening()
                result.success(null)
            }
            "cancelListening" -> {
                if (isCurrent) recognizer?.cancel()
                result.success(null)
            }
            "speak" -> {
                if (isCurrent) speak(sessionId, call.argument<String>("text"), result)
                else result.success(null)
            }
            "stopSpeaking" -> {
                if (isCurrent) {
                    textToSpeech?.stop()
                    emitTts(sessionId, false)
                }
                result.success(null)
            }
            "dispose" -> {
                if (isCurrent) disposeSession(sessionId)
                result.success(null)
            }
            else -> result.notImplemented()
        }
    }

    private fun claimSession(sessionId: Long): Boolean {
        if (sessionId < activeSessionId) return false
        if (sessionId > activeSessionId) {
            disposeVoiceActivity(activeSessionId)
            activeSessionId = sessionId
        }
        return true
    }

    private fun capabilities(): Map<String, Any?> {
        val speechAvailable = SpeechRecognizer.isRecognitionAvailable(activity)
        val hasPermission = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.RECORD_AUDIO,
        ) == PackageManager.PERMISSION_GRANTED
        val reason = when {
            !speechAvailable -> "系统没有可用的语音识别服务，可直接输入文字"
            !hasPermission -> "请在系统设置中允许麦克风权限，或直接输入文字"
            else -> null
        }
        return mapOf(
            "speechAvailable" to speechAvailable,
            "microphonePermission" to hasPermission,
            "ttsAvailable" to ttsReady,
            "reason" to reason,
        )
    }

    private fun startListening(sessionId: Long, result: MethodChannel.Result) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            result.error(
                "microphone_permission_required",
                "Microphone permission is required. It can be enabled in system settings.",
                null,
            )
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(activity)) {
            result.error(
                "speech_service_unavailable",
                "No Android speech recognition provider is available",
                null,
            )
            return
        }

        try {
            val activeRecognizer = recognizer ?: SpeechRecognizer.createSpeechRecognizer(activity)
                .also {
                    it.setRecognitionListener(recognitionListener(sessionId))
                    recognizer = it
                }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            activeRecognizer.startListening(intent)
            result.success(null)
        } catch (_: RuntimeException) {
            result.error(
                "speech_service_unavailable",
                "Android speech recognition could not be started",
                null,
            )
        }
    }

    private fun requestMicrophonePermission(sessionId: Long, result: MethodChannel.Result) {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            result.success(true)
            return
        }
        if (pendingPermissionResult != null) {
            if (pendingPermissionSessionId == sessionId) {
                result.error("permission_pending", "A microphone permission request is already open", null)
            } else {
                pendingPermissionResult?.error(
                    "voice_session_replaced",
                    "A newer voice session replaced the permission request",
                    null,
                )
                pendingPermissionResult = result
                pendingPermissionSessionId = sessionId
            }
            return
        }
        pendingPermissionResult = result
        pendingPermissionSessionId = sessionId
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQUEST_MICROPHONE_PERMISSION,
        )
    }

    fun onRequestPermissionsResult(requestCode: Int, grantResults: IntArray): Boolean {
        if (requestCode != REQUEST_MICROPHONE_PERMISSION) return false
        val pending = pendingPermissionResult
        val pendingSession = pendingPermissionSessionId
        pendingPermissionResult = null
        pendingPermissionSessionId = null
        if (pendingSession == activeSessionId) {
            pending?.success(grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
        } else {
            pending?.error("voice_session_replaced", "Voice session is no longer active", null)
        }
        return true
    }

    private fun speak(sessionId: Long, text: String?, result: MethodChannel.Result) {
        val normalized = text?.trim().orEmpty()
        if (normalized.isEmpty()) {
            result.error("tts_text_required", "Text to speak is required", null)
            return
        }
        val engine = textToSpeech
        if (!ttsReady || engine == null) {
            result.error("tts_unavailable", "Android text to speech is not ready", null)
            return
        }
        val status = engine.speak(
            normalized,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "$UTTERANCE_ID_PREFIX$sessionId",
        )
        if (status == TextToSpeech.ERROR) {
            result.error("tts_failed", "Android text to speech could not start", null)
            return
        }
        result.success(null)
    }

    override fun onInit(status: Int) {
        val engine = textToSpeech
        if (status != TextToSpeech.SUCCESS || engine == null) {
            ttsReady = false
            return
        }
        val languageStatus = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
        ttsReady = languageStatus != TextToSpeech.LANG_MISSING_DATA &&
            languageStatus != TextToSpeech.LANG_NOT_SUPPORTED
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) = emitTts(utteranceSession(utteranceId), true)

            override fun onDone(utteranceId: String?) = emitTts(utteranceSession(utteranceId), false)

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) = emitTts(utteranceSession(utteranceId), false)

            override fun onError(utteranceId: String?, errorCode: Int) =
                emitTts(utteranceSession(utteranceId), false)

            override fun onStop(utteranceId: String?, interrupted: Boolean) =
                emitTts(utteranceSession(utteranceId), false)
        })
    }

    private fun recognitionListener(sessionId: Long) = object : RecognitionListener {
        override fun onResults(results: Bundle?) =
            emitSpeech(sessionId, firstResult(results), true, null)

        override fun onPartialResults(partialResults: Bundle?) =
            emitSpeech(sessionId, firstResult(partialResults), false, null)

        override fun onError(error: Int) =
            emitSpeech(sessionId, "", true, speechErrorCode(error))

        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun firstResult(bundle: Bundle?): String =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            ?.firstOrNull()
            .orEmpty()

    private fun speechErrorCode(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "microphone_permission_required"
        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "no_match"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "recognizer_busy"
        SpeechRecognizer.ERROR_CLIENT -> "cancelled"
        else -> "speech_service_unavailable"
    }

    private fun emitSpeech(sessionId: Long, text: String, isFinal: Boolean, errorCode: String?) {
        activity.runOnUiThread {
            if (!destroyed && sessionId == activeSessionId) {
                channel.invokeMethod(
                    "onSpeechEvent",
                    mapOf(
                        "sessionId" to sessionId,
                        "text" to text,
                        "final" to isFinal,
                        "errorCode" to errorCode,
                    ),
                )
            }
        }
    }

    private fun emitTts(sessionId: Long, speaking: Boolean) {
        activity.runOnUiThread {
            if (!destroyed && sessionId == activeSessionId) {
                channel.invokeMethod(
                    "onTtsEvent",
                    mapOf("sessionId" to sessionId, "speaking" to speaking),
                )
            }
        }
    }

    private fun disposeSession(sessionId: Long) {
        disposeVoiceActivity(sessionId)
        if (pendingPermissionSessionId == sessionId) {
            pendingPermissionResult?.error(
                "voice_session_closed",
                "Voice session closed before permission completed",
                null,
            )
            pendingPermissionResult = null
            pendingPermissionSessionId = null
        }
    }

    private fun disposeVoiceActivity(sessionId: Long) {
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        textToSpeech?.stop()
        emitTts(sessionId, false)
    }

    fun destroy() {
        if (destroyed) return
        pendingPermissionResult?.error(
            "bridge_destroyed",
            "Voice bridge was destroyed before permission completed",
            null,
        )
        pendingPermissionResult = null
        pendingPermissionSessionId = null
        disposeVoiceActivity(activeSessionId)
        destroyed = true
        textToSpeech?.shutdown()
        textToSpeech = null
        ttsReady = false
        channel.setMethodCallHandler(null)
    }

    private fun utteranceSession(utteranceId: String?): Long =
        utteranceId
            ?.removePrefix(UTTERANCE_ID_PREFIX)
            ?.toLongOrNull()
            ?: -1L

    companion object {
        private const val CHANNEL_NAME = "ainote/voice_bridge"
        private const val UTTERANCE_ID_PREFIX = "ainote_voice_discussion_"
        private const val REQUEST_MICROPHONE_PERMISSION = 9042

        @JvmStatic
        fun register(activity: Activity, messenger: BinaryMessenger): VoiceBridge =
            VoiceBridge(activity, messenger)
    }
}
