package aifunc.top.ainote_app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

internal enum class CaptureHandlingMode(val wireValue: String) {
    CONFIRM("confirm"),
    AUTO("auto");

    companion object {
        fun fromWire(value: String?): CaptureHandlingMode =
            entries.firstOrNull { it.wireValue == value } ?: CONFIRM
    }
}

internal enum class CaptureJobState(val wireValue: String) {
    QUEUED("queued"),
    OCR("ocr"),
    UPLOADING("uploading"),
    COMPLETED("completed"),
    FAILED("failed"),
    PAUSED("paused");

    companion object {
        fun fromWire(value: String?): CaptureJobState =
            entries.firstOrNull { it.wireValue == value } ?: QUEUED
    }
}

internal data class CaptureCandidate(
    val id: String,
    val source: String,
    val uri: String? = null,
    val localPath: String? = null,
    val displayName: String? = null,
    val mimeType: String? = null,
    val recognizedText: String? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val handlingMode: CaptureHandlingMode = CaptureHandlingMode.CONFIRM,
    val autoConsentEpoch: Long? = null,
    val state: CaptureJobState = CaptureJobState.QUEUED,
    val identityId: String? = null,
    val noteId: String? = null,
    val errorMessage: String? = null,
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "source" to source,
        "uri" to uri,
        "localPath" to localPath,
        "displayName" to displayName,
        "mimeType" to mimeType,
        "recognizedText" to recognizedText,
        "createdAtMillis" to createdAtMillis,
        "handlingMode" to handlingMode.wireValue,
        "autoConsentEpoch" to autoConsentEpoch,
        "state" to state.wireValue,
        "noteId" to noteId,
        "errorMessage" to errorMessage,
    )

    internal fun toPersistenceMap(): Map<String, Any?> = toMap() + mapOf(
        "identityId" to identityId,
    )

    fun toJson(): String = JSONObject(toPersistenceMap()).toString()

    companion object {
        fun fromJson(value: String): CaptureCandidate? = runCatching {
            val json = JSONObject(value)
            CaptureCandidate(
                id = json.getString("id"),
                source = json.getString("source"),
                uri = json.optString("uri").takeIf { it.isNotBlank() && it != "null" },
                localPath = json.optString("localPath").takeIf { it.isNotBlank() && it != "null" },
                displayName = json.optString("displayName").takeIf { it.isNotBlank() && it != "null" },
                mimeType = json.optString("mimeType").takeIf { it.isNotBlank() && it != "null" },
                recognizedText = json.optString("recognizedText").takeIf { it.isNotBlank() && it != "null" },
                createdAtMillis = json.optLong("createdAtMillis", System.currentTimeMillis()),
                handlingMode = CaptureHandlingMode.fromWire(json.optString("handlingMode")),
                autoConsentEpoch = json.optLong("autoConsentEpoch", 0L).takeIf { it > 0L },
                state = CaptureJobState.fromWire(json.optString("state")),
                identityId = json.optString("identityId").takeIf { it.isNotBlank() && it != "null" },
                noteId = json.optString("noteId").takeIf { it.isNotBlank() && it != "null" },
                errorMessage = json.optString("errorMessage").takeIf { it.isNotBlank() && it != "null" },
            )
        }.getOrNull()
    }
}

internal object CaptureCandidateStore {
    private const val PREFS = "ainote_capture"
    private const val LEGACY_PENDING = "pending_candidate"
    private const val PENDING_QUEUE = "pending_candidates_v2"
    private const val MAX_PENDING = 20

