package xyz.vexo.utils

import java.util.concurrent.ConcurrentHashMap
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import xyz.vexo.Vexo
import xyz.vexo.events.EventBus

object PlayerData {
    private const val MOJANG_API = "https://api.mojang.com/users/profiles/minecraft"
    private const val PLAYER_API = "https://api.infm7.xyz/player"

    private const val CACHE_EVICTION_MS = 60 * 60 * 1000L // 60 min
    private const val EVICTION_CHECK_INTERVAL_MS = 15 * 60 * 1000L // 15 min
    private const val MAX_UUID_CACHE_SIZE = 300

    val playerCache = ConcurrentHashMap<String, CachedPlayerData>()
    val uuidCache = ConcurrentHashMap<String, CachedUuid>()

    private val pendingPlayerRequests = ConcurrentHashMap<String, Deferred<PlayerDataObject?>>()

    data class CachedPlayerData(
        val data: PlayerDataObject,
        var lastAccessed: Long
    )

    data class CachedUuid(
        val uuid: String,
        var lastAccessed: Long
    )

    /**
     * Main player data class with accessor methods
     *
     * @property uuid The player's UUID
     * @property ign The player's in-game name
     * @property dungeons The player's dungeon data
     * @property kuudra The player's kuudra data
     * @property isError Whether the data is an error (e.g. player not found)
     */
    data class PlayerDataObject(
        val uuid: String,
        val ign: String,
        val dungeons: DungeonsData?,
        val kuudra: KuudraData?,
        val isError: Boolean = false
    )
    {
        val catacombsLevel: Int get() = dungeons?.catacombsLevel ?: 0
        val totalSecrets: Int get() = dungeons?.totalSecrets ?: 0
        val hasFairyPerk: Boolean get() = dungeons?.essenceShopPerks?.helpOfTheFairy ?: false

        fun getBestTime(floor: Int, master: Boolean = false): Int? {
            return if (master) {
                dungeons?.personalBests?.masterCatacombs?.get(floor.toString())
            } else {
                dungeons?.personalBests?.catacombs?.get(floor.toString())
            }
        }

        fun getKuudraRuns(tier: String): Int = kuudra?.totalRuns?.get(tier) ?: 0
        val totalKuudraRuns: Int get() = kuudra?.totalRuns?.values?.sum() ?: 0
    }

    /**
     * Dungeons data class
     *
     * @property personalBests The player's personal best times
     * @property catacombsLevel The player's catacombs level
     * @property totalSecrets The player's total secrets
     * @property essenceShopPerks The player's essence shop perks
     */
    data class DungeonsData(
        val personalBests: PersonalBests?,
        val catacombsLevel: Int?,
        val totalSecrets: Int?,
        val essenceShopPerks: EssenceShopPerks?
    )

    /**
     * Personal bests data class
     *
     * @property catacombs The player's personal best times for normal catacombs
     * @property masterCatacombs The player's personal best times for master catacombs
     */
    data class PersonalBests(
        val catacombs: Map<String, Int>?,
        val masterCatacombs: Map<String, Int>?
    )

    /**
     * Essence shop perks data class
     *
     * @property helpOfTheFairy Whether the player has the help of the fairy perk
     */
    data class EssenceShopPerks(
        val helpOfTheFairy: Boolean = false
    )

    /**
     * Kuudra data class
     *
     * @property totalRuns The player's total kuudra runs by tier
     */
    data class KuudraData(
        val totalRuns: Map<String, Int>?
    )

    init {
        EventBus.subscribe(this)

        Vexo.scope.launch {
            val name = Vexo.mc.user?.name ?: return@launch
            fetchPlayerData(name)
        }

        Vexo.scope.launch {
            while (isActive) {
                delay(EVICTION_CHECK_INTERVAL_MS)
                evictOldPlayers()
            }
        }
    }

    /**
     * Gets player data by username. Loads from cache, file, or API.
     *
     * @param username The Minecraft username
     * @param callback Called with PlayerDataObject or null when data is loaded
     */
    fun getPlayerData(username: String, callback: (PlayerDataObject?) -> Unit) {
        Vexo.scope.launch {
            val key = username.lowercase()

            val deferred = pendingPlayerRequests.computeIfAbsent(key) {
                async { fetchPlayerData(username) }
            }

            try {
                val result = deferred.await()
                callback(result)
            } finally {
                pendingPlayerRequests.remove(key, deferred)
            }
        }
    }

    /**
     * Fetches and caches player data without a callback.
     *
     * @param username The Minecraft username
     */
    fun fetchAndCachePlayerData(username: String) {
        Vexo.scope.launch {
            val key = username.lowercase()

            val deferred = pendingPlayerRequests.computeIfAbsent(key) {
                async { fetchPlayerData(username) }
            }

            try {
                deferred.await()
            } finally {
                pendingPlayerRequests.remove(key, deferred)
            }
        }
    }

    /**
     * Internal suspend function that does the actual loading
     *
     * @param username The Minecraft username
     * @return PlayerDataObject or null if player not found
     */
    private suspend fun fetchPlayerData(username: String): PlayerDataObject? {
        return withContext(Dispatchers.IO) {
            try {
                val uuid = getUuidFromUsername(username) ?: run {
                    logInfo("Player not found: $username")
                    return@withContext PlayerDataObject(
                        uuid = "",
                        ign = username,
                        dungeons = null,
                        kuudra = null,
                        isError = true
                    )
                }

                val currentTime = System.currentTimeMillis()

                playerCache[uuid]?.let { cached ->
                    cached.lastAccessed = currentTime
                    return@withContext cached.data
                }

                val data = fetchFromApi(uuid, username)

                if (data != null && !data.isError) {
                    playerCache[uuid] = CachedPlayerData(data, currentTime)
                    return@withContext data
                }

                return@withContext PlayerDataObject(
                    uuid = uuid,
                    ign = username,
                    dungeons = null,
                    kuudra = null,
                    isError = true
                )

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError(e, this@PlayerData)
                PlayerDataObject(
                    uuid = "",
                    ign = username,
                    dungeons = null,
                    kuudra = null,
                    isError = true
                )
            }
        }
    }


