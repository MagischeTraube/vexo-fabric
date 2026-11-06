package xyz.vexo.utils

import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

object UuidResolver {
    private const val MAX_SIZE = 300

    private const val MOJANG_API = "https://api.mojang.com/users/profiles/minecraft"
    private const val MOJANG_BULK_API = "https://api.mojang.com/profiles/minecraft"
    private const val BATCH_SIZE = 10

    private val VALID_NAME = Regex("^\\w{1,16}$")

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private class Entry(val uuid: String, @Volatile var lastAccessed: Long)
    private val cache = ConcurrentHashMap<String, Entry>()

    /**
     * Resolves a Minecraft username to a UUID.
     *
     * @param username The Minecraft username to resolve
     * @return The UUID of the player, or null if the username could not be resolved
     */
    suspend fun resolve(username: String): String? {
        val key = username.lowercase()
        val now = System.currentTimeMillis()

        cache[key]?.let {
            it.lastAccessed = now
            return it.uuid
        }

        return try {
            RequestUtils.fetchJson("$MOJANG_API/$username")
                .get("id")?.asString
                ?.also { store(key, it, now) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError(e, this)
            null
        }
    }

    /**
     * Resolves multiple Minecraft usernames to UUIDs, querying Mojang in batches of 10.
     *
     * @return Map of lowercase username -> UUID. Unresolvable names are missing from the map.
     */
    suspend fun resolveAll(usernames: Collection<String>): Map<String, String> {
        val result = HashMap<String, String>()
        val missing = LinkedHashSet<String>()
        val now = System.currentTimeMillis()

        for (name in usernames) {
            val key = name.lowercase()
            if (key in result || key in missing) continue

            val entry = cache[key]
            if (entry != null) {
                entry.lastAccessed = now
                result[key] = entry.uuid
            } else if (VALID_NAME.matches(name)) {
                missing.add(key)
            }
        }

        for (batch in missing.chunked(BATCH_SIZE)) {
            try {
                for ((key, uuid) in fetchBatch(batch)) {
                    store(key, uuid, now)
                    result[key] = uuid
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError(e, this)
            }
        }

        return result
    }

    /**
     * Resolves multiple Minecraft usernames in a single Mojang API request.
     *
     * @param names The usernames to resolve (maximum 10 per request)
     * @return Map of lowercase usernames to their UUIDs
     */
    private suspend fun fetchBatch(names: List<String>): Map<String, String> =
        withContext(Dispatchers.IO) {
            val body = names.joinToString(prefix = "[", postfix = "]") { "\"$it\"" }
            val request = HttpRequest.newBuilder(URI.create(MOJANG_BULK_API))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() != 200) {
                throw IOException("Mojang bulk API returned ${response.statusCode()}")
            }

            JsonParser.parseString(response.body()).asJsonArray.associate {
                val obj = it.asJsonObject
                obj.get("name").asString.lowercase() to obj.get("id").asString
            }
        }

    /**
     * Stores a username-to-UUID mapping in the cache.
     *
     * If the cache reaches its maximum capacity, the least recently accessed
     * entries are evicted before storing the new entry.
     *
     * @param key The lowercase Minecraft username
     * @param uuid The UUID associated with the username
     * @param now The current timestamp used for cache access tracking
     */
    private fun store(key: String, uuid: String, now: Long) {
        if (cache.size >= MAX_SIZE) evictLeastRecentlyUsed()
        cache[key] = Entry(uuid, now)
    }

    /**
     * Removes the 25% least recently accessed cache entries.
     */
    private fun evictLeastRecentlyUsed() {
        val sorted = cache.entries.sortedBy { it.value.lastAccessed }
        sorted.take((sorted.size / 4).coerceAtLeast(1))
            .forEach { cache.remove(it.key, it.value) }
    }
}