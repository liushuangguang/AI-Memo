package aifunc.top.ainote_app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import java.io.File

internal class OnDeviceImageTextRecognizer(private val context: Context) {
    private val recognizer = TextRecognition.getClient(
        ChineseTextRecognizerOptions.Builder().build(),
    )

    fun recognize(
        localPath: String,
        onSuccess: (String) -> Unit,
        onFailure: (Throwable) -> Unit,
    ) {
        val file = File(localPath)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val width = bounds.outWidth
        val height = bounds.outHeight
        if (width <= 0 || height <= 0 || width > MAX_IMAGE_DIMENSION ||
            height > MAX_IMAGE_DIMENSION || width.toLong() * height > MAX_IMAGE_PIXELS
        ) {
            onFailure(IllegalArgumentException("图片尺寸过大或格式无效"))
            return
        }
        val image = runCatching {
            InputImage.fromFilePath(context, Uri.fromFile(file))
        }.getOrElse {
            onFailure(it)
            return
        }
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text.trim()
                if (text.isBlank()) {
                    onFailure(IllegalArgumentException("图片中没有识别到可读文字"))
                } else {
                    onSuccess(text)
                }
            }
            .addOnFailureListener(onFailure)
    }

    fun close() {
        recognizer.close()
    }

    companion object {
        internal const val MAX_IMAGE_DIMENSION = 20_000
        internal const val MAX_IMAGE_PIXELS = 20_000_000L
    }
}
