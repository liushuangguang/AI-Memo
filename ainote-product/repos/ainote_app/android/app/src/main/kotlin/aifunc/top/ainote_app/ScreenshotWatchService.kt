package aifunc.top.ainote_app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.UUID
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors

class ScreenshotWatchService : Service() {
    private val filter = ScreenshotCandidateFilter()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val workExecutor = Executors.newSingleThreadExecutor()
    private val uploader = NativeCaptureUploader()
    private var observer: ContentObserver? = null
    private var processingCandidateId: String? = null
    private var destroyed = false
    private lateinit var textRecognizer: OnDeviceImageTextRecognizer
    private lateinit var materializer: CaptureImageMaterializer
    private lateinit var confirmationOverlay: ScreenshotConfirmationOverlay
    private lateinit var pendingUiCoordinator: ScreenshotPendingUiCoordinator

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        textRecognizer = OnDeviceImageTextRecognizer(applicationContext)
        materializer = CaptureImageMaterializer(applicationContext)
        confirmationOverlay = ScreenshotConfirmationOverlay(this)
        pendingUiCoordinator = ScreenshotPendingUiCoordinator(
            hasPending = { pendingUiModel() != null },
            showOverlay = {
                val model = pendingUiModel()
                model == null || confirmationOverlay.show(model)
            },
            removeOverlay = { confirmationOverlay.remove() },
            updateNotification = {
                getSystemService(NotificationManager::class.java)
                    .notify(NOTIFICATION_ID, buildNotification())
            },
            onOverlayFailure = { recordOverlayFailure() },
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                runCatching { CaptureAutomationStore.setEnabled(this, false) }
                pauseForExternalChange("截图监听已关闭，任务已暂停")
                startupGate.markStopped()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RETRY -> {
                retryAutoCandidate(this, intent.getStringExtra(EXTRA_CANDIDATE_ID).orEmpty())
                return START_REDELIVER_INTENT
            }
            ACTION_START, null -> Unit
            else -> {
                startupGate.markFailed(IllegalStateException("截图监听启动指令无效"))
                stopSelfResult(startId)
                return START_NOT_STICKY
            }
        }
        if (!startupGate.prepareForServiceStart()) return START_REDELIVER_INTENT
        initializeWatch(startId)
        return START_REDELIVER_INTENT
    }

    private fun initializeWatch(startId: Int) {
        runCatching {
            createNotificationChannel()
            startForeground(NOTIFICATION_ID, buildNotification())
            check(hasRequiredPermissions()) { "截图监听权限已失效" }
            registerObserver()
            check(hasRequiredPermissions()) { "截图监听权限已失效" }
            check(CaptureCandidateStore.recoverInterruptedAutoJobs(this)) { "无法恢复截图任务" }
            check(pendingUiCoordinator.synchronize(hostActivityResumed)) { "系统未能显示截图处理浮层" }
        }.onSuccess {
            if (startupGate.markReady()) {
                CaptureCandidateStore.peek(this)?.let(CaptureEventSink::emit)
                scheduleAutoProcessing()
            } else stopSelfResult(startId)
        }.onFailure { error ->
            pauseForExternalChange("截图监听或系统权限异常，任务已暂停")
            startupGate.markFailed(error)
            stopSelfResult(startId)
        }
    }

    override fun onDestroy() {
        destroyed = true
        observer?.let { runCatching { contentResolver.unregisterContentObserver(it) } }
        observer = null
        uploader.cancel()
        workExecutor.shutdownNow()
        if (::textRecognizer.isInitialized) textRecognizer.close()
        if (::confirmationOverlay.isInitialized) confirmationOverlay.remove()
        if (activeInstance === this) activeInstance = null
        startupGate.markServiceStopped()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun registerObserver() {
        if (observer != null) return
        observer = object : ContentObserver(mainHandler) {
            override fun onChange(selfChange: Boolean, uri: Uri?) = inspect(uri)
        }.also {
            contentResolver.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                it,
            )
        }
    }

    private fun inspect(changedUri: Uri?) {
        if (!hasRequiredPermissions()) {
            pauseHead("系统权限已变化，请重新开启截图监听后重试")
            startupGate.markStopped()
            stopSelf()
            return
        }
        val queryUri = changedUri?.takeIf(::isMediaImageUri)
            ?: MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            add(MediaStore.Images.Media.DATE_TAKEN)
            add(MediaStore.Images.Media.SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Images.Media.RELATIVE_PATH)
        }.toTypedArray()
        runCatching {
            contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val bucket = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME))
                val relativePath = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                        .takeIf { it >= 0 }?.let(cursor::getString)
                } else null
                val added = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                val taken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                val itemUri = if (queryUri.lastPathSegment?.toLongOrNull() != null) queryUri
                else Uri.withAppendedPath(queryUri, id.toString())
                val observedAtSeconds = added.takeIf { it > 0L } ?: (taken / 1_000L)
                if (!filter.shouldAccept(
                        itemUri.toString(),
                        name,
                        listOfNotNull(bucket, relativePath).joinToString("/"),
                        observedAtSeconds,
                        size,
                    )
                ) return@use
                val captureTimeMillis = CaptureAutomationPolicy.conservativeCaptureTimeMillis(
                    added,
                    taken,
                )
                acceptDetectedScreenshot(itemUri, name, captureTimeMillis)
            }
        }.onFailure { error ->
            if (error is SecurityException) {
                pauseHead("照片权限已失效，请重新开启截图监听后重试")
                deliverFailure("照片权限已失效，截图监听已停止；请回到设置重新开启")
                startupGate.markStopped()
                stopSelf()
            }
        }
    }

    private fun acceptDetectedScreenshot(uri: Uri, name: String?, captureTimeMillis: Long?) {
        val automation = CaptureAutomationStore.status(this)
        val mode = CaptureAutomationPolicy.modeForDetectedScreenshot(
            automation.enabled,
            automation.consentEpoch,
            captureTimeMillis,
        )
        val candidate = CaptureCandidate(
            id = UUID.randomUUID().toString(),
            source = "screenshot",
            uri = uri.toString(),
            displayName = name,
            handlingMode = mode,
            autoConsentEpoch = automation.consentEpoch.takeIf { mode == CaptureHandlingMode.AUTO },
            identityId = CaptureAutomationStore.deviceId(this).takeIf { mode == CaptureHandlingMode.AUTO },
        )
        workExecutor.execute {
            val outcome = runCatching { materializer.materialize(candidate) }
            mainHandler.post {
                if (destroyed) return@post
                outcome.onSuccess { materialized ->
                    if (!CaptureCandidateStore.save(this, materialized)) {
                        materialized.localPath?.let { File(it).delete() }
                        deliverFailure("未接收此图片：待处理队列已满。处理队列后请重新截图，原图未删除")
                    } else {
                        CaptureEventSink.emit(materialized)
                        synchronizePendingUiInternal()
                        scheduleAutoProcessing()
                    }
                }.onFailure { error -> deliverFailure(error.message ?: "无法读取新截图，原图未删除") }
            }
        }
    }

    private fun scheduleAutoProcessing() = mainHandler.post(::startNextAutoIfPossible)

    private fun startNextAutoIfPossible() {
        if (destroyed || processingCandidateId != null) return
        val candidate = CaptureCandidateStore.peekAutoHead(this) ?: run {
            synchronizePendingUiInternal()
            return
        }
        when (candidate.state) {
            CaptureJobState.FAILED, CaptureJobState.PAUSED -> {
                synchronizePendingUiInternal()
                return
            }
            CaptureJobState.COMPLETED -> {
                finalizeCompleted(candidate)
                return
            }
            else -> Unit
        }
        val automation = CaptureAutomationStore.status(this)
        val identity = CaptureAutomationStore.deviceId(this)
        val endpoint = CaptureEndpointPolicy.acceptedBaseUrl(BuildConfig.API_BASE_URL, BuildConfig.DEBUG)
        val pauseMessage = when {
            !automation.enabled -> "自动生成已关闭，任务已暂停"
            endpoint == null || !BuildConfig.GUEST_MODE -> "当前版本不支持后台自动生成"
            !hasRequiredPermissions() -> "系统权限已变化，请重新开启截图监听后重试"
            identity == null || identity != candidate.identityId -> "设备身份已变化，请明确重试此任务"
            else -> null
        }
        if (pauseMessage != null) {
            pauseCandidate(candidate, pauseMessage)
            return
        }
        processingCandidateId = candidate.id
        workExecutor.execute {
            val outcome = runCatching { materializer.materialize(candidate) }
            mainHandler.post {
                if (destroyed || processingCandidateId != candidate.id) return@post
                outcome.onSuccess(::startRecognition)
                    .onFailure { failCandidate(candidate, it.message ?: "无法读取截图") }
            }
        }
    }

    private fun startRecognition(candidate: CaptureCandidate) {
        val recognizing = candidate.copy(state = CaptureJobState.OCR, errorMessage = null)
        if (!CaptureCandidateStore.save(this, recognizing)) {
            failCandidate(candidate, "无法保存本地识别状态")
            return
        }
        CaptureEventSink.emit(recognizing)
        synchronizePendingUiInternal()
        textRecognizer.recognize(
            requireNotNull(recognizing.localPath),
            onSuccess = { text -> mainHandler.post { onRecognitionComplete(recognizing.id, text) } },
            onFailure = { error -> mainHandler.post { failCandidate(recognizing, error.message ?: "图片文字识别失败") } },
        )
    }

    private fun onRecognitionComplete(candidateId: String, recognizedText: String) {
        if (destroyed || processingCandidateId != candidateId) return
        val candidate = CaptureCandidateStore.findById(this, candidateId) ?: return finishProcessing()
        val identity = CaptureAutomationStore.deviceId(this)
        val endpoint = CaptureEndpointPolicy.acceptedBaseUrl(BuildConfig.API_BASE_URL, BuildConfig.DEBUG)
        if (!CaptureAutomationStore.status(this).enabled || !hasRequiredPermissions() ||
            identity == null || identity != candidate.identityId || endpoint == null || !BuildConfig.GUEST_MODE
        ) {
            pauseCandidate(candidate, "授权、身份或系统权限已变化，请明确重试此任务")
            return
        }
        val uploading = candidate.copy(
            recognizedText = recognizedText,
            state = CaptureJobState.UPLOADING,
            errorMessage = null,
        )
        if (!CaptureCandidateStore.save(this, uploading)) {
            failCandidate(candidate, "无法保存上传状态")
            return
        }
        CaptureEventSink.emit(uploading)
        synchronizePendingUiInternal()
        workExecutor.execute {
            val outcome = runCatching { uploader.upload(uploading, identity, endpoint) }
            mainHandler.post {
                if (destroyed || processingCandidateId != candidateId) return@post
                outcome.onSuccess { noteId -> completeCandidate(uploading, noteId) }
                    .onFailure { error -> failCandidate(uploading, error.message ?: "上传生成失败") }
            }
        }
    }

    private fun completeCandidate(candidate: CaptureCandidate, noteId: String) {
        val completed = candidate.copy(
            state = CaptureJobState.COMPLETED,
            noteId = noteId,
            errorMessage = null,
        )
        if (!CaptureCandidateStore.save(this, completed)) {
            failCandidate(candidate, "备忘录已生成，但完成状态保存失败；图片仍保留")
            return
        }
        val completionPersisted = CaptureCompletionStore.save(this, CaptureCompletion(candidate.id, noteId))
        if (!CaptureAutoTransition.mayDeletePrivateCopy(completed, completionPersisted)) {
            processingCandidateId = null
            synchronizePendingUiInternal()
            return
        }
        finalizeCompleted(completed)
    }

    private fun finalizeCompleted(candidate: CaptureCandidate) {
        val noteId = candidate.noteId ?: return
        if (!CaptureCompletionStore.save(this, CaptureCompletion(candidate.id, noteId))) return
        if (CaptureCandidateStore.acknowledge(this, candidate.id)) {
            processingCandidateId = null
            CaptureEventSink.emit(candidate)
            synchronizePendingUiInternal()
            scheduleAutoProcessing()
        }
    }

    private fun failCandidate(candidate: CaptureCandidate, message: String) {
        if (destroyed) return
        val current = CaptureCandidateStore.findById(this, candidate.id) ?: candidate
        val failed = current.copy(state = CaptureJobState.FAILED, errorMessage = message)
        CaptureCandidateStore.save(this, failed)
        processingCandidateId = null
        CaptureEventSink.emit(failed)
        synchronizePendingUiInternal()
    }

    private fun pauseHead(message: String) {
        CaptureCandidateStore.peekAutoHead(this)?.let { pauseCandidate(it, message) }
    }

    private fun pauseCandidate(candidate: CaptureCandidate, message: String) {
        val paused = CaptureAutoTransition.pause(candidate, message)
        if (paused == candidate) return
        CaptureCandidateStore.save(this, paused)
        processingCandidateId = null
        CaptureEventSink.emit(paused)
        synchronizePendingUiInternal()
    }

    private fun finishProcessing() {
        processingCandidateId = null
        synchronizePendingUiInternal()
    }

    private fun pauseForExternalChange(message: String) {
        uploader.cancel()
        val candidate = processingCandidateId?.let { CaptureCandidateStore.findById(this, it) }
            ?: CaptureCandidateStore.peekAutoHead(this)
        if (candidate != null && candidate.state !in setOf(CaptureJobState.FAILED, CaptureJobState.COMPLETED)) {
            pauseCandidate(candidate, message)
        }
    }

    private fun pendingUiModel(): ScreenshotOverlayModel? {
        CaptureCompletionStore.peek(this)?.let { return ScreenshotOverlayModel.Completed(it.noteId) }
        return CaptureCandidateStore.peek(this)?.let(ScreenshotOverlayModel::Candidate)
    }

    private fun hasRequiredPermissions(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else Manifest.permission.READ_EXTERNAL_STORAGE
        val imageGranted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        return imageGranted && notificationGranted && Settings.canDrawOverlays(this)
    }

    private fun isMediaImageUri(uri: Uri): Boolean {
        val segments = uri.pathSegments
        val imageIndex = segments.indexOf("images")
        return uri.scheme == "content" && uri.authority == MediaStore.AUTHORITY &&
            imageIndex >= 0 && segments.getOrNull(imageIndex + 1) == "media"
    }

    private fun synchronizePendingUiInternal() {
        if (!::pendingUiCoordinator.isInitialized) return
        if (!pendingUiCoordinator.synchronize(hostActivityResumed)) stopAfterUiFailure()
    }

    private fun onCandidateAcknowledgedInternal() {
        if (!::pendingUiCoordinator.isInitialized) return
        if (!pendingUiCoordinator.onCandidateAcknowledged(hostActivityResumed)) stopAfterUiFailure()
        scheduleAutoProcessing()
    }

    private fun deliverFailure(message: String) {
        val failure = CaptureFailure("screenshot", message)
        CaptureFailureStore.save(this, failure)
        CaptureFailureEventSink.emit(failure)
    }

    private fun recordOverlayFailure() {
        deliverFailure("系统未能显示截图处理浮层，监听已停止；任务已保留，可回到应用后处理")
    }

    private fun stopAfterUiFailure() {
        pauseForExternalChange("系统浮层不可用，任务已暂停")
        startupGate.markStopped()
        stopSelf()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "截图识别", NotificationManager.IMPORTANCE_LOW).apply {
                description = "截图监听与已授权的后台生成进行时持续显示，可随时停止"
                setShowBadge(false)
            },
        )
    }

    private fun buildNotification(): android.app.Notification {
        val completion = CaptureCompletionStore.peek(this)
        val candidate = CaptureCandidateStore.peek(this)
        val (title, body) = when {
            completion != null -> "备忘录已生成" to "点此打开刚生成的备忘录"
            candidate?.handlingMode == CaptureHandlingMode.AUTO -> when (candidate.state) {
                CaptureJobState.OCR -> "正在本地识别" to "图片尚未上传"
                CaptureJobState.UPLOADING -> "正在上传并生成" to "请保持网络可用"
                CaptureJobState.FAILED -> "截图生成失败" to "打开后可明确重试"
                CaptureJobState.PAUSED -> "截图任务已暂停" to (candidate.errorMessage ?: "回到应用处理")
                else -> "准备识别新截图" to "已按你的授权开始处理"
            }
            candidate != null -> "发现新截图" to "点此确认是否生成备忘录"
            else -> "截图识别已开启" to if (CaptureAutomationStore.status(this).enabled) {
                "只自动处理授权后发现的新截图"
            } else "新截图需要逐张确认"
        }
        val openIntent = (packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java)).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            completion?.let { putExtra(EXTRA_OPEN_NOTE_ID, it.noteId) }
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setContentIntent(PendingIntent.getActivity(this, 0, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .addAction(
                0,
                "停止监听",
                PendingIntent.getService(
                    this,
                    1,
                    Intent(this, ScreenshotWatchService::class.java).setAction(ACTION_STOP),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .build()
    }

    companion object {
        const val ACTION_START = "aifunc.top.ainote_app.capture.START"
        const val ACTION_STOP = "aifunc.top.ainote_app.capture.STOP"
        const val ACTION_RETRY = "aifunc.top.ainote_app.capture.RETRY"
        const val EXTRA_CANDIDATE_ID = "capture_candidate_id"
        const val EXTRA_OPEN_NOTE_ID = "capture_note_id"
        private const val CHANNEL_ID = "screenshot_capture"
        private const val NOTIFICATION_ID = 4102
        private val startupGate = ScreenshotWatchStartupGate()

        @Volatile private var activeInstance: ScreenshotWatchService? = null
        @Volatile private var hostActivityResumed: Boolean = false

        val running: Boolean get() = startupGate.isReady
        fun awaitReady(callback: (Result<Unit>) -> Unit): Boolean = startupGate.begin(callback)
        fun failStart(error: Throwable) = startupGate.markFailed(error)
        fun markStopRequested() = startupGate.markStopped()

        fun stopForPause(context: android.content.Context, message: String) {
            activeInstance?.pauseForExternalChange(message)
            startupGate.markStopped()
            context.stopService(Intent(context, ScreenshotWatchService::class.java))
        }

        fun reconcileOnAppLaunch(context: android.content.Context) {
            if (running || !CaptureAutomationStore.status(context).enabled) return
            val shouldDispatch = startupGate.begin { }
            if (!shouldDispatch) return
            runCatching {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, ScreenshotWatchService::class.java).setAction(ACTION_START),
                )
            }.onFailure(startupGate::markFailed)
        }

        fun onHostActivityResumed() {
            hostActivityResumed = true
            synchronizePendingUi()
            activeInstance?.scheduleAutoProcessing()
        }

        fun onHostActivityPaused() { hostActivityResumed = false }

        fun onIdentityChanged() {
            val service = activeInstance ?: return
            service.mainHandler.post {
                service.pauseForExternalChange("设备身份已变化，请明确重试此任务")
            }
        }

        fun onAutomationChanged() {
            val service = activeInstance ?: return
            service.mainHandler.post {
                if (CaptureAutomationStore.status(service).enabled) service.scheduleAutoProcessing()
                else service.pauseForExternalChange("自动生成已关闭，任务已暂停")
                service.synchronizePendingUiInternal()
            }
        }

        fun retryAutoCandidate(context: android.content.Context, candidateId: String): Boolean {
            val automation = CaptureAutomationStore.status(context)
            val permissionsGranted = hasRequiredPermissions(context)
            val candidate = CaptureCandidateStore.findById(context, candidateId)
            if (!CaptureAutomationPolicy.canRetry(
                    candidate,
                    automation.enabled,
                    automation.available,
                    permissionsGranted,
                    running && activeInstance != null,
                )
            ) return false
            val identity = CaptureAutomationStore.deviceId(context) ?: return false
            val service = activeInstance ?: return false
            val retried = CaptureCandidateStore.retryAuto(context, candidateId, identity)
            if (retried) service.scheduleAutoProcessing()
            return retried
        }

        private fun hasRequiredPermissions(context: android.content.Context): Boolean {
            val imagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else Manifest.permission.READ_EXTERNAL_STORAGE
            val imageGranted = ContextCompat.checkSelfPermission(context, imagePermission) == PackageManager.PERMISSION_GRANTED
            val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            return imageGranted && notificationGranted && Settings.canDrawOverlays(context)
        }

        fun synchronizePendingUi() {
            val service = activeInstance ?: return
            if (Looper.myLooper() == Looper.getMainLooper()) service.synchronizePendingUiInternal()
            else Handler(Looper.getMainLooper()).post(service::synchronizePendingUiInternal)
        }

        fun onCandidateAcknowledged() {
            val service = activeInstance ?: return
            if (Looper.myLooper() == Looper.getMainLooper()) service.onCandidateAcknowledgedInternal()
            else Handler(Looper.getMainLooper()).post(service::onCandidateAcknowledgedInternal)
        }
    }
}

