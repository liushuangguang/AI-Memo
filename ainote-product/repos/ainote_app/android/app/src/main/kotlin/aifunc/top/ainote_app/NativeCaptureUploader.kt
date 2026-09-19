package aifunc.top.ainote_app

import org.json.JSONObject
import java.io.BufferedOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

internal class NativeCaptureUploader {
    @Volatile
    private var activeConnection: HttpURLConnection? = null

    fun upload(candidate: CaptureCandidate, deviceId: String, apiBaseUrl: String): String {
        val file = File(requireNotNull(candidate.localPath) { "图片副本不存在" })
        require(file.isFile) { "图片副本不存在" }
        val boundary = "ainote-${UUID.randomUUID()}"
        val connection = (URL("$apiBaseUrl/v2/note/createImageNote").openConnection() as HttpURLConnection)
        activeConnection = connection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 200_000
            connection.setRequestProperty("Authorization", "Guest $deviceId")
            connection.setRequestProperty("Device-Id", deviceId)
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            BufferedOutputStream(connection.outputStream).use { output ->
                writeTextPart(output, boundary, "recognizedText", candidate.recognizedText.orEmpty())
                writeTextPart(output, boundary, "requestId", candidate.id)
                output.write("--$boundary\r\n".toByteArray())
                output.write(
                    "Content-Disposition: form-data; name=\"file\"; filename=\"${safeFilename(file.name)}\"\r\n"
                        .toByteArray(),
                )
                output.write("Content-Type: ${candidate.mimeType ?: "image/jpeg"}\r\n\r\n".toByteArray())
                file.inputStream().use { it.copyTo(output) }
                output.write("\r\n--$boundary--\r\n".toByteArray())
            }
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val payload = runCatching { JSONObject(body) }.getOrNull()
            if (responseCode !in 200..299 || payload?.optInt("code") != 200) {
                throw IllegalStateException(payload?.optString("message")?.takeIf { it.isNotBlank() }
                    ?: "上传失败（$responseCode）")
            }
            val noteId = payload.optJSONObject("data")?.optString("id").orEmpty().trim()
            require(noteId.isNotEmpty()) { "服务端没有返回已创建的备忘录" }
            return noteId
        } finally {
            activeConnection = null
            connection.disconnect()
        }
    }

    fun cancel() {
        activeConnection?.disconnect()
        activeConnection = null
    }

    private fun writeTextPart(
        output: BufferedOutputStream,
        boundary: String,
        name: String,
        value: String,
    ) {
        output.write("--$boundary\r\n".toByteArray())
        output.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray())
        output.write(value.toByteArray(Charsets.UTF_8))
        output.write("\r\n".toByteArray())
    }

    private fun safeFilename(value: String): String =
        value.replace(Regex("[^A-Za-z0-9._-]"), "_").take(120).ifBlank { "capture.jpg" }
}
