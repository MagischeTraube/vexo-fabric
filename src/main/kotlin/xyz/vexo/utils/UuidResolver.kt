package xyz.vexo.utils

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.vexo.Vexo
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.DurationUnit
import kotlin.time.toDuration


object UuidResolver {
    private const val MAX_SIZE = 1000
    private const val TTL_MS = 7L * 24 * 60 * 60 * 1000

    private const val NOT_FOUND_MAX_SIZE = 1000
    private const val NOT_FOUND_TTL_MS = 600 * 1000

    private const val ERROR_COOLDOWN_MS = 5_000L
    private const val RATE_LIMIT_COOLDOWN_MS = 60_000L
    private const val SAVE_DELAY_MS = 10_000L

    private const val MOJANG_API = "https://api.mojang.com/users/profiles/minecraft"
    private const val MOJANG_BULK_API = "https://api.mojang.com/profiles/minecraft"
    private const val BATCH_SIZE = 10

    private val RETRY_DELAYS_MS = longArrayOf(1_000L, 2_000L, 5_000L)

    private val gson get() = Vexo.gson

    private val VALID_NAME = Regex("^\\w{1,16}$")

    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    /**
     * A cached mapping. Validity is based on [fetchedAt] only, [accessedAt]
     * is used solely for least-recently-used eviction.
     */
    private class Entry(val uuid: String, val fetchedAt: Long, @Volatile var accessedAt: Long = fetchedAt)

    private class HttpStatusException(val code: Int, api: String) :
        IOException("$api returned $code")

    private fun statusException(response: HttpResponse<*>, api: String): HttpStatusException =
        HttpStatusException(response.statusCode(), api)

    private val cache: ConcurrentHashMap<String, Entry> by lazy { loadFromDisk() }
    private val notFound = ConcurrentHashMap<String, Long>()

    private val inFlight = ConcurrentHashMap<String, Deferred<String?>>()

    @Volatile private var blockedUntil = 0L
    @Volatile private var cacheLoaded = false

    @Volatile private var dirty = false
    private val saveScheduled = AtomicBoolean(false)

    private val cacheFile: File by lazy {
        Vexo.configDir.resolve("uuid_cache.json").also { it.parentFile?.mkdirs() }
    }

    /**
     * Resolves a Minecraft username to a UUID.
     * Concurrent calls for the same name share one request.
     *
     * @param username The Minecraft username to resolve
     * @return The UUID of the player, or null if the username could not be resolved
     */
    suspend fun resolve(username: String): String? {
        if (!VALID_NAME.matches(username)) return null
        val key = username.lowercase()
        val now = System.currentTimeMillis()

        lookup(key, now)?.let { return it }

        inFlight[key]?.let { return it.await() }

        if (isNotFound(key, now) || now < blockedUntil) return null

        val created = Vexo.scope.async(start = CoroutineStart.LAZY) { fetchSingle(key) }
        val existing = inFlight.putIfAbsent(key, created)
        if (existing != null) {
            created.cancel()
            return existing.await()
        }

        created.invokeOnCompletion { inFlight.remove(key, created) }
        created.start()
        return created.await()
    }

    /**
     * Resolves multiple Minecraft usernames to UUIDs, querying Mojang in batches of 10.
     * A batch with a single name uses the single-lookup endpoint instead.
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

            val uuid = lookup(key, now)
            if (uuid != null) {
                result[key] = uuid
            } else if (VALID_NAME.matches(name) && !isNotFound(key, now)) {
                missing.add(key)
            }
        }

        for (batch in missing.chunked(BATCH_SIZE)) {
            if (System.currentTimeMillis() < blockedUntil) break

            if (batch.size == 1) {
                val key = batch[0]
                resolve(key)?.let { result[key] = it }
                continue
            }


            try {
                val fetched = fetchBatch(batch)
                val batchTime = System.currentTimeMillis()

                for ((key, uuid) in fetched) {
                    store(key, uuid, batchTime)
                    result[key] = uuid
                }
                for (key in batch) if (key !in fetched) markNotFound(key, batchTime)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onRequestFailed(e)
                break
            }
        }

        return result
    }

    /**
     * Writes the cache to disk immediately, if anything changed since the last write.
     *
     * Should be called on shutdown (e.g. `CLIENT_STOPPING`) so pending changes are not lost.
     */
    fun flush() {
        if (!cacheLoaded || !dirty) return
        writeToDisk()
    }

