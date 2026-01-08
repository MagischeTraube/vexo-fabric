package xyz.vexo.utils

import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import java.net.URI
import java.net.HttpURLConnection

/**
 * Utility for making HTTP requests to APIs using Coroutines
 */
object ApiUtils {
    private val gson = Gson()

    private const val DEFAULT_CONNECT_TIMEOUT = 5000
    private const val DEFAULT_READ_TIMEOUT = 5000
    private val USER_AGENT by lazy { "Vexo/${xyz.vexo.Vexo.version}" }

    /**
     * Fetches JSON from a URL asynchronously.
     *
     * @param url The URL to fetch JSON from.
     * @param connectTimeout The connection timeout in milliseconds.
     * @param readTimeout The read timeout in milliseconds.
     * @return The JSON object.
     */
    suspend fun fetchJson(
        url: String,
        connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
        readTimeout: Int = DEFAULT_READ_TIMEOUT
    ): JsonObject = withContext(Dispatchers.IO) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/json")
                this.connectTimeout = connectTimeout
                this.readTimeout = readTimeout
            }

            connection.inputStream.use { stream ->
                gson.fromJson(stream.reader(), JsonObject::class.java)
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Fetches JSON with automatic retry on failure.
     *
     * @param url The URL to fetch JSON from.
     * @param maxRetries The maximum number of retries.
     * @param initialDelayMs The initial delay in milliseconds.
     * @return The JSON object.
     */
    suspend fun fetchJsonWithRetry(
        url: String,
        maxRetries: Int = 3,
        initialDelayMs: Long = 1000
    ): JsonObject? {
        repeat(maxRetries) { attempt ->
            try {
                return fetchJson(url)
            } catch (e: Exception) {
                if (attempt == maxRetries - 1) {
                    logError(e, this)
                }

                if (attempt < maxRetries - 1) {
                    val delay = initialDelayMs * (1 shl attempt)
                    delay(delay)
                }
            }
        }
        return null
    }

    /**
     * Makes a POST request with JSON body.
     *
     * @param url The URL to make the POST request to.
     * @param body The JSON body to send.
     * @param connectTimeout The connection timeout in milliseconds.
     * @param readTimeout The read timeout in milliseconds.
     * @return The JSON object.
     */
    suspend fun postJson(
        url: String,
        body: JsonObject,
        connectTimeout: Int = DEFAULT_CONNECT_TIMEOUT,
        readTimeout: Int = DEFAULT_READ_TIMEOUT
    ): JsonObject = withContext(Dispatchers.IO) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection

        try {
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                doOutput = true
                this.connectTimeout = connectTimeout
                this.readTimeout = readTimeout
            }

            connection.outputStream.use { output ->
                output.write(body.toString().toByteArray())
            }

            connection.inputStream.use { stream ->
                gson.fromJson(stream.reader(), JsonObject::class.java)
            }
        } finally {
            connection.disconnect()
        }
    }
}