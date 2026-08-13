package com.tripnext.app.data

import com.tripnext.app.data.local.PendingOperationEntity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class PushResult(val mutationId: String, val status: String, val version: Long = 0, val duplicate: Boolean = false, val currentVersion: Long = 0, val currentPayload: JSONObject? = null)
data class RemoteChange(val sequence: Long, val tripId: String, val entityType: String, val version: Long, val deleted: Boolean, val payload: JSONObject?)
data class PullResult(val cursor: Long, val changes: List<RemoteChange>)

interface TripRemoteRepository {
    fun session(): TripSession?
    suspend fun register(apiUrl: String, name: String, email: String, password: String): TripSession
    suspend fun login(apiUrl: String, email: String, password: String): TripSession
    fun logout()
    fun cursor(): Long
    fun saveCursor(value: Long)
    fun version(tripId: String): Long
    fun saveVersion(tripId: String, value: Long)
    suspend fun ensureTrip(operation: PendingOperationEntity)
    suspend fun push(operations: List<PendingOperationEntity>): List<PushResult>
    suspend fun pull(cursor: Long): PullResult
    suspend fun plan(tripId: String, context: JSONObject): JSONObject
    suspend fun applyProposal(proposalId: String, selectedItemIds: Set<String>): JSONObject
}

class HttpTripRemoteRepository(private val sessions: SessionStore) : TripRemoteRepository {
    override fun session() = sessions.load()
    override suspend fun register(apiUrl: String, name: String, email: String, password: String) = authenticate(apiUrl, "/api/auth/register", JSONObject().put("name", name).put("email", email).put("password", password))
    override suspend fun login(apiUrl: String, email: String, password: String) = authenticate(apiUrl, "/api/auth/login", JSONObject().put("email", email).put("password", password))
    override fun logout() = sessions.clear()
    override fun cursor() = sessions.cursor()
    override fun saveCursor(value: Long) = sessions.saveCursor(value)
    override fun version(tripId: String) = sessions.version(tripId)
    override fun saveVersion(tripId: String, value: Long) = sessions.saveVersion(tripId, value)
    private fun authenticate(apiUrl: String, path: String, body: JSONObject): TripSession {
        require(apiUrl.startsWith("https://") || apiUrl.startsWith("http://10.0.2.2") || apiUrl.startsWith("http://127.0.0.1") || apiUrl.startsWith("http://localhost")) { "Use HTTPS fora do servidor local." }
        val result = request(apiUrl.trimEnd('/') + path, "POST", body, null)
        val user = result.getJSONObject("user")
        return TripSession(apiUrl.trimEnd('/'), result.getString("token"), user.getString("id"), user.getString("name"), user.getString("email")).also(sessions::save)
    }
    override suspend fun ensureTrip(operation: PendingOperationEntity) {
        val session = requireSession()
        val response = requestRaw(session.apiUrl + "/api/trips", "POST", JSONObject().put("id", operation.tripId).put("data", JSONObject(operation.payload)), session.token)
        if (response.first !in 200..299 && response.first != 409) errorFrom(response)
    }
    override suspend fun push(operations: List<PendingOperationEntity>): List<PushResult> {
        if (operations.isEmpty()) return emptyList()
        val session = requireSession()
        val array = JSONArray(operations.map { operation -> JSONObject().put("mutationId", operation.id).put("tripId", operation.tripId).put("entityType", operation.entityType).put("entityId", operation.entityId).put("baseVersion", operation.baseVersion).put("deleted", operation.deleted).apply { if (!operation.deleted) put("payload", JSONObject(operation.payload)) } })
        val result = request(session.apiUrl + "/api/sync/push", "POST", JSONObject().put("operations", array), session.token).getJSONArray("results")
        return (0 until result.length()).map { index -> val item = result.getJSONObject(index); val current = item.optJSONObject("current"); PushResult(item.getString("mutationId"), item.getString("status"), item.optLong("version"), item.optBoolean("duplicate"), current?.optLong("version") ?: 0, current?.optJSONObject("payload")) }
    }
    override suspend fun pull(cursor: Long): PullResult {
        val session = requireSession()
        val result = request(session.apiUrl + "/api/sync/pull?cursor=$cursor", "GET", null, session.token)
        val changes = result.getJSONArray("changes")
        return PullResult(result.getLong("cursor"), (0 until changes.length()).map { index -> val item = changes.getJSONObject(index); RemoteChange(item.getLong("sequence"), item.getString("tripId"), item.getString("entityType"), item.getLong("version"), item.optBoolean("deleted"), item.optJSONObject("payload")) })
    }
    override suspend fun plan(tripId: String, context: JSONObject): JSONObject {
        val session = requireSession()
        return request(session.apiUrl + "/api/ai/plan", "POST", JSONObject().put("tripId", tripId).put("context", context), session.token).getJSONObject("record")
    }
    override suspend fun applyProposal(proposalId: String, selectedItemIds: Set<String>): JSONObject {
        val session = requireSession()
        return request(session.apiUrl + "/api/ai/proposals/$proposalId/apply", "POST", JSONObject().put("selectedItemIds", JSONArray(selectedItemIds.toList())), session.token).getJSONObject("record")
    }
    private fun requireSession() = sessions.load() ?: error("Entre na sua conta para sincronizar.")
    private fun request(url: String, method: String, body: JSONObject?, token: String?) = requestRaw(url, method, body, token).let { if (it.first !in 200..299) errorFrom(it); JSONObject(it.second) }
    private fun errorFrom(response: Pair<Int, String>): Nothing { val code = runCatching { JSONObject(response.second).optString("error") }.getOrDefault(""); error(if (response.first == 401) "Sessão expirada." else "API ${response.first}: $code") }
    private fun requestRaw(url: String, method: String, body: JSONObject?, token: String?): Pair<Int, String> {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method; connection.connectTimeout = 12_000; connection.readTimeout = 20_000; connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) { connection.doOutput = true; connection.setRequestProperty("Content-Type", "application/json"); connection.outputStream.use { it.write(body.toString().toByteArray()) } }
            val code = connection.responseCode; val stream = if (code in 200..299) connection.inputStream else connection.errorStream; code to (stream?.bufferedReader()?.use { it.readText() } ?: "{}")
        } finally { connection.disconnect() }
    }
}