    /**
     * Returns the cached UUID for [key] if present and not expired.
     * Expired entries are removed. Does not extend the entry's lifetime.
     */
    private fun lookup(key: String, now: Long): String? {
        val entry = cache[key] ?: return null
        if (now - entry.fetchedAt > TTL_MS) {
            cache.remove(key, entry)
            return null
        }
        entry.accessedAt = now
        return entry.uuid
    }

    /**
     * Checks whether [key] was recently reported as non-existent by Mojang.
     */
    private fun isNotFound(key: String, now: Long): Boolean {
        val ts = notFound[key] ?: return false
        if (now - ts > NOT_FOUND_TTL_MS) {
            notFound.remove(key, ts)
            return false
        }
        return true
    }

    /**
     * Remembers that [key] does not exist. If the map is full, expired entries are dropped
     * first, then the oldest 25%.
     */
    private fun markNotFound(key: String, now: Long) {
        if (notFound.size >= NOT_FOUND_MAX_SIZE) {
            notFound.entries.removeIf { now - it.value > NOT_FOUND_TTL_MS }

            if (notFound.size >= NOT_FOUND_MAX_SIZE) {
                val sorted = notFound.entries.sortedBy { it.value }
                sorted.take((sorted.size / 4).coerceAtLeast(1))
                    .forEach { notFound.remove(it.key, it.value) }
            }
        }
        notFound[key] = now
    }

    /**
     * Stores a username-to-UUID mapping and schedules a disk write.
     *
     * If the cache reaches its maximum capacity, entries are evicted first.
     *
     * @param key The lowercase Minecraft username
     * @param uuid The UUID associated with the username
     * @param now The current timestamp, used as fetch time
     */
    private fun store(key: String, uuid: String, now: Long) {
        if (cache.size >= MAX_SIZE) evict(now)
        cache[key] = Entry(uuid, now)
        notFound.remove(key)
        dirty = true
        scheduleSave()
    }

    /**
     * Removes expired entries first. If the cache is still full, removes the
     * 25% least recently accessed entries.
     */
    private fun evict(now: Long) {
        cache.entries.removeIf { now - it.value.fetchedAt > TTL_MS }
        if (cache.size < MAX_SIZE) return

        val sorted = cache.entries.sortedBy { it.value.accessedAt }
        sorted.take((sorted.size / 4).coerceAtLeast(1))
            .forEach { cache.remove(it.key, it.value) }
    }

    /**
     * Applies a cooldown after a failed request. Rate limits (HTTP 429) block for
     * [RATE_LIMIT_COOLDOWN_MS], all other errors for [ERROR_COOLDOWN_MS].
     */
    private fun onRequestFailed(e: Exception) {
        val cooldown = if (e is HttpStatusException && e.code == 429) {
            RATE_LIMIT_COOLDOWN_MS
        } else {
            ERROR_COOLDOWN_MS
        }
        blockedUntil = System.currentTimeMillis() + cooldown
        logError(e, this)
    }

