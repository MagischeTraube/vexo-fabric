package xyz.vexo.utils

import com.google.gson.reflect.TypeToken
import com.google.gson.annotations.SerializedName
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import xyz.vexo.Vexo
import xyz.vexo.events.EventBus
import xyz.vexo.events.impl.PriceDataUpdateEvent

object PriceUtils : IInitializable {
    private val gson get() = Vexo.gson
    private val PRICE_DATA_FILE = File(Vexo.configDir, "price_data.json")

    private const val API_URL = "https://api.infm7.xyz/prices"

    private var lastFetchTime: Long = 0
    private const val FETCH_INTERVAL_MS = 15 * 60 * 1000L
    private const val FORCE_FETCH_INTERVAL_MS = 5 * 60 * 1000L

    private val cachedBazaarData = ConcurrentHashMap<String, BazaarData>()
    private val cachedAuctionData = ConcurrentHashMap<String, AuctionData>()

    private val fetchMutex = Mutex()

    data class BazaarData(
        val sellOfferPrice: Int? = null,
        val instaSellPrice: Int? = null
    )

    data class AuctionData(
        val lowestBin: Int? = null
    )

    private data class PriceBackup(
        val lastFetchTime: Long,
        val bazaar: Map<String, BazaarData>,
        val auctions: Map<String, AuctionData>
    )

    override fun init() {
        loadCachedPriceData()
        EventBus.subscribe(this)

        Vexo.scope.launch {
            safeFetchPrices()
            while (isActive) {
                delay(FETCH_INTERVAL_MS)
                safeFetchPrices()
            }
        }
    }

    /**
     * Forces a price fetch.
     */
    fun forceFetch() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastFetchTime < FORCE_FETCH_INTERVAL_MS) return

        Vexo.scope.launch {
            safeFetchPrices()
        }
    }

    /**
     * Safely fetches prices with mutex protection.
     */
    private suspend fun safeFetchPrices() {
        if (!fetchMutex.tryLock()) return

        try {
            fetchPrices()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logError(e, this@PriceUtils)
        } finally {
            fetchMutex.unlock()
        }
    }

    /**
     * Fetches the prices from the API.
     */
    private suspend fun fetchPrices() {
        val rootJson = ApiUtils.fetchJsonWithRetry(API_URL, maxRetries = 3)
            ?: return

        if (!rootJson["success"].asBoolean) return

        val data = rootJson.getAsJsonObject("data") ?: return

        val newBazaarData = ConcurrentHashMap<String, BazaarData>()
        val newAuctionData = ConcurrentHashMap<String, AuctionData>()

        // Bazaar
        data.getAsJsonObject("bazaar")?.entrySet()?.forEach { (itemId, element) ->
            val obj = element.asJsonObject
            newBazaarData[itemId] = BazaarData(
                sellOfferPrice = obj.get("sellOfferPrice")?.asInt,
                instaSellPrice = obj.get("instaSellPrice")?.asInt
            )
        }

        // Auctions
        data.getAsJsonObject("auctions")?.entrySet()?.forEach { (itemId, element) ->
            val obj = element.asJsonObject
            newAuctionData[itemId] = AuctionData(
                lowestBin = obj.get("lowestBin")?.asInt
            )
        }

        cachedBazaarData.clear()
        cachedBazaarData.putAll(newBazaarData)

        cachedAuctionData.clear()
        cachedAuctionData.putAll(newAuctionData)

        lastFetchTime = System.currentTimeMillis()

        saveCachedPriceData()
        PriceDataUpdateEvent.postAndCatch()
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
        val bazaarItem = cachedBazaarData[skyblockID]
        if (bazaarItem != null) {
            return if (sellOffer) bazaarItem.sellOfferPrice ?: 0 else bazaarItem.instaSellPrice ?: 0
        }

        val auctionItem = cachedAuctionData[skyblockID] ?: return 0
        val rawPrice = auctionItem.lowestBin ?: return 0

        return if (includeTaxes) {
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

            val backup = PriceBackup(
                lastFetchTime = lastFetchTime,
                bazaar = cachedBazaarData.toMap(),
                auctions = cachedAuctionData.toMap()
            )

            PRICE_DATA_FILE.writeText(gson.toJson(backup))
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
            val type = object : TypeToken<PriceBackup>() {}.type
            val backup: PriceBackup = gson.fromJson(PRICE_DATA_FILE.readText(), type)

            cachedBazaarData.putAll(backup.bazaar)
            cachedAuctionData.putAll(backup.auctions)
            lastFetchTime = backup.lastFetchTime

            val minutesAgo = (System.currentTimeMillis() - lastFetchTime) / (60 * 1000)
            val totalItems = backup.bazaar.size + backup.auctions.size
            logInfo("Loaded $totalItems cached prices (${minutesAgo}m old)")
        } catch (e: Exception) {
            logError(e, this@PriceUtils)
        }
    }
}
