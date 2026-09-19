package aifunc.top.ainote_app

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

class CaptureBridge(
    private val activity: MainActivity,
    messenger: BinaryMessenger,
) : MethodChannel.MethodCallHandler {
    private val channel = MethodChannel(messenger, CHANNEL_NAME)
    private var pendingPermissionResult: MethodChannel.Result? = null
    private var pendingPickerResult: MethodChannel.Result? = null
    private var pendingWatchStartResult: MethodChannel.Result? = null
    private var waitingForOverlayReturn = false
    private var requestedOpenNoteId: String? = null
    private val ioExecutor = Executors.newSingleThreadExecutor()
    private val textRecognizer = OnDeviceImageTextRecognizer(activity.applicationContext)
    private val materializer = CaptureImageMaterializer(activity.applicationContext)
    private val eventListener: (CaptureCandidate) -> Unit = { candidate ->
        activity.runOnUiThread { channel.invokeMethod("captureCandidate", candidate.toMap()) }
    }
    private val failureListener: (CaptureFailure) -> Unit = { failure ->
        activity.runOnUiThread { channel.invokeMethod("captureError", failure.toMap()) }
    }

    fun attach() {
        channel.setMethodCallHandler(this)
        CaptureEventSink.attach(eventListener)
        CaptureFailureEventSink.attach(failureListener)
    }

    fun detach() {
        CaptureEventSink.detach(eventListener)
        CaptureFailureEventSink.detach(failureListener)
        channel.setMethodCallHandler(null)
        pendingPermissionResult?.error("activity_closed", "权限请求已取消", null)
        pendingPermissionResult = null
        pendingPickerResult?.error("activity_closed", "图片选择已取消", null)
        pendingPickerResult = null
        pendingWatchStartResult?.error("activity_closed", "截图监听启动已取消", null)
        pendingWatchStartResult = null
        ioExecutor.shutdownNow()
        textRecognizer.close()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "getStatus" -> result.success(currentStatus())
            "provisionCaptureIdentity" -> {
                val previous = CaptureAutomationStore.deviceId(activity)
                val accepted = CaptureAutomationStore.provisionDeviceId(
                    activity,
                    call.argument<String>("deviceId"),
                )
                if (!accepted) {
                    result.error("invalid_identity", "设备身份无效，后台自动生成不可用", null)
                } else {
                    val current = CaptureAutomationStore.deviceId(activity)
                    if (previous != null && previous != current) {
                        ScreenshotWatchService.onIdentityChanged()
                    }
                    if (permissionStatus().allGranted) {
                        ScreenshotWatchService.reconcileOnAppLaunch(activity)
                    }
                    result.success(CaptureAutomationStore.status(activity).toMap())
                }
            }
            "setAutoGenerationEnabled" -> {
                val enabled = call.argument<Boolean>("enabled") == true
                if (enabled && !ScreenshotWatchService.running) {
                    result.error("watch_required", "请先开启截图监听", null)
                } else {
                    runCatching { CaptureAutomationStore.setEnabled(activity, enabled) }
                        .onSuccess {
                            ScreenshotWatchService.onAutomationChanged()
                            result.success(it.toMap())
                        }
                        .onFailure { result.error("auto_setting_failed", it.message, null) }
                }
            }
            "peekAutoCompletion" -> {
                val requested = requestedOpenNoteId
                val completion = CaptureCompletionStore.select(activity, requested)
                if (requested != null && completion == null) {
                    requestedOpenNoteId = null
                }
                result.success(completion?.toMap())
            }
            "ackAutoCompletion" -> {
                val noteId = call.argument<String>("noteId").orEmpty()
                val consumed = CaptureCompletionStore.consume(activity, noteId)
                if (consumed && requestedOpenNoteId == noteId) requestedOpenNoteId = null
                ScreenshotWatchService.synchronizePendingUi()
                result.success(consumed)
            }
            "retryAutoCandidate" -> {
                val candidateId = call.argument<String>("id").orEmpty()
                result.success(ScreenshotWatchService.retryAutoCandidate(activity, candidateId))
            }
            "requestWatchPermissions" -> requestPermissions(result)
            "startScreenshotWatch" -> startWatch(result)
            "stopScreenshotWatch" -> stopWatch(result)
            "pickImage" -> pickImage(result)
            "materializeUri" -> {
                val rawUri = call.argument<String>("uri")
                val candidateId = call.argument<String>("id")
                val stored = candidateId?.let { CaptureCandidateStore.findById(activity, it) }
                if (rawUri.isNullOrBlank() || stored == null || stored.uri != rawUri) {
                    result.error("invalid_uri", "缺少图片地址", null)
                } else {
                    deliverMaterialized(Uri.parse(rawUri), stored.source, result, stored)
                }
            }
            "peekPendingCandidate", "consumePendingCandidate" ->
                result.success(CaptureCandidateStore.peekConfirmHead(activity)?.toMap())
            "peekPendingError" -> result.success(CaptureFailureStore.peek(activity)?.toMap())
            "ackPendingError" -> result.success(CaptureFailureStore.clear(activity))
            "recognizeCandidate" -> recognizeCandidate(call, result)
            "ackCandidate" -> {
                val acknowledged = CaptureCandidateStore.acknowledge(
                    activity,
                    call.argument<String>("id").orEmpty(),
                )
                if (acknowledged) ScreenshotWatchService.onCandidateAcknowledged()
                result.success(acknowledged)
            }
            else -> result.notImplemented()
        }
    }

    fun handleIntent(intent: Intent?) {
        intent?.getStringExtra(ScreenshotWatchService.EXTRA_OPEN_NOTE_ID)?.let { noteId ->
            requestedOpenNoteId = noteId
            intent.removeExtra(ScreenshotWatchService.EXTRA_OPEN_NOTE_ID)
            activity.runOnUiThread { channel.invokeMethod("autoCaptureUpdated", mapOf("noteId" to noteId)) }
        }
        if (intent?.action != Intent.ACTION_SEND || !intent.type.orEmpty().startsWith("image/")) return
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        } ?: return
        deliverMaterialized(uri, "share", null)
        intent.removeExtra(Intent.EXTRA_STREAM)
        intent.action = null
    }

    private fun requestPermissions(result: MethodChannel.Result) {
        if (pendingPermissionResult != null) {
            result.error("permission_pending", "已有权限请求正在进行", null)
            return
        }
        pendingPermissionResult = result
        val missingRuntimePermissions = missingRequiredRuntimePermissions()
        if (missingRuntimePermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                activity,
                requestableRuntimePermissions().toTypedArray(),
                REQUEST_CAPTURE_PERMISSIONS,
            )
            return
        }
        continuePermissionRequest()
    }

    private fun startWatch(result: MethodChannel.Result) {
        if (pendingWatchStartResult != null) {
            result.error("watch_start_pending", "截图监听正在启动", null)
            return
        }
        val status = permissionStatus()
        if (!status.allGranted) {
            result.error("permission_denied", status.denialMessage(), status.toMap())
            return
        }
        pendingWatchStartResult = result
        val shouldDispatch = ScreenshotWatchService.awaitReady { outcome ->
            activity.runOnUiThread {
                if (pendingWatchStartResult !== result) return@runOnUiThread
                pendingWatchStartResult = null
                outcome.onSuccess {
                    result.success(true)
                }.onFailure { error ->
                    result.error(
                        "watch_start_failed",
                        error.message ?: "截图监听无法启动",
                        null,
                    )
                }
            }
        }
        if (!shouldDispatch) return
        runCatching {
            ContextCompat.startForegroundService(
                activity,
                Intent(activity, ScreenshotWatchService::class.java)
                    .setAction(ScreenshotWatchService.ACTION_START),
            )
        }.onFailure { error ->
            ScreenshotWatchService.failStart(error)
        }
    }

    private fun stopWatch(result: MethodChannel.Result) {
        runCatching { CaptureAutomationStore.setEnabled(activity, false) }
            .onFailure {
                result.error("watch_stop_failed", "无法保存关闭状态，请稍后重试", null)
                return
            }
        ScreenshotWatchService.stopForPause(activity, "截图监听已关闭，任务已暂停")
        result.success(true)
    }

    private fun pickImage(result: MethodChannel.Result) {
        if (pendingPickerResult != null) {
            result.error("picker_pending", "图片选择器已经打开", null)
            return
        }
        pendingPickerResult = result
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    fun onRequestPermissionsResult(requestCode: Int): Boolean {
        if (requestCode != REQUEST_CAPTURE_PERMISSIONS) return false
        if (missingRequiredRuntimePermissions().isNotEmpty()) {
            finishPermissionRequest()
        } else {
            continuePermissionRequest()
        }
        return true
    }

    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            waitingForOverlayReturn = false
            finishPermissionRequest()
            return true
        }
        if (requestCode != REQUEST_PICK_IMAGE) return false
        val result = pendingPickerResult
        pendingPickerResult = null
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || uri == null) {
            result?.success(null)
            return true
        }
        runCatching {
            activity.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
        deliverMaterialized(uri, "picker", result)
        return true
    }

    fun onActivityResumed() {
        if (!waitingForOverlayReturn || pendingPermissionResult == null) return
        waitingForOverlayReturn = false
        finishPermissionRequest()
    }

    private fun continuePermissionRequest() {
        if (Settings.canDrawOverlays(activity)) {
            finishPermissionRequest()
            return
        }
        waitingForOverlayReturn = true
        runCatching {
            activity.startActivityForResult(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${activity.packageName}"),
                ),
                REQUEST_OVERLAY_PERMISSION,
            )
        }.onFailure { error ->
            waitingForOverlayReturn = false
            val result = pendingPermissionResult
            pendingPermissionResult = null
            result?.error(
                "overlay_settings_unavailable",
                error.message ?: "无法打开显示在其他应用上层的系统设置",
                permissionStatus().toMap(),
            )
        }
    }

    private fun finishPermissionRequest() {
        val result = pendingPermissionResult ?: return
        pendingPermissionResult = null
        waitingForOverlayReturn = false
        val status = permissionStatus()
        result.success(
            status.toMap() + mapOf(
                "granted" to status.allGranted,
                "message" to status.denialMessage(),
            ),
        )
    }

    private fun currentStatus(): Map<String, Any?> {
        val permissions = permissionStatus()
        if (ScreenshotWatchService.running && !permissions.allGranted) {
            ScreenshotWatchService.stopForPause(
                activity,
                "截图权限已变化，任务已暂停",
            )
        }
        return permissions.toMap() + CaptureAutomationStore.status(activity).toMap() + mapOf(
            "watchEnabled" to (ScreenshotWatchService.running && permissions.allGranted),
            "message" to permissions.denialMessage(),
            "autoJob" to CaptureCandidateStore.peekAutoHead(activity)?.toMap(),
        )
    }

    private fun permissionStatus() = CapturePermissionStatus(
        imageGranted = hasPermission(imagePermission()),
        notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            hasPermission(Manifest.permission.POST_NOTIFICATIONS),
        overlayGranted = Settings.canDrawOverlays(activity),
    )

    private fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED

    private fun missingRequiredRuntimePermissions(): List<String> =
        requiredPermissions().filterNot(::hasPermission)

    private fun requestableRuntimePermissions(): List<String> = buildList {
        val missing = missingRequiredRuntimePermissions()
        addAll(missing)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            imagePermission() in missing
        ) {
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        }
    }.distinct()

    private fun imagePermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun recognizeCandidate(call: MethodCall, result: MethodChannel.Result) {
        val candidateId = call.argument<String>("id").orEmpty()
        val candidate = CaptureCandidateStore.findById(activity, candidateId)
        val path = candidate?.localPath
        if (candidate == null || path.isNullOrBlank()) {
            result.error("candidate_unavailable", "图片已经无法读取，请重新选择", null)
            return
        }
        textRecognizer.recognize(
            path,
            onSuccess = { recognizedText ->
                val updated = candidate.copy(recognizedText = recognizedText)
                if (!CaptureCandidateStore.save(activity, updated)) {
                    result.error("queue_unavailable", "无法保存识别结果，请稍后重试", null)
                } else {
                    result.success(updated.toMap())
                }
            },
            onFailure = { error ->
                result.error("no_text", error.message ?: "图片文字识别失败", null)
            },
        )
    }

    private fun deliverMaterialized(
        uri: Uri,
        source: String,
        result: MethodChannel.Result?,
        storedCandidate: CaptureCandidate? = null,
    ) {
        ioExecutor.execute {
            val candidate = storedCandidate ?: CaptureCandidate(
                id = UUID.randomUUID().toString(),
                source = source,
                uri = uri.toString(),
            )
            val outcome = runCatching {
                materializer.materialize(candidate, fallbackMime = activity.intent?.type)
            }
            activity.runOnUiThread {
                outcome.onSuccess { candidate ->
                    if (!CaptureCandidateStore.save(activity, candidate)) {
                        candidate.localPath?.let { File(it).delete() }
                        deliverFailure(
                            source,
                            "未接收此图片：待处理队列已满。处理队列后请重新分享或选择，原图未删除",
                            result,
                        )
                    } else if (result == null) {
                        CaptureEventSink.emit(candidate)
                    } else {
                        result.success(candidate.toMap())
                    }
                }.onFailure { error ->
                    deliverFailure(source, error.message ?: "无法读取所选图片", result)
                }
            }
        }
    }

    private fun deliverFailure(source: String, message: String, result: MethodChannel.Result?) {
        if (result != null) {
            result.error("image_unavailable", message, null)
        } else {
            val failure = CaptureFailure(source, message)
            CaptureFailureStore.save(activity, failure)
            CaptureFailureEventSink.emit(failure)
        }
    }

    private fun requiredPermissions(): List<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(imagePermission())
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(imagePermission())
        }
    }

    companion object {
        private const val CHANNEL_NAME = "ainote/capture"
        private const val REQUEST_CAPTURE_PERMISSIONS = 41020
        private const val REQUEST_PICK_IMAGE = 41021
        private const val REQUEST_OVERLAY_PERMISSION = 41022
    }
}
