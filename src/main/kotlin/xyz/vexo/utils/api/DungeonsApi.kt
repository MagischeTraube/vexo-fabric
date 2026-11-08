package xyz.vexo.utils.api

import com.google.gson.JsonObject

/**
 * Dungeon-related stats of a single player.
 *
 * @property catacombsLevel The player's catacombs level
 * @property totalSecrets The player's total secrets
 * @property hasFairyPerk Whether the player has the Help of the Fairy perk
 * @property normalPbs Personal best times (ms) per floor for normal catacombs
 * @property masterPbs Personal best times (ms) per floor for master catacombs
 */
data class DungeonProfile(
    val catacombsLevel: Int,
    val totalSecrets: Int,
    val hasFairyPerk: Boolean,
    private val normalPbs: Map<String, Int>,
    private val masterPbs: Map<String, Int>
) {
    /**
     * Gets the personal best time of a floor.
     *
     * @param floor The floor number (0 = Entrance)
     * @param master Whether to look up master mode instead of normal mode
     * @return The best time in ms, or null if the player has no PB on that floor
     */
    fun bestTime(floor: Int, master: Boolean): Int? =
        (if (master) masterPbs else normalPbs)[floor.toString()]
}

/**
 * Typed access to the [ApiEndpoint.DUNGEONS] endpoint.
 *
 * Expected query: `uuid` (the player's UUID without dashes).
 */
object DungeonsApi {

    /**
     * Fetches the dungeon profile of a player.
     *
     * @param uuid The player's UUID
     * @param maxRetries How often a failed request is retried
     * @return The parsed profile, or null if the request failed
     */
    suspend fun fetch(uuid: String, maxRetries: Int = 2): DungeonProfile? {
        val data = VexoApi.get(ApiEndpoint.DUNGEONS, mapOf("uuid" to uuid), maxRetries) ?: return null
        return parse(data)
    }

    /**
     * Parses the dungeon profile from the API response. Missing fields default to zero/empty.
     *
     * @param data The "data" object of the API response
     * @return The parsed profile
     */
    private fun parse(data: JsonObject): DungeonProfile {
        val dungeons = data.getAsJsonObject("dungeons")
        val personalBests = dungeons?.getAsJsonObject("personal_bests")

        return DungeonProfile(
            catacombsLevel = dungeons?.intOrNull("catacombs_level") ?: 0,
            totalSecrets = dungeons?.intOrNull("total_secrets") ?: 0,
            hasFairyPerk = dungeons?.getAsJsonObject("essence_shop_perks")
                ?.get("HELP_OF_THE_FAIRY")?.takeIf { !it.isJsonNull }?.asBoolean ?: false,
            normalPbs = personalBests.intMap("catacombs"),
            masterPbs = personalBests.intMap("master_catacombs")
        )
    }

    private fun JsonObject.intOrNull(name: String): Int? =
        get(name)?.takeIf { !it.isJsonNull }?.asInt

    /** Reads a JSON object of numbers into a map, skipping null values. */
    private fun JsonObject?.intMap(name: String): Map<String, Int> =
        this?.getAsJsonObject(name)?.entrySet()
            ?.filter { !it.value.isJsonNull }
            ?.associate { it.key to it.value.asInt }
            ?: emptyMap()
}