    /**
     * Runs [block] and retries it after transient failures, waiting 1s, 2s and 5s
     * before the 1st, 2nd and 3rd retry. If the last retry fails too, the exception
     * is rethrown. Only cancellation and HTTP 404/204 are never retried.
     */
    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isRetryable(e) || attempt >= RETRY_DELAYS_MS.size) throw e
                delay(RETRY_DELAYS_MS[attempt++].toDuration(DurationUnit.MILLISECONDS))
            }
        }
    }

    /**
     * Everything is retried except HTTP 404 and 204 (name does not exist).
     */
    private fun isRetryable(e: Exception): Boolean =
        !(e is HttpStatusException && (e.code == 404 || e.code == 204))

    /**
     * Loads the cache from disk. Only runs on first access.
     *
     * File format: `{"name": {"uuid": "...", "fetchedAt": millis, "accessedAt": millis}, ...}`.
     * Expired or malformed entries are skipped. If the file holds more than [MAX_SIZE]
     * valid entries, only the most recently accessed ones are kept and a save is scheduled.
     * A corrupt file results in an empty cache.
     */
    private fun loadFromDisk(): ConcurrentHashMap<String, Entry> {
        val entries = ConcurrentHashMap<String, Entry>()
        var trimmed = false

        try {
            if (cacheFile.exists()) {
                val now = System.currentTimeMillis()
                val root = gson.fromJson(cacheFile.readText(), JsonObject::class.java)

                if (root != null) {
                    for ((username, element) in root.entrySet()) {
                        val entry = runCatching { parseEntry(element.asJsonObject, now) }.getOrNull() ?: continue
                        entries[username] = entry
                    }
                }

                if (entries.size > MAX_SIZE) {
                    entries.entries
                        .sortedBy { it.value.accessedAt }
                        .take(entries.size - MAX_SIZE)
                        .forEach { entries.remove(it.key) }
                    dirty = true
                    trimmed = true
                }
            }
        } catch (e: Exception) {
            logError(e, this)
        }

        cacheLoaded = true
        if (trimmed) scheduleSave()
        return entries
    }

    private fun parseEntry(json: JsonObject, now: Long): Entry? {
        val uuid = json.get("uuid")?.asString ?: return null
        val fetchedAt = json.get("fetchedAt")?.asLong ?: return null
        if (now - fetchedAt > TTL_MS || fetchedAt > now + 60_000) return null

        val accessedAt = json.get("accessedAt")?.asLong?.coerceIn(fetchedAt, now) ?: fetchedAt
        return Entry(uuid, fetchedAt, accessedAt)
    }

    /**
     * Schedules a single delayed disk write, batching multiple [store] calls together.
     */
    private fun scheduleSave() {
        if (!saveScheduled.compareAndSet(false, true)) return
        Vexo.scope.launch(Dispatchers.IO) {
            delay(SAVE_DELAY_MS.toDuration(DurationUnit.MILLISECONDS))
            saveScheduled.set(false)
            flush()
        }
    }

    /**
     * Writes all non-expired entries to a temporary file and atomically replaces
     * the cache file, so a crash cannot leave a half-written cache behind.
     */
    @Synchronized
    private fun writeToDisk() {
        try {
            dirty = false
            val now = System.currentTimeMillis()
            val root = JsonObject()

            for ((username, entry) in cache) {
                if (now - entry.fetchedAt > TTL_MS) continue

                val entryJson = JsonObject()
                entryJson.addProperty("uuid", entry.uuid)
                entryJson.addProperty("fetchedAt", entry.fetchedAt)
                entryJson.addProperty("accessedAt", entry.accessedAt)
                root.add(username, entryJson)
            }

            val tmp = File(cacheFile.parentFile, cacheFile.name + ".tmp")
            tmp.writeText(gson.toJson(root))

            try {
                Files.move(tmp.toPath(), cacheFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } catch (_: AtomicMoveNotSupportedException) {
                Files.move(tmp.toPath(), cacheFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
        } catch (e: Exception) {
            dirty = true
            logError(e, this)
        }
    }

    /**
     * Resolves a single (lowercase) username via the Mojang API and updates the cache.
     * Transient failures are retried via [withRetry] (1s, 2s, 5s).
     * 404/204 mark the name as non-existent without triggering a cooldown.
     * Never throws except for [CancellationException].
     */
    private suspend fun fetchSingle(key: String): String? {
        try {
            return withRetry {
                val request = HttpRequest.newBuilder(URI.create("$MOJANG_API/$key"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build()

                val response = withContext(Dispatchers.IO) {
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                }
                val now = System.currentTimeMillis()

                when (response.statusCode()) {
                    200 -> {
                        val uuid = gson.fromJson(response.body(), JsonObject::class.java)
                            ?.get("id")?.asString
                        if (uuid != null) store(key, uuid, now) else markNotFound(key, now)
                        uuid
                    }
                    204, 404 -> {
                        markNotFound(key, now)
                        null
                    }
                    else -> throw statusException(response, "Mojang API")
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            onRequestFailed(e)
            return null
        }
    }

    /**
     * Resolves multiple Minecraft usernames in a single Mojang API request.
     * Transient failures are retried via [withRetry] (1s, 2s, 5s).
     *
     * @param names The usernames to resolve (maximum 10 per request)
     * @return Map of lowercase usernames to their UUIDs
     */
    private suspend fun fetchBatch(names: List<String>): Map<String, String> =
        withRetry {
            withContext(Dispatchers.IO) {
                val body = gson.toJson(names)
                val request = HttpRequest.newBuilder(URI.create(MOJANG_BULK_API))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() != 200) {
                    throw statusException(response, "Mojang bulk API")
                }

                gson.fromJson(response.body(), JsonArray::class.java).associate {
                    val obj = it.asJsonObject
                    obj.get("name").asString.lowercase() to obj.get("id").asString
                }
            }
        }
}