    /**
     * Gets UUID from Mojang API
     *
     * @param username The Minecraft username
     * @return The UUID or null if not found
     */
    private suspend fun getUuidFromUsername(username: String): String? {
        val key = username.lowercase()
        val currentTime = System.currentTimeMillis()

        uuidCache[key]?.let { cached ->
            cached.lastAccessed = currentTime
            return cached.uuid
        }

        return try {
            val json = ApiUtils.fetchJson("$MOJANG_API/$username")
            json.get("id")?.asString?.also { uuid ->
                if (uuidCache.size >= MAX_UUID_CACHE_SIZE) {
                    evictOldestUuids()
                }
                uuidCache[key] = CachedUuid(uuid, currentTime)
            }
        } catch (e: Exception) {
            logError(e, this@PlayerData)
            null
        }
    }

    /**
     * Fetches player data from the API
     *
     * @param uuid The player's UUID
     * @param ign The player's in-game name
     * @return PlayerDataObject or null if not found
     */
    private suspend fun fetchFromApi(uuid: String, ign: String): PlayerDataObject? {
        return try {
            val json = ApiUtils.fetchJsonWithRetry("$PLAYER_API?uuid=$uuid", maxRetries = 2)
                ?: return null

            if (json.get("success")?.asBoolean != true) {
                return null
            }

            val playerData = json.getAsJsonObject("player_data")

            parsePlayerData(uuid, ign, playerData)
        } catch (e: Exception) {
            logError(e, this@PlayerData)
            null
        }
    }

    /**
     * Parses player data from JSON
     *
     * @param uuid The player's UUID
     * @param ign The player's in-game name
     * @param playerData The player data JSON
     * @return PlayerDataObject
     */
    private fun parsePlayerData(uuid: String, ign: String, playerData: JsonObject): PlayerDataObject {
        val dungeonsJson = playerData.getAsJsonObject("dungeons")
        val kuudraJson = playerData.getAsJsonObject("kuudra")

        val dungeons = dungeonsJson?.let {
            DungeonsData(
                personalBests = it.getAsJsonObject("personal_bests")?.let { pb ->
                    PersonalBests(
                        catacombs = pb.getAsJsonObject("catacombs")?.entrySet()
                            ?.mapNotNull { (k, v) ->
                                if (v.isJsonNull) null else k to v.asInt
                            }
                            ?.toMap(),

                        masterCatacombs = pb.getAsJsonObject("master_catacombs")?.entrySet()
                            ?.mapNotNull { (k, v) ->
                                if (v.isJsonNull) null else k to v.asInt
                            }
                            ?.toMap()
                    )
                },
                catacombsLevel = it.get("catacombs_level")?.asInt,
                totalSecrets = it.get("total_secrets")?.asInt,
                essenceShopPerks = it.getAsJsonObject("essence_shop_perks")?.let { perks ->
                    EssenceShopPerks(
                        helpOfTheFairy = perks.get("HELP_OF_THE_FAIRY")?.asBoolean ?: false
                    )
                }
            )
        }

        val kuudra = kuudraJson?.let {
            KuudraData(
                totalRuns = it.getAsJsonObject("total_runs")?.entrySet()
                    ?.associate { (k, v) -> k to v.asInt }
            )
        }

        return PlayerDataObject(uuid, ign, dungeons, kuudra, isError = false)
    }

    /**
     * Removes players from cache that haven't been accessed for CACHE_EVICTION_MS
     */
    private fun evictOldPlayers() {
        val now = System.currentTimeMillis()
        val ownName = Vexo.mc.user?.name?.lowercase()
        val ownUuid = ownName?.let { uuidCache[it]?.uuid }

        val removedUuids = mutableSetOf<String>()

        playerCache.entries.removeIf { (uuid, cached) ->
            val expired = uuid != ownUuid && now - cached.lastAccessed >= CACHE_EVICTION_MS
            if (expired) {
                removedUuids.add(uuid)
                pendingPlayerRequests[uuid]?.cancel()
            }
            expired
        }

        uuidCache.entries.removeIf { (_, cachedUuid) ->
            val expired = cachedUuid.uuid in removedUuids ||
                    (cachedUuid.uuid != ownUuid && now - cachedUuid.lastAccessed >= CACHE_EVICTION_MS)
            if (expired) {
                pendingPlayerRequests.remove(cachedUuid.uuid)?.cancel()
            }
            expired
        }
    }

    /**
     * Removes the oldest 25% of UUIDs from the cache
     */
    private fun evictOldestUuids() {
        val ownName = Vexo.mc.user?.name?.lowercase()

        val sortedEntries = uuidCache.entries
            .filter { it.key != ownName }
            .sortedBy { it.value.lastAccessed }

        val toRemove = (sortedEntries.size * 0.25).toInt().coerceAtLeast(1)

        sortedEntries.take(toRemove).forEach { entry ->
            uuidCache.remove(entry.key)
            pendingPlayerRequests.remove(entry.value.uuid)?.cancel()
        }
    }
}
