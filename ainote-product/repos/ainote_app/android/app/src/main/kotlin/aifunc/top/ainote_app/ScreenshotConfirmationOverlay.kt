package aifunc.top.ainote_app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.ContextThemeWrapper
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

internal sealed interface ScreenshotOverlayModel {
    data class Candidate(val candidate: CaptureCandidate) : ScreenshotOverlayModel
    data class Completed(val noteId: String) : ScreenshotOverlayModel
}

/** Native status surface. Hiding it never acknowledges or cancels a task. */
internal class ScreenshotConfirmationOverlay(private val context: Context) {
    private val windowManager = context.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null
    private var renderedKey: String? = null

    fun show(model: ScreenshotOverlayModel): Boolean {
        val key = renderKey(model)
        if (overlayView != null && renderedKey == key) return true
        remove()
        val themedContext = ContextThemeWrapper(context, android.R.style.Theme_Material_Light_NoActionBar)
        val card = LinearLayout(themedContext).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(14), dp(18), dp(12))
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), Color.rgb(225, 228, 235))
            }
            elevation = dp(12).toFloat()
        }
        val titleRow = LinearLayout(themedContext).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        titleRow.addView(TextView(themedContext).apply {
            text = title(model)
            setTextColor(Color.rgb(24, 28, 36))
            textSize = 17f
            setTypeface(typeface, Typeface.BOLD)
        }, LinearLayout.LayoutParams(0, WindowManager.LayoutParams.WRAP_CONTENT, 1f))
        titleRow.addView(TextView(themedContext).apply {
            text = if (model is ScreenshotOverlayModel.Candidate &&
                model.candidate.handlingMode == CaptureHandlingMode.CONFIRM
            ) "关闭" else "隐藏到后台"
            contentDescription = "隐藏截图处理浮层，保留任务"
            setTextColor(Color.rgb(95, 101, 113))
            textSize = 14f
            setPadding(dp(12), dp(6), dp(2), dp(6))
            setOnClickListener { remove() }
        })
        card.addView(titleRow)
        card.addView(TextView(themedContext).apply {
            text = description(model)
            setTextColor(Color.rgb(82, 88, 100))
            textSize = 14f
            setLineSpacing(0f, 1.15f)
            setPadding(0, dp(7), 0, dp(8))
        })
        primaryAction(model)?.let { action ->
            card.addView(Button(themedContext).apply {
                text = action.first
                isAllCaps = false
                setOnClickListener { action.second.invoke() }
            }, LinearLayout.LayoutParams(WindowManager.LayoutParams.MATCH_PARENT, dp(44)))
        }
        val root = LinearLayout(themedContext).apply {
            setPadding(dp(14), 0, dp(14), 0)
            addView(card, LinearLayout.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
            ))
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP; y = dp(40) }
        return runCatching {
            windowManager.addView(root, params)
            overlayView = root
            renderedKey = key
            true
        }.getOrElse {
            overlayView = null
            renderedKey = null
            false
        }
    }

    fun remove() {
        val view = overlayView ?: return
        overlayView = null
        renderedKey = null
        runCatching { windowManager.removeViewImmediate(view) }
    }

    private fun title(model: ScreenshotOverlayModel): String = when (model) {
        is ScreenshotOverlayModel.Completed -> "备忘录已生成"
        is ScreenshotOverlayModel.Candidate -> when {
            model.candidate.handlingMode == CaptureHandlingMode.CONFIRM -> "发现新截图"
            model.candidate.state == CaptureJobState.UPLOADING -> "正在上传并生成"
            model.candidate.state == CaptureJobState.FAILED -> "生成失败"
            model.candidate.state == CaptureJobState.PAUSED -> "任务已暂停"
            else -> "正在本地识别"
        }
    }

    private fun description(model: ScreenshotOverlayModel): String = when (model) {
        is ScreenshotOverlayModel.Completed -> "这张截图已经生成备忘录，可以打开查看。"
        is ScreenshotOverlayModel.Candidate -> when {
            model.candidate.handlingMode == CaptureHandlingMode.CONFIRM ->
                "检测到一张可能的系统截图。无法判断它来自哪个应用；打开 AI备忘录后再确认，未确认不会识别或上传。"
            model.candidate.state == CaptureJobState.UPLOADING ->
                "本地文字识别已完成，正在上传图片并生成备忘录。"
            model.candidate.state == CaptureJobState.FAILED ->
                "${model.candidate.errorMessage ?: "处理失败"}。图片已保留，不会自动重复上传。"
            model.candidate.state == CaptureJobState.PAUSED ->
                model.candidate.errorMessage ?: "授权、身份或系统权限发生变化，处理已暂停。"
            else -> "正在设备本地识别截图文字，图片尚未上传。"
        }
    }

    private fun primaryAction(model: ScreenshotOverlayModel): Pair<String, () -> Unit>? = when (model) {
        is ScreenshotOverlayModel.Completed -> "打开备忘录" to { openApp(model.noteId) }
        is ScreenshotOverlayModel.Candidate -> when {
            model.candidate.handlingMode == CaptureHandlingMode.CONFIRM ->
                "打开确认" to { openApp(null) }
            model.candidate.state in setOf(CaptureJobState.FAILED, CaptureJobState.PAUSED) ->
                "重试" to { retry(model.candidate.id) }
            else -> null
        }
    }

    private fun openApp(noteId: String?) {
        remove()
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?: Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        noteId?.let { intent.putExtra(ScreenshotWatchService.EXTRA_OPEN_NOTE_ID, it) }
        runCatching { context.startActivity(intent) }
    }

    private fun retry(candidateId: String) {
        remove()
        runCatching {
            context.startService(
                Intent(context, ScreenshotWatchService::class.java)
                    .setAction(ScreenshotWatchService.ACTION_RETRY)
                    .putExtra(ScreenshotWatchService.EXTRA_CANDIDATE_ID, candidateId),
            )
        }
    }

    private fun renderKey(model: ScreenshotOverlayModel): String = when (model) {
        is ScreenshotOverlayModel.Completed -> "completed:${model.noteId}"
        is ScreenshotOverlayModel.Candidate -> with(model.candidate) {
            "$id:${handlingMode.wireValue}:${state.wireValue}:${errorMessage.orEmpty()}"
        }
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density + 0.5f).toInt()
}
