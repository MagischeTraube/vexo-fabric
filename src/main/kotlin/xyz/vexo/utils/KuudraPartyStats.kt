package xyz.vexo.utils

import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch
import xyz.vexo.Vexo


object KuudraPartyStats {
    private const val KUUDRA_API = "https://api.infm7.xyz/kuudra"
    private const val MAX_CACHE_SIZE = 300


    data class Stats(
        val infernalRuns: Int,
        val cataLevel: Int,
        val magicalPower: Int,
        val rendBone: Boolean,
        val isError: Boolean = false,
    )

    private val ERROR = Stats(0, 0, 0, false, isError = true)

    private val cache = ConcurrentHashMap<String, Stats>()
    private val pending = ConcurrentHashMap.newKeySet<String>()


    fun get(name: String): Stats? {
        val key = name.lowercase()
        cache[key]?.let { return it }
        fetch(name, key)
        return null
    }

    fun clear() = cache.clear()

    private fun fetch(name: String, key: String) {
        if (!pending.add(key)) return
        Vexo.scope.launch {
            try {
                val uuid = PlayerData.getUuidFromUsername(name) ?: run { put(key, ERROR); return@launch }
                val json = ApiUtils.fetchJsonWithRetry("$KUUDRA_API?uuid=$uuid", maxRetries = 2)
                val data = json?.getAsJsonObject("data")
                if (json?.get("success")?.asBoolean != true || data == null) {
                    put(key, ERROR)
                    return@launch
                }
                put(key, parse(data))
            } catch (e: Exception) {
                logError(e, this@KuudraPartyStats)
                put(key, ERROR)
            } finally {
                pending.remove(key)
            }
        }
    }

    private fun put(key: String, stats: Stats) {
        if (cache.size >= MAX_CACHE_SIZE) cache.clear()
        cache[key] = stats
    }

    private fun parse(data: JsonObject): Stats {
        fun int(k: String): Int = data.get(k)?.takeUnless { it.isJsonNull }?.asInt ?: 0
        fun flag(k: String): Boolean {
            val el = data.get(k) ?: return false
            return el.isJsonPrimitive && el.asJsonPrimitive.isBoolean && el.asBoolean
        }
        return Stats(int("infernal_runs"), int("cata_level"), int("magical_power"), flag("rend_bone"))
    }
}
