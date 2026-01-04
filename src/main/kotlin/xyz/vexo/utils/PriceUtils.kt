package xyz.vexo.utils

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.*
import xyz.vexo.Vexo
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import xyz.vexo.events.EventBus
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.PriceDataUpdateEvent

object PriceUtils : IInitializable {
    private val gson = Gson()
    private val PRICE_DATA_FILE = File(Vexo.configDir, "price_data.json")

    private const val API_URL = "https://api.infm7.xyz/prices"

    private var lastFetchTime: Long = 0
    private const val FETCH_INTERVAL_MS = 15 * 60 * 1000L
    private var forceFetch = false
    private const val FORCE_FETCH_INTERVAL_MS = 5 * 60 * 1000L

    private val cachedPriceData = ConcurrentHashMap<String, PriceData>()
    private var currentFetchJob: Job? = null

    data class PriceData(
        val sellLocation: String,
        val sellOfferPrice: Int? = null,
        val instaSellPrice: Int? = null,
        val lowestBin: Int? = null
    )

    private data class PriceBackup(
        val lastFetchTime: Long,
        val prices: Map<String, PriceData>
    )

    override fun init() {
        loadCachedPriceData()
        EventBus.subscribe(this)
    }

    @EventHandler
    private fun onServerTick(event: ClientTickEvent) {
        val currentTime = System.currentTimeMillis()

        val shouldFetch = (forceFetch && currentTime - lastFetchTime >= FORCE_FETCH_INTERVAL_MS) ||
                (currentTime - lastFetchTime >= FETCH_INTERVAL_MS)

        if (shouldFetch) {
            fetchPrices()
            forceFetch = false
            lastFetchTime = currentTime
        }
    }

    /**
     * Forces a price fetch.
     */
    fun forceFetch() {
        forceFetch = true
    }

    /**
     * Fetches the prices from the API.
     */
    private fun fetchPrices() {
        if (currentFetchJob?.isActive == true) return

        currentFetchJob = Vexo.scope.launch {
            try {
                val rootJson = ApiUtils.fetchJsonWithRetry(API_URL, maxRetries = 3)
                    ?: return@launch

                if (!rootJson.get("success").asBoolean) {
                    return@launch
                }

                val pricesJson = rootJson.getAsJsonObject("prices")
                    ?: return@launch
                val newPriceData = ConcurrentHashMap<String, PriceData>()

                for ((itemId, dataElem) in pricesJson.entrySet()) {
                    val dataObj = dataElem.asJsonObject
                    val sellLocation = dataObj.get("sellLocation")?.asString ?: continue

                    newPriceData[itemId] = PriceData(
                        sellLocation = sellLocation,
                        sellOfferPrice = dataObj.get("sellOfferPrice")?.asInt,
                        instaSellPrice = dataObj.get("instaSellPrice")?.asInt,
                        lowestBin = dataObj.get("lowestBin")?.asInt
                    )
                }

                cachedPriceData.clear()
                cachedPriceData.putAll(newPriceData)
                launch { saveCachedPriceData() }

                PriceDataUpdateEvent.postAndCatch()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError(e, this@PriceUtils)
            }
        }
    }

    /**
     * Gets the price of an item.
     *
     * @param skyblockID The skyblock ID of the item.
     * @param sellOffer Whether to get the sell offer price.
     * @param includeTaxes Whether to include taxes.
     * @return The price of the item.
     */
    fun getPrice(skyblockID: String, sellOffer: Boolean, includeTaxes: Boolean): Int {
        val itemData = cachedPriceData[skyblockID] ?: return 0

        val rawPrice = when (itemData.sellLocation) {
            "auction_house" -> itemData.lowestBin
            "bazaar" -> if (sellOffer) itemData.sellOfferPrice else itemData.instaSellPrice
            else -> null
        } ?: return 0

        return if (itemData.sellLocation == "auction_house" && includeTaxes) {
            calculateBinAfterTaxes(rawPrice.toDouble()).toInt()
        } else {
            rawPrice
        }
    }

    /**
     * Calculates the price after taxes.
     *
     * @param price The price to calculate.
     * @return The price after taxes.
     */
    private fun calculateBinAfterTaxes(price: Double): Double {
        if (price <= 0) return 0.0

        val startFee = when {
            price > 100_000_000 -> price * 0.025
            price >= 10_000_000 -> price * 0.02
            else -> price * 0.01
        }

        var afterStartFee = price - startFee

        if (price > 1_000_000) {
            val collectTax = afterStartFee * 0.01
            afterStartFee = (afterStartFee - collectTax).coerceAtLeast(1_000_000.0)
        }

        return afterStartFee
    }

    /**
     * Saves the cached price data to a file.
     */
    private suspend fun saveCachedPriceData() = withContext(Dispatchers.IO) {
        try {
            PRICE_DATA_FILE.parentFile?.mkdirs()

            val gsonPretty = GsonBuilder().setPrettyPrinting().create()
            val backup = PriceBackup(
                lastFetchTime = lastFetchTime,
                prices = cachedPriceData.toMap()
            )

            PRICE_DATA_FILE.writeText(gsonPretty.toJson(backup))
        } catch (e: Exception) {
            logError(e, this@PriceUtils)
        }
    }

    /**
     * Loads the cached price data from a file.
     */
    private fun loadCachedPriceData() {
        if (!PRICE_DATA_FILE.exists()) {
            logInfo("No cached price data found, will fetch on first tick")
            return
        }

        try {
            val type = object : com.google.gson.reflect.TypeToken<PriceBackup>() {}.type
            val backup: PriceBackup = gson.fromJson(PRICE_DATA_FILE.readText(), type)

            cachedPriceData.putAll(backup.prices)
            lastFetchTime = backup.lastFetchTime

            val minutesAgo = (System.currentTimeMillis() - lastFetchTime) / (60 * 1000)
            logInfo("Loaded ${backup.prices.size} cached prices (${minutesAgo}m old)")
        } catch (e: Exception) {
            logError(e, this@PriceUtils)
        }
    }
}