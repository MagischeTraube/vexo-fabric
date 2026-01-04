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
    private const val MOJANG_API = "https://api.mojang.com/users/profiles/minecraft"
    private const val PLAYER_API = "https://api.infm7.xyz/player"

    private const val CACHE_EVICTION_MS = 60 * 60 * 1000L // 60 min
    private const val EVICTION_CHECK_INTERVAL_MS = 15 * 60 * 1000L // 15 min

    private var lastEvictionCheck = System.currentTimeMillis()

    val playerCache = ConcurrentHashMap<String, CachedPlayerData>()
    val uuidCache = ConcurrentHashMap<String, String>()


    data class CachedPlayerData(
        val data: PlayerDataObject,
        var lastAccessed: Long
    )

    /**
     * Main player data class with accessor methods
     */
    data class PlayerDataObject(
        val uuid: String,
        val ign: String,
        val dungeons: DungeonsData?,
        val kuudra: KuudraData?,
        val lastUpdated: Long = System.currentTimeMillis(),
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


    init {
        EventBus.subscribe(this)
        getPlayerData(Vexo.mc.user?.name ?: "") { data ->
            if (data != null) {
                playerCache[data.uuid] = CachedPlayerData(data, System.currentTimeMillis())
            }
        }
    }

    @EventHandler
    private fun onServerTick(event: ServerTickEvent) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastEvictionCheck >= EVICTION_CHECK_INTERVAL_MS) {
            evictOldPlayers()
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
     * Cache + API only (no file storage)
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

                val apiData = fetchFromApi(uuid, username) ?: return@withContext null

                val data = apiData.copy(lastUpdated = currentTime)

                playerCache[uuid] = CachedPlayerData(
                    data = data,
                    lastAccessed = currentTime
                )

                data
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

        return PlayerDataObject(uuid, ign, dungeons, kuudra)
    }

    /**
     * Removes players from cache that haven't been accessed in 60 minutes
     */
    private fun evictOldPlayers() {
        val currentTime = System.currentTimeMillis()
        val toRemove = mutableListOf<String>()

        val ownName = Vexo.mc.user?.name ?: ""
        val ownUuid = uuidCache[ownName.lowercase()]

        playerCache.forEach { (uuid, cached) ->
            if (uuid == ownUuid) return@forEach

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
     * Clears the entire cache
     */
    fun clearCache() {
        playerCache.clear()
    }


}