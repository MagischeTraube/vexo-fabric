package xyz.vexo.utils.api

import com.google.gson.JsonObject
import xyz.vexo.Vexo
import xyz.vexo.utils.RequestUtils
import xyz.vexo.utils.UuidResolver
import xyz.vexo.utils.logError
import java.net.URLEncoder
import kotlin.coroutines.cancellation.CancellationException

object VexoApi {
    /**
     * Sends a GET request to the Vexo API and returns the "data" object of the response.
     *
     * Prefer the typed wrappers (e.g. [DungeonsApi]) over calling this directly.
     *
     * @param endpoint The endpoint to request
     * @param query Query parameters, keys and values are URL-encoded
     * @param maxRetries How often a failed request is retried
     * @return The "data" object, or null if the request failed, the API did not
     *         report success or "data" is not a JSON object
     */
    suspend fun get(
        endpoint: ApiEndpoint,
        query: Map<String, String> = emptyMap(),
        maxRetries: Int = 2
    ): JsonObject? {
        val qs = query.entries.joinToString("&") { (k, v) ->
            "${URLEncoder.encode(k, Charsets.UTF_8)}=${URLEncoder.encode(v, Charsets.UTF_8)}"
        }
        val url = "${Vexo.VEXO_API}/${endpoint.path}" + if (qs.isNotEmpty()) "?$qs" else ""

        return try {
            val json = RequestUtils.fetchJsonWithRetry(url, maxRetries) ?: return null
            if (json.get("success")?.asBoolean != true) return null
            json.get("data")?.takeIf { it.isJsonObject }?.asJsonObject
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError(e, this)
            null
        }
    }

    /**
     * Resolves the username to a UUID and requests the endpoint for that player.
     *
     * @param endpoint The endpoint to request
     * @param username The Minecraft username
     * @param maxRetries How often a failed request is retried
     * @return The "data" object, or null if the username could not be resolved or the request failed
     */
    suspend fun forPlayer(endpoint: ApiEndpoint, username: String, maxRetries: Int = 2): JsonObject? {
        val uuid = UuidResolver.resolve(username) ?: return null
        return get(endpoint, mapOf("uuid" to uuid), maxRetries)
    }
}