internal class ScreenshotPendingUiCoordinator(
    private val hasPending: () -> Boolean,
    private val showOverlay: () -> Boolean,
    private val removeOverlay: () -> Unit,
    private val updateNotification: (Boolean) -> Unit,
    private val onOverlayFailure: () -> Unit,
) {
    fun onCandidateAcknowledged(activityForeground: Boolean): Boolean {
        removeOverlay()
        return reconcile(activityForeground, overlayAlreadyRemoved = true)
    }

    fun synchronize(activityForeground: Boolean): Boolean =
        reconcile(activityForeground, overlayAlreadyRemoved = false)

    private fun reconcile(activityForeground: Boolean, overlayAlreadyRemoved: Boolean): Boolean {
        val pending = hasPending()
        if (!pending || activityForeground) {
            if (!overlayAlreadyRemoved) removeOverlay()
        } else if (!showOverlay()) {
            onOverlayFailure()
            return false
        }
        updateNotification(pending)
        return true
    }
}

internal class ScreenshotWatchStartupGate {
    private enum class State { STOPPED, STARTING, READY }
    private val waiters = mutableListOf<(Result<Unit>) -> Unit>()
    private var state = State.STOPPED
    val isReady: Boolean @Synchronized get() = state == State.READY

    @Synchronized
    fun prepareForServiceStart(): Boolean = when (state) {
        State.STOPPED -> { state = State.STARTING; true }
        State.STARTING -> true
        State.READY -> false
    }

    fun begin(callback: (Result<Unit>) -> Unit): Boolean {
        var completeImmediately = false
        val shouldDispatch = synchronized(this) {
            if (state == State.READY) {
                completeImmediately = true
                false
            } else {
                waiters += callback
                if (state == State.STOPPED) { state = State.STARTING; true } else false
            }
        }
        if (completeImmediately) callback(Result.success(Unit))
        return shouldDispatch
    }

    fun markReady(): Boolean {
        val callbacks = synchronized(this) {
            if (state != State.STARTING) return false
            state = State.READY
            waiters.toList().also { waiters.clear() }
        }
        callbacks.forEach { it(Result.success(Unit)) }
        return true
    }

    fun markFailed(error: Throwable) = completeStopped(Result.failure(error))
    fun markStopped() = completeStopped(Result.failure(CancellationException("截图监听已停止")))
    fun markServiceStopped() { synchronized(this) { if (state == State.READY) state = State.STOPPED } }

    private fun completeStopped(result: Result<Unit>) {
        val callbacks = synchronized(this) {
            state = State.STOPPED
            waiters.toList().also { waiters.clear() }
        }
        callbacks.forEach { it(result) }
    }
}
