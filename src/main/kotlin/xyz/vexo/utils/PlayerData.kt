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

    val playerCache = ConcurrentHashMap<String, CachedPlayerData>()
    val uuidCache = ConcurrentHashMap<String, String>()

    private val pendingPlayerRequests = ConcurrentHashMap<String, Deferred<PlayerDataObject?>>()

    data class CachedPlayerData(
        val data: PlayerDataObject,
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

            val result = deferred.await()
            pendingPlayerRequests.remove(key)

            callback(result)
        }
    }

    /**
     * Fetches and caches player data without a callback.
     *
     * @param username The Minecraft username
     * @return The fetched PlayerDataObject or null if not found/error
     */
    suspend fun fetchAndCachePlayerData(username: String): PlayerDataObject? {
        val key = username.lowercase()

        val deferred = pendingPlayerRequests.computeIfAbsent(key) {
            Vexo.scope.async { fetchPlayerData(username) }
        }

        val result = try {
            deferred.await()
        } finally {
            pendingPlayerRequests.remove(key)
        }

        result?.let { data ->
            if (data.uuid.isNotEmpty()) {
                playerCache[data.uuid] = CachedPlayerData(data, System.currentTimeMillis())
                uuidCache[username.lowercase()] = data.uuid
            }
        }

        return result
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

                val errorData = fetchFromApi(uuid, username) ?: return@withContext PlayerDataObject(
                    uuid = uuid,
                    ign = username,
                    dungeons = null,
                    kuudra = null,
                    isError = true
                )

                playerCache[uuid] = CachedPlayerData(errorData, System.currentTimeMillis())
                return@withContext errorData

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
        uuidCache[username.lowercase()]?.let { return it }

        return try {
            val json = ApiUtils.fetchJson("$MOJANG_API/$username")
            json.get("id")?.asString?.also {
                uuidCache[username.lowercase()] = it
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
        val ownUuid = uuidCache[Vexo.mc.user?.name?.lowercase() ?: return]

        playerCache.entries.removeIf { (uuid, cached) ->
            val expired = uuid != ownUuid && now - cached.lastAccessed >= CACHE_EVICTION_MS
            if (expired) {
                uuidCache.entries.removeIf { it.value == uuid }
                pendingPlayerRequests.remove(uuid)?.cancel()
            }
            expired
        }
    }
}
