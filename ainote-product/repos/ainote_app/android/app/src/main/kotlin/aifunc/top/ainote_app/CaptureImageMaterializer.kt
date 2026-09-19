package aifunc.top.ainote_app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal class CaptureImageMaterializer(private val context: Context) {
    fun materialize(candidate: CaptureCandidate, fallbackMime: String? = null): CaptureCandidate {
        candidate.localPath?.let { existing ->
            if (File(existing).isFile) return candidate
        }
        val rawUri = requireNotNull(candidate.uri) { "缺少图片地址" }
        val uri = Uri.parse(rawUri)
        val resolver = context.contentResolver
        val declaredMime = resolver.getType(uri) ?: fallbackMime
        require(declaredMime?.startsWith("image/") == true) { "仅支持图片文件" }
        val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        val directory = File(context.filesDir, "capture_inbox").apply { mkdirs() }
        prune(directory)
        val temporary = File(directory, "${UUID.randomUUID()}.pending")
        try {
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "图片已不可读取" }
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= MAX_IMAGE_BYTES) { "图片不能超过 5MB" }
                        output.write(buffer, 0, read)
                    }
                    require(total > 0) { "图片内容为空" }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.absolutePath, bounds)
            val actualMime = bounds.outMimeType
            require(actualMime == "image/jpeg" || actualMime == "image/png") {
                "仅支持 JPEG 或 PNG 图片"
            }
            require(bounds.outWidth > 0 && bounds.outHeight > 0 &&
                bounds.outWidth <= OnDeviceImageTextRecognizer.MAX_IMAGE_DIMENSION &&
                bounds.outHeight <= OnDeviceImageTextRecognizer.MAX_IMAGE_DIMENSION &&
                bounds.outWidth.toLong() * bounds.outHeight <= OnDeviceImageTextRecognizer.MAX_IMAGE_PIXELS
            ) { "图片尺寸过大，最长边不能超过 20000 像素且总像素不能超过 2000 万" }
            val extension = if (actualMime == "image/png") "png" else "jpg"
            val target = File(directory, "${UUID.randomUUID()}.$extension")
            require(temporary.renameTo(target)) { "无法保存图片副本" }
            return candidate.copy(
                localPath = target.absolutePath,
                displayName = displayName ?: candidate.displayName ?: target.name,
                mimeType = actualMime,
            )
        } catch (error: Throwable) {
            temporary.delete()
            throw error
        }
    }

    private fun prune(directory: File) {
        val cutoff = System.currentTimeMillis() - CACHE_RETENTION_MILLIS
        val retained = CaptureCandidateStore.retainedLocalPaths(context)
        directory.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath !in retained && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }

    companion object {
        private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
        private const val CACHE_RETENTION_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