    @Synchronized
    fun save(context: Context, candidate: CaptureCandidate): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val legacy = prefs.getString(LEGACY_PENDING, null)?.let(CaptureCandidate::fromJson)
        if (legacy?.id == candidate.id) {
            return prefs.edit().putString(LEGACY_PENDING, candidate.toJson()).commit()
        }
        val queue = CaptureCandidateQueue(
            readQueue(prefs.getString(PENDING_QUEUE, null)),
            MAX_PENDING,
        )
        if (legacy != null && queue.find(candidate.id) == null && queue.snapshot().size >= MAX_PENDING - 1) return false
        if (!queue.upsert(candidate)) return false
        return prefs.edit()
            .putString(PENDING_QUEUE, writeQueue(queue.snapshot()))
            .commit()
    }

    @Synchronized
    fun peek(context: Context): CaptureCandidate? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Keep the legacy candidate at the head until explicitly acknowledged.
        prefs.getString(LEGACY_PENDING, null)?.let(CaptureCandidate::fromJson)?.let { return it }
        val queue = CaptureCandidateQueue(readQueue(prefs.getString(PENDING_QUEUE, null)), MAX_PENDING)
        if (queue.peek() == null) {
            prefs.getString(LEGACY_PENDING, null)?.let(CaptureCandidate::fromJson)?.let(queue::upsert)
            if (queue.peek() != null) {
                prefs.edit().putString(PENDING_QUEUE, writeQueue(queue.snapshot())).remove(LEGACY_PENDING).commit()
            }
        }
        return queue.peek()
    }

    @Synchronized
    fun peekConfirmHead(context: Context): CaptureCandidate? =
        peek(context)?.takeIf { it.handlingMode == CaptureHandlingMode.CONFIRM }

    @Synchronized
    fun peekAutoHead(context: Context): CaptureCandidate? =
        peek(context)?.takeIf { it.handlingMode == CaptureHandlingMode.AUTO }

    @Synchronized
    fun findById(context: Context, candidateId: String): CaptureCandidate? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return readQueue(prefs.getString(PENDING_QUEUE, null)).firstOrNull { it.id == candidateId }
            ?: prefs.getString(LEGACY_PENDING, null)?.let(CaptureCandidate::fromJson)
                ?.takeIf { it.id == candidateId }
    }

    @Synchronized
    fun retainedLocalPaths(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val candidates = readQueue(prefs.getString(PENDING_QUEUE, null)).toMutableList()
        prefs.getString(LEGACY_PENDING, null)?.let(CaptureCandidate::fromJson)?.let(candidates::add)
        return candidates.mapNotNull { it.localPath }.toSet()
    }

    @Synchronized
    fun recoverInterruptedAutoJobs(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val queue = CaptureCandidateQueue(readQueue(prefs.getString(PENDING_QUEUE, null)), MAX_PENDING)
        var changed = false
        queue.snapshot().forEach { candidate ->
            val recovered = CaptureAutoTransition.recoverAfterRestart(candidate)
            if (recovered != candidate) {
                changed = queue.upsert(recovered) || changed
            }
        }
        return !changed || prefs.edit()
            .putString(PENDING_QUEUE, writeQueue(queue.snapshot()))
            .commit()
    }

    @Synchronized
    fun retryAuto(
        context: Context,
        candidateId: String,
        identityId: String,
    ): Boolean {
        val candidate = findById(context, candidateId) ?: return false
        val retried = CaptureAutoTransition.explicitRetry(candidate, identityId) ?: return false
        return save(context, retried)
    }

    @Synchronized
    fun acknowledge(context: Context, candidateId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val queue = CaptureCandidateQueue(readQueue(prefs.getString(PENDING_QUEUE, null)), MAX_PENDING)
        val removed = queue.find(candidateId)
            ?: prefs.getString(LEGACY_PENDING, null)?.let(CaptureCandidate::fromJson)
                ?.takeIf { it.id == candidateId }
            ?: return false
        queue.acknowledge(candidateId)
        val editor = prefs.edit().putString(PENDING_QUEUE, writeQueue(queue.snapshot()))
        val legacy = prefs.getString(LEGACY_PENDING, null)?.let(CaptureCandidate::fromJson)
        if (legacy?.id == candidateId) editor.remove(LEGACY_PENDING)
        val committed = editor.commit()
        if (committed) deletePrivateCopy(context, removed.localPath)
        return committed
    }

    private fun readQueue(raw: String?): List<CaptureCandidate> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    CaptureCandidate.fromJson(json.getString(index))?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeQueue(queue: List<CaptureCandidate>): String = JSONArray().apply {
        queue.forEach { put(it.toJson()) }
    }.toString()

    private fun deletePrivateCopy(context: Context, localPath: String?) {
        if (localPath.isNullOrBlank()) return
        runCatching {
            val file = File(localPath).canonicalFile
            val persistentInbox = File(context.filesDir, "capture_inbox").canonicalFile
            val legacyCacheInbox = File(context.cacheDir, "capture_inbox").canonicalFile
            if ((file.parentFile == persistentInbox || file.parentFile == legacyCacheInbox) && file.isFile) {
                file.delete()
            }
        }
    }
}

internal data class CaptureCompletion(
    val candidateId: String,
    val noteId: String,
) {
    fun toMap(): Map<String, String> = mapOf("candidateId" to candidateId, "noteId" to noteId)
}

