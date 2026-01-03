package xyz.vexo.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import kotlinx.coroutines.*
import xyz.vexo.Vexo
import xyz.vexo.events.EventBus
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ServerTickEvent
import java.io.File
import java.util.concurrent.ConcurrentHashMap

object PlayerData {
    private val gson = Gson()
    private val gsonPretty = GsonBuilder().setPrettyPrinting().create()

    private val PLAYER_DATA_DIR = File(Vexo.configDir, "player_data")

    private const val MOJANG_API = "https://api.mojang.com/users/profiles/minecraft"
    private const val PLAYER_API = "https://api.infm7.xyz/player"

    private const val CACHE_EVICTION_MS = 60 * 60 * 1000L // 60 min
    private const val EVICTION_CHECK_INTERVAL_MS = 15 * 60 * 1000L // 15 min

    private var lastEvictionCheck = System.currentTimeMillis()

    private val playerCache = ConcurrentHashMap<String, CachedPlayerData>()

    private data class CachedPlayerData(
        val data: PlayerDataObject,
        var lastAccessed: Long
    )

    /**
     * Main player data class with accessor methods
     */
    class PlayerDataObject(
        val uuid: String,
        val ign: String,
        val dungeons: DungeonsData?,
        val kuudra: KuudraData?,
        val lastUpdated: Long = System.currentTimeMillis(),

    ) {
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

    data class DungeonsData(
        val personalBests: PersonalBests?,
        val catacombsLevel: Int?,
        val totalSecrets: Int?,
        val essenceShopPerks: EssenceShopPerks?
    )

    data class PersonalBests(
        val catacombs: Map<String, Int>?,
        val masterCatacombs: Map<String, Int>?
    )

    data class EssenceShopPerks(
        val helpOfTheFairy: Boolean = false
    )

    data class KuudraData(
        val totalRuns: Map<String, Int>?
    )

    private data class PlayerDataFile(
        val uuid: String,
        val ign: String,
        val dungeons: DungeonsData?,
        val kuudra: KuudraData?,
        val lastUpdated: Long,
    )

    init {
        PLAYER_DATA_DIR.mkdirs()
        EventBus.subscribe(this)
    }

    @EventHandler
    private fun onServerTick(event: ServerTickEvent) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastEvictionCheck >= EVICTION_CHECK_INTERVAL_MS) {
            Vexo.scope.launch {
                evictOldPlayers()
            }
            lastEvictionCheck = currentTime
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
            val data = fetchPlayerData(username)
            callback(data)
        }
    }

    /**
     * Internal suspend function that does the actual loading
     */
    private suspend fun fetchPlayerData(username: String): PlayerDataObject? {
        return withContext(Dispatchers.IO) {
            try {
                val uuid = getUuidFromUsername(username) ?: run {
                    logInfo("Player not found: $username")
                    return@withContext null
                }

                val currentTime = System.currentTimeMillis()

                playerCache[uuid]?.let { cached ->
                    cached.lastAccessed = currentTime
                    return@withContext cached.data
                }

                val fileData = loadFromFile(uuid)

                val apiData = fetchFromApi(uuid, username)

                val mergedData =
                    if (apiData != null && fileData != null) {
                        PlayerDataObject(
                            uuid = apiData.uuid,
                            ign = apiData.ign,
                            dungeons = apiData.dungeons,
                            kuudra = apiData.kuudra,
                            lastUpdated = currentTime,
                        )
                    } else if (apiData != null) {
                        apiData
                    } else if (fileData != null) {
                        fileData
                    } else {
                        return@withContext null
                    }

                saveToFile(mergedData)

                playerCache[uuid] = CachedPlayerData(
                    data = mergedData,
                    lastAccessed = currentTime
                )

                mergedData
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError(e, this@PlayerData)
                null
            }
        }
    }

    /**
     * Gets UUID from Mojang API
     */
    private suspend fun getUuidFromUsername(username: String): String? {
        return try {
            val json = ApiUtils.fetchJson("$MOJANG_API/$username")
            json.get("id")?.asString
        } catch (e: Exception) {
            logError(e, this@PlayerData)
            null
        }
    }

    /**
     * Fetches player data from the API
     */
    private suspend fun fetchFromApi(uuid: String, ign: String): PlayerDataObject? {
        return try {
            val json = ApiUtils.fetchJsonWithRetry("$PLAYER_API?uuid=$uuid", maxRetries = 2)
                ?: return null

            if (!json.get("success")?.asBoolean!!) {
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
     * Parses JSON into PlayerDataObject
     */
    private fun parsePlayerData(uuid: String, ign: String, playerData: JsonObject): PlayerDataObject {
        val dungeonsJson = playerData.getAsJsonObject("dungeons")
        val kuudraJson = playerData.getAsJsonObject("kuudra")

        val dungeons = dungeonsJson?.let {
            DungeonsData(
                personalBests = it.getAsJsonObject("personal_bests")?.let { pb ->
                    PersonalBests(
                        catacombs = pb.getAsJsonObject("catacombs")?.entrySet()
                            ?.associate { (k, v) -> k to v.asInt },
                        masterCatacombs = pb.getAsJsonObject("master_catacombs")?.entrySet()
                            ?.associate { (k, v) -> k to v.asInt }
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

        return PlayerDataObject(uuid, ign, dungeons, kuudra)
    }

    /**
     * Loads player data from file
     */
    private fun loadFromFile(uuid: String): PlayerDataObject? {
        val file = File(PLAYER_DATA_DIR, "$uuid.json")
        if (!file.exists()) return null

        return try {
            val fileData = gson.fromJson(file.readText(), PlayerDataFile::class.java)
            PlayerDataObject(
                uuid = fileData.uuid,
                ign = fileData.ign,
                dungeons = fileData.dungeons,
                kuudra = fileData.kuudra,
                lastUpdated = fileData.lastUpdated,
            )
        } catch (e: Exception) {
            logError(e, this@PlayerData)
            null
        }
    }

    /**
     * Saves player data to file
     */
    private suspend fun saveToFile(data: PlayerDataObject) = withContext(Dispatchers.IO) {
        try {
            if (!PLAYER_DATA_DIR.exists()) {
                PLAYER_DATA_DIR.mkdirs()
            }

            val file = File(PLAYER_DATA_DIR, "${data.uuid}.json")
            val fileData = PlayerDataFile(
                uuid = data.uuid,
                ign = data.ign,
                dungeons = data.dungeons,
                kuudra = data.kuudra,
                lastUpdated = data.lastUpdated,

            )
            file.writeText(gsonPretty.toJson(fileData))
        } catch (e: Exception) {
            logError(e, this@PlayerData)
        }
    }

    /**
     * Updates data for a player (without API call)
     */
    suspend fun updateData(username: String, update: (PlayerDataObject) -> Unit) {
        withContext(Dispatchers.IO) {
            try {
                val uuid = getUuidFromUsername(username) ?: return@withContext

                val data = playerCache[uuid]?.data ?: loadFromFile(uuid) ?: run {
                    return@withContext
                }

                update(data)

                saveToFile(data)
                playerCache[uuid] = CachedPlayerData(data, System.currentTimeMillis())
            } catch (e: Exception) {
                logError(e, this@PlayerData)
            }
        }
    }

    /**
     * Removes players from cache that haven't been accessed in 60 minutes
     */
    private fun evictOldPlayers() {
        val currentTime = System.currentTimeMillis()
        val toRemove = mutableListOf<String>()

        playerCache.forEach { (uuid, cached) ->
            val timeSinceLastAccess = currentTime - cached.lastAccessed
            if (timeSinceLastAccess >= CACHE_EVICTION_MS) {
                toRemove.add(uuid)
            }
        }

        if (toRemove.isNotEmpty()) {
            toRemove.forEach { playerCache.remove(it) }
        }
    }

    /**
     * Forces immediate save of a specific player
     */
    suspend fun forceSave(uuid: String) {
        playerCache[uuid]?.let { cached ->
            saveToFile(cached.data)
        }
    }

    /**
     * Clears the entire cache
     */
    fun clearCache() {
        playerCache.clear()
    }
}