internal object CaptureCompletionSelector {
    fun select(completions: List<CaptureCompletion>, requestedNoteId: String?): CaptureCompletion? =
        if (requestedNoteId == null) completions.firstOrNull()
        else completions.firstOrNull { it.noteId == requestedNoteId }
}

internal object CaptureCompletionStore {
    private const val PREFS = "ainote_capture"
    private const val PENDING_COMPLETIONS = "pending_auto_completions"

    @Synchronized
    fun save(context: Context, completion: CaptureCompletion): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val queue = read(prefs.getString(PENDING_COMPLETIONS, null)).toMutableList()
        if (queue.none { it.candidateId == completion.candidateId }) queue += completion
        return prefs.edit().putString(PENDING_COMPLETIONS, write(queue.takeLast(20))).commit()
    }

    @Synchronized
    fun peek(context: Context): CaptureCompletion? = all(context).firstOrNull()

    @Synchronized
    fun find(context: Context, noteId: String): CaptureCompletion? =
        all(context).firstOrNull { it.noteId == noteId }

    @Synchronized
    fun select(context: Context, requestedNoteId: String?): CaptureCompletion? =
        CaptureCompletionSelector.select(all(context), requestedNoteId)

    @Synchronized
    fun consume(context: Context, noteId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val queue = read(prefs.getString(PENDING_COMPLETIONS, null)).toMutableList()
        if (!queue.removeAll { it.noteId == noteId }) return false
        return prefs.edit().putString(PENDING_COMPLETIONS, write(queue)).commit()
    }

    private fun all(context: Context): List<CaptureCompletion> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(PENDING_COMPLETIONS, null)
        return read(raw)
    }

    private fun read(raw: String?): List<CaptureCompletion> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val json = JSONArray(raw)
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(CaptureCompletion(item.getString("candidateId"), item.getString("noteId")))
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun write(queue: List<CaptureCompletion>): String = JSONArray().apply {
        queue.forEach { put(JSONObject(it.toMap())) }
    }.toString()
}

internal class CaptureCandidateQueue(
    initial: List<CaptureCandidate> = emptyList(),
    private val capacity: Int = 20,
) {
    private val candidates = initial.take(capacity).toMutableList()

    fun upsert(candidate: CaptureCandidate): Boolean {
        val existing = candidates.indexOfFirst { it.id == candidate.id }
        if (existing >= 0) {
            candidates[existing] = candidate
            return true
        }
        if (candidates.size >= capacity) return false
        candidates.add(candidate)
        return true
    }

    fun peek(): CaptureCandidate? = candidates.firstOrNull()

    fun find(candidateId: String): CaptureCandidate? = candidates.firstOrNull { it.id == candidateId }

    fun acknowledge(candidateId: String): Boolean = candidates.removeAll { it.id == candidateId }

    fun snapshot(): List<CaptureCandidate> = candidates.toList()
}

data class CaptureFailure(
    val source: String,
    val message: String,
) {
    fun toMap(): Map<String, String> = mapOf("source" to source, "message" to message)
    fun toJson(): String = JSONObject(toMap()).toString()

    companion object {
        fun fromJson(value: String): CaptureFailure? = runCatching {
            val json = JSONObject(value)
            CaptureFailure(json.optString("source", "share"), json.getString("message"))
        }.getOrNull()
    }
}

internal object CaptureFailureStore {
    private const val PREFS = "ainote_capture"
    private const val PENDING_FAILURE = "pending_failure"

    fun save(context: Context, failure: CaptureFailure) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(PENDING_FAILURE, failure.toJson()).commit()
    }

    fun peek(context: Context): CaptureFailure? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val raw = prefs.getString(PENDING_FAILURE, null) ?: return null
        return CaptureFailure.fromJson(raw)
    }

    fun clear(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .edit().remove(PENDING_FAILURE).commit()
}

internal object CaptureEventSink {
    @Volatile
    private var listener: ((CaptureCandidate) -> Unit)? = null

    fun attach(value: (CaptureCandidate) -> Unit) {
        listener = value
    }

    fun detach(value: (CaptureCandidate) -> Unit) {
        if (listener === value) listener = null
    }

    fun emit(candidate: CaptureCandidate) {
        listener?.invoke(candidate)
    }
}

internal object CaptureFailureEventSink {
    @Volatile
    private var listener: ((CaptureFailure) -> Unit)? = null

    fun attach(value: (CaptureFailure) -> Unit) {
        listener = value
    }

    fun detach(value: (CaptureFailure) -> Unit) {
        if (listener === value) listener = null
    }

    fun emit(failure: CaptureFailure) {
        listener?.invoke(failure)
    }
}
