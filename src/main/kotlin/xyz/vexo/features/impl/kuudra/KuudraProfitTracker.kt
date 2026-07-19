package xyz.vexo.features.impl.kuudra

import java.awt.Color
import kotlin.math.abs
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.GuiRenderEvent
import xyz.vexo.events.impl.PriceDataUpdateEvent
import xyz.vexo.features.Module
import xyz.vexo.mixin.AbstractContainerScreenAccessor
import xyz.vexo.utils.PriceUtils
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting


object KuudraProfitTracker : Module(
    name = "Kuudra Profit Tracker",
    description = "Shows Croesus chest profit for Kuudra runs",
    toggled = false
) {
    private val sellOffer by BooleanSetting(
        "Sell Offer",
        "Use Bazaar sell-offer prices instead of insta-sell",
        default = false
    )
    private val includeTaxes by BooleanSetting(
        "Include Taxes",
        "Subtract AH taxes from the item value",
        default = true
    )
    private val lvl100Kuudra by BooleanSetting(
        "Lvl 100 Kuudra",
        "Kuudra Collection Lvl 100: +20% essence",
        default = false
    )
    private val armorSalvage by BooleanSetting(
        "Armor Salvage",
        "Value Kuudra armor by its Crimson Essence salvage value",
        default = true
    )
    private val salvagePerk by BooleanSetting(
        "Salvage Perk (+20%)",
        "Account for the +20% salvage essence perk",
        default = true
    ).apply { dependsOn { armorSalvage } }
    private val valueMode by SelectorSetting(
        "Value Mode",
        "Actual = this chest's contents; Expected = average value from drop chances",
        default = "Actual",
        options = listOf("Actual", "Expected")
    )
    private val showBreakdown by BooleanSetting(
        "Show Breakdown",
        "Panel next to the GUI showing how the profit is calculated",
        default = true
    )
    private val breakdownSide by SelectorSetting(
        "Breakdown Side",
        "Which side of the GUI the breakdown panel appears on",
        default = "Right",
        options = listOf("Right", "Left", "Auto")
    ).apply { dependsOn { showBreakdown } }
    private val highlightColor = ColorSetting(
        "Highlight",
        "Color of the best chest highlight",
        default = Color(18, 250, 0, 120)
    )
    private val highlightCroesus by BooleanSetting(
        "Highlight Croesus",
        "Tints runs in the Croesus menu by chest status",
        default = true
    )
    private val openableColor = ColorSetting(
        "Color: Openable",
        "Run with chests left to open (\"Chests expire in ...\")",
        default = Color(18, 250, 0, 130)
    ).apply { dependsOn { highlightCroesus } }
    private val doneColor = ColorSetting(
        "Color: Done",
        "Run with no chests left (\"No more chests to open!\")",
        default = Color(78, 0, 2, 150)
    ).apply { dependsOn { highlightCroesus } }



    private val QTY_REGEX = Regex("""[x×]\s*([\d,]+)\s*$""")
    private val COINS_REGEX = Regex("""([\d,]+)\s*Coins?""", RegexOption.IGNORE_CASE)
    private val KUUDRA_TIER_TITLE = Regex("""Kuudra - (Basic|Hot|Burning|Fiery|Infernal)""")
    private val ENCHANT_BOOK_REGEX = Regex("""Enchanted Book \((.+) ([IVXLCDM]+)\)""")

    private val NAME_TO_ID = mapOf(
        "Kuudra Teeth" to "KUUDRA_TEETH",
        "Kuudra Mandible" to "KUUDRA_MANDIBLE",
        "Kuudra Tentacle" to "KUUDRA_TENTACLE",
        "Kuudra Core" to "KUUDRA_CORE",
        "Burning Kuudra Core" to "BURNING_KUUDRA_CORE",
        "Hot Potato Book" to "HOT_POTATO_BOOK",
        "Fuming Potato Book" to "FUMING_POTATO_BOOK",
        "Recombobulator 3000" to "RECOMBOBULATOR_3000",
        "Heavy Pearl" to "HEAVY_PEARL",
        "Tormentor" to "TORMENTOR",
        "Mandraa" to "MANDRAA",
        "Kismet Feather" to "KISMET_FEATHER",
        "Ananke Feather" to "ANANKE_FEATHER",
        "Hellstorm Staff" to "HELLSTORM_STAFF",
        "Wheel of Fate" to "WHEEL_OF_FATE",
        "Cinderbat Shard" to "SHARD_CINDER_BAT",
        "Wither Spectre Shard" to "SHARD_WITHER_SPECTER",
    )

    private val ZERO_VALUE = setOf("Magma Slug Shard", "Dusty Travel Scroll to the Kuudra Skull")

    private data class KeyRecipe(val coins: Long, val components: List<Pair<String, Int>>)

    private const val BASE_CRIMSON_ESSENCE = 100
    private val ESSENCE_PER_STAR = intArrayOf(15, 17, 20, 22, 25, 27, 30)

    private val KUUDRA_ARMOR: Set<String> = buildSet {
        for (set in listOf("Aurora", "Crimson", "Terror", "Hollow", "Fervor")) {
            for (piece in listOf("Helmet", "Chestplate", "Leggings", "Boots")) add("$set $piece")
        }
    }

    private val SALVAGE_ESSENCE_OVERRIDE: Map<String, Long> = mapOf(
        "Aurora Staff" to 500,
        "Hollow Wand" to 500,
        "Molten Belt" to 500,
        "Molten Bracelet" to 500,
        "Molten Cloak" to 500,
        "Molten Necklace" to 500,
        "Kuudra Mandible" to 500
    )
    private val SALVAGEABLE_EXTRA: Set<String> = SALVAGE_ESSENCE_OVERRIDE.keys

    private val KEY_RECIPES = mapOf(
        "Infernal Kuudra Key" to KeyRecipe(
            coins = 2_328_000L,
            components = listOf("ENCHANTED_MYCELIUM" to 80, "CORRUPTED_NETHER_STAR" to 2)
        ),
    )


    private data class Drop(val name: String, val qty: Int, val chancePct: Double)

    private data class TierTable(
        val guaranteed: List<Pair<String, Int>>,
        val slot1: List<Drop>,
        val slot2: List<Drop>
    )

    private val KEY_TIER_REGEX = Regex("(Basic|Hot|Burning|Fiery|Infernal) Kuudra Key")


    private val INFERNAL_GUARANTEED_DROPS = listOf(
        "Crimson Essence" to 2000,
        "Kuudra Teeth" to 3,
        "Kraken Shard" to 1,
    )

    private val INFERNAL_SLOT1: List<Drop> = buildList {
        for (set in listOf("Aurora", "Crimson", "Fervor", "Hollow", "Terror")) {
            for (piece in listOf("Helmet", "Chestplate", "Leggings", "Boots")) add(Drop("$set $piece", 1, 2.96))
        }
        addAll(
            listOf(
                Drop("Ananke Shard", 1, 0.05),
                Drop("Hellstorm Staff", 1, 0.1),
                Drop("Tormentor", 1, 0.1),
                Drop("Kismet Feather", 1, 0.43),
                Drop("Burning Kuudra Core", 1, 0.51),
                Drop("Daemon Shard", 1, 0.51),
                Drop("Lord Jawbus Shard", 1, 0.51),
                Drop("Moltenfish Shard", 1, 0.51),
                Drop("Cinderbat Shard", 1, 0.67),
                Drop("Taurus Shard", 1, 0.67),
                Drop("Hollow Wand", 1, 0.72),
                Drop("Aurora Staff", 1, 0.72),
                Drop("Mandraa", 1, 1.23),
                Drop("Molten Belt", 1, 1.23),
                Drop("Molten Bracelet", 1, 1.23),
                Drop("Molten Cloak", 1, 1.23),
                Drop("Molten Necklace", 1, 1.23),
                Drop("XYZ Shard", 1, 1.29),
                Drop("Hellwisp Shard", 1, 1.59),
                Drop("Barbarian Duke X Shard", 1, 1.95),
                Drop("Fire Eel Shard", 1, 1.95),
                Drop("Flare Shard", 1, 1.95),
                Drop("Lava Flame Shard", 1, 2.57),
                Drop("Kada Knight Shard", 1, 3.14),
                Drop("Matcho Shard", 1, 3.14),
                Drop("Wither Specter Shard", 1, 3.14),
                Drop("Magma Slug Shard", 1, 3.86),
                Drop("Bezal Shard", 1, 4.63),
            )
        )
    }

    private val INFERNAL_SLOT2: List<Drop> = listOf(
        Drop("Kuudra Tentacle", 1, 10.0),
        Drop("Kuudra Teeth", 100, 0.06),
        Drop("Crimson Essence", 10000, 0.06),
        Drop("Ananke Shard", 1, 0.06),
        Drop("Enchanted Book (Inferno I)", 1, 0.23),
        Drop("Enchanted Book (Fatal Tempo I)", 1, 0.23),
        Drop("Kuudra Teeth", 25, 0.59),
        Drop("Crimson Essence", 2500, 0.59),
        Drop("Heavy Pearl", 10, 0.59),
        Drop("Lord Jawbus Shard", 1, 0.59),
        Drop("Daemon Shard", 1, 0.59),
        Drop("Moltenfish Shard", 1, 0.59),
        Drop("Taurus Shard", 1, 0.77),
        Drop("Cinderbat Shard", 1, 0.77),
        Drop("XYZ Shard", 1, 1.48),
        Drop("Kuudra Mandible", 1, 1.75),
        Drop("Hellwisp Shard", 1, 1.84),
        Drop("Fire Eel Shard", 1, 1.97),
        Drop("Flare Shard", 1, 1.97),
        Drop("Lava Flame Shard", 1, 2.46),
        Drop("Barbarian Duke X Shard", 1, 2.46),
        Drop("Kada Knight Shard", 1, 2.68),
        Drop("Matcho Shard", 1, 2.68),
        Drop("Wither Specter Shard", 1, 2.68),
        Drop("Magma Slug Shard", 1, 3.29),
        Drop("Bezal Shard", 1, 3.95),
    )

    private val TIER_TABLES = mapOf(
        "Infernal" to TierTable(INFERNAL_GUARANTEED_DROPS, INFERNAL_SLOT1, INFERNAL_SLOT2),
    )

    /**
     * A single priced row of the profit GUI; [parts] are the sub-rows (e.g. a key's recipe).
     * [error] marks rows containing an item that has no price in the API (rendered red).
     */
    private data class Entry(
        val label: String,
        val value: Long,
        val parts: List<Entry> = emptyList(),
        val error: Boolean = false
    )

    private data class Breakdown(val loot: List<Entry>, val costs: List<Entry>, val total: Long) {
        val hasApiError: Boolean get() = loot.any { it.error } || costs.any { it.error }

        val missingInfo: List<String> get() = (loot + costs).filter { it.error }.map { it.label }
    }

    // Cache the computed best chest per GUI content snapshot so we only re-parse when slots change.
    private var cacheHash = 0
    private var cacheResult: Pair<Slot, Breakdown>? = null


    private val ERROR_HIGHLIGHT_COLOR = Color(250, 18, 0, 120).rgb

    private val reportedErrors = HashSet<String>()

    /** Drops the cached best-chest result when the module is turned off. */
    override fun onDisable() {
        super.onDisable()
        invalidate()
    }

    /**
     * Invalidates the cache whenever fresh prices arrive, so the profit re-computes with them.
     *
     * @param event the price-data refresh notification
     */
    @EventHandler
    fun onPriceUpdate(event: PriceDataUpdateEvent) = invalidate()

    /**
     * Draws the profit overlay each frame: Croesus run tints, the best-chest highlight, and the
     * optional breakdown panel. Does nothing unless the open screen is a Croesus or Kuudra chest GUI.
     *
     * @param event the GUI render event carrying the screen and draw context
     */
    @EventHandler
    fun onGuiRender(event: GuiRenderEvent) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        val title = screen.title.string.removeFormatting()
        val accessor = screen as AbstractContainerScreenAccessor

        if (title == "Croesus") {
            if (highlightCroesus) {
                renderCroesusHighlights(event.context, accessor.vexoLeftPos(), accessor.vexoTopPos(), screen)
            }
            return
        }

        if (title != "Paid Chest" && title != "Free Chest" && !KUUDRA_TIER_TITLE.containsMatchIn(title)) {
            return
        }

        val hash = contentHash(screen)
        if (hash != cacheHash) {
            cacheHash = hash
            cacheResult = bestChest(screen)
        }

        val (slot, breakdown) = cacheResult ?: return
        val ctx = event.context

        if (breakdown.hasApiError) reportMissing(breakdown.missingInfo)

        renderHighlight(ctx, accessor.vexoLeftPos(), accessor.vexoTopPos(), slot, breakdown.total, breakdown.hasApiError)

        if (showBreakdown) {
            renderBreakdown(ctx, accessor.vexoLeftPos(), accessor.vexoTopPos(), accessor.vexoImageWidth(), screen.width, breakdown)
        }
    }

    /**
     * Tints each run in the Croesus menu by its chest status: green when chests are still openable,
     * dark red when the run is done. Runs with neither lore line are left untouched.
     *
     * @param ctx draw context to fill the slot backgrounds into
     * @param leftPos left pixel edge of the GUI
     * @param topPos top pixel edge of the GUI
     * @param screen the Croesus screen whose slots are scanned
     */
    private fun renderCroesusHighlights(
        ctx: GuiGraphicsExtractor,
        leftPos: Int,
        topPos: Int,
        screen: AbstractContainerScreen<*>
    ) {
        for (slot in screen.menu.slots) {
            if (!slot.hasItem()) continue
            val lore = slot.item.get(DataComponents.LORE)?.styledLines()
                ?.map { it.string.removeFormatting() }
                ?: continue

            val color = when {
                lore.any { "No more chests to open" in it } -> doneColor.getRGBA()
                lore.any { "Chests expire in" in it } -> openableColor.getRGBA()
                else -> continue
            }

            val x = leftPos + slot.x
            val y = topPos + slot.y
            ctx.fill(x, y, x + 16, y + 16, color)
        }
    }

    /**
     * Single-unit coin value of the loot named [name] using this tracker's price settings.
     *
     * @param name the loot display name to price
     * @return the value of one unit, or null if the item is unpriced
     */
    fun priceOf(name: String): Long? = lootValueOrNull(name, 1L)

    /**
     * Profit total of one chest item (its Contents minus Cost).
     *
     * @param stack the chest item to value
     * @return the profit, or null if it isn't a priceable chest
     */
    fun chestProfit(stack: ItemStack): Long? = chestBreakdown(stack)?.total

    /**
     * Calculates the total profit of a chest, but only once all of its items are priced.
     *
     * @param stack the chest item whose Contents lore holds the loot to value
     * @return the chest profit, or null while any content item still lacks an API price
     */
    fun chestProfitReady(stack: ItemStack): Long? =
        chestBreakdown(stack)?.takeUnless { it.hasApiError }?.total

    /**
     * Loot-item display names (quantity stripped) read from a chest item's Contents lore.
     *
     * @param stack the chest item to read
     * @return the loot names, or an empty list if the item has no Contents lore
     */
    fun chestLootNames(stack: ItemStack): List<String> {
        val lore = stack.get(DataComponents.LORE)?.styledLines()
            ?.map { it.string.removeFormatting().trim() } ?: return emptyList()
        val contentsIdx = lore.indexOf("Contents")
        if (contentsIdx == -1) return emptyList()
        return lore.drop(contentsIdx + 1)
            .takeWhile { it.isNotBlank() && it != "Cost" }
            .map { line ->
                val qty = QTY_REGEX.find(line)
                (if (qty != null) line.substring(0, qty.range.first) else line).trim()
            }
    }

    /**
     * Picks the single most profitable chest in the current GUI.
     *
     * @param screen the chest GUI whose slots are scanned
     * @return the winning slot paired with its breakdown, or null if no slot holds a priceable chest
     */
    private fun bestChest(screen: AbstractContainerScreen<*>): Pair<Slot, Breakdown>? {
        var best: Pair<Slot, Breakdown>? = null
        for (slot in screen.menu.slots) {
            if (!slot.hasItem()) continue
            val breakdown = chestBreakdown(slot.item) ?: continue
            if (best == null || breakdown.total > best.second.total) best = slot to breakdown
        }
        return best
    }

    /**
     * Parses the `Contents`/`Cost` lore of a chest item into a priced breakdown. In "Expected" value
     * mode the actual contents are replaced by the tier's average chest value.
     *
     * @param stack the chest item to parse
     * @return the priced breakdown, or null if the item has no `Contents` lore to value
     */
    private fun chestBreakdown(stack: ItemStack): Breakdown? {
        val lore = stack.get(DataComponents.LORE)?.styledLines()
            ?.map { it.string.removeFormatting().trim() }
            ?: return null

        val contentsIdx = lore.indexOf("Contents")
        if (contentsIdx == -1) return null

        val lootLines = lore.drop(contentsIdx + 1).takeWhile { it.isNotBlank() && it != "Cost" }
        if (lootLines.isEmpty()) return null

        val costIdx = lore.indexOf("Cost")
        val costLines = if (costIdx >= 0) lore.drop(costIdx + 1).takeWhile { it.isNotBlank() } else emptyList()

        val costs = costLines.map { costEntry(it) }

        // Expected-value mode: replace the actual contents with the tier's average chest value.
        if (valueMode == "Expected") {
            val table = costLines.firstNotNullOfOrNull { KEY_TIER_REGEX.find(it)?.groupValues?.get(1) }
                ?.let { TIER_TABLES[it] }
            if (table != null) return expectedBreakdown(table, costs)
        }

        val loot = lootLines.map { lootEntry(it) }
        val total = loot.sumOf { it.value } - costs.sumOf { it.value }
        return Breakdown(loot, costs, total)
    }

    /**
     * Builds the average-value breakdown for a tier: guaranteed loot plus the expected value of each
     * of the two slot rolls, minus the given costs.
     *
     * @param table the tier's guaranteed and per-slot drop tables
     * @param costs the already-priced cost rows to subtract
     * @return the expected-value breakdown
     */
    private fun expectedBreakdown(table: TierTable, costs: List<Entry>): Breakdown {
        var guaranteedError = false
        val guaranteed = table.guaranteed.sumOf {
            lootValueOrNull(it.first, it.second.toLong()) ?: run { guaranteedError = true; 0L }
        }
        val (slot1, slot1Error) = expectedSlot(table.slot1)
        val (slot2, slot2Error) = expectedSlot(table.slot2)

        val loot = listOf(
            Entry("Guaranteed", guaranteed, error = guaranteedError),
            Entry("E[Slot 1]", slot1, error = slot1Error),
            Entry("E[Slot 2]", slot2, error = slot2Error),
        )
        val total = loot.sumOf { it.value } - costs.sumOf { it.value }
        return Breakdown(loot, costs, total)
    }

    /**
     * Expected value of one slot roll: the sum of each drop's chance times its coin value.
     *
     * @param drops the possible drops for this slot with their chances
     * @return the expected coin value paired with a flag that is true if any drop had no API price
     */
    private fun expectedSlot(drops: List<Drop>): Pair<Long, Boolean> {
        var sum = 0.0
        var error = false
        for (d in drops) {
            val value = lootValueOrNull(d.name, d.qty.toLong())
            if (value == null) error = true else sum += (d.chancePct / 100.0) * value
        }
        return sum.toLong() to error
    }

    /**
     * Turns one Contents lore line into a priced loot row, appending a tag for salvage or the
     * Lvl 100 essence bonus where it applies.
     *
     * @param line a single Contents lore line, e.g. `Aurora Helmet` or `Crimson Essence x2,500`
     * @return the priced entry, flagged as an error if the item has no API price
     */
    private fun lootEntry(line: String): Entry {
        val qtyMatch = QTY_REGEX.find(line)
        val qty = qtyMatch?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 1L
        val name = (if (qtyMatch != null) line.substring(0, qtyMatch.range.first) else line).trim()

        val value = lootValueOrNull(name, qty)
            ?: return Entry(line, 0L, error = true)
        val tag = when {
            armorSalvage && isSalvageable(name) -> " §8(Salvage ${salvageEssence(name)} Ess)"
            lvl100Kuudra && lootIdFor(name)?.startsWith("ESSENCE_") == true -> " §d(+20%)"
            else -> ""
        }
        return Entry("$line$tag", value)
    }

    /**
     * Coin value of [qty] of the loot named [name]; shared by the actual and expected-value modes.
     * Kuudra gear is valued by the Crimson Essence it salvages into rather than its market price.
     *
     * @param name the loot display name (quantity stripped)
     * @param qty how many of the item to value
     * @return the total coin value, or null when the item has no usable API price
     */
    private fun lootValueOrNull(name: String, qty: Long): Long? {
        if (name in ZERO_VALUE) return 0L

        // Kuudra gear is valued by the Crimson Essence it salvages into, not its market price.
        if (armorSalvage && isSalvageable(name)) {
            val price = crimsonPrice()
            return if (price <= 0) null else qty * salvageEssence(name) * price
        }

        val id = lootIdFor(name) ?: return null
        val unit = PriceUtils.getPrice(id, sellOffer, includeTaxes)
        if (unit <= 0) return null
        var value = qty * unit.toLong()
        // Kuudra collection Lvl 100 grants +20% essence; reflect it on essence drops when enabled.
        if (lvl100Kuudra && id.startsWith("ESSENCE_")) value = value * 12 / 10
        return value
    }

    /** @return the current coin price of a single Crimson Essence. */
    private fun crimsonPrice(): Long = PriceUtils.getPrice("ESSENCE_CRIMSON", sellOffer, includeTaxes).toLong()

    /**
     * Whether an item is valued by its Crimson Essence salvage rather than its market price.
     *
     * @param name the item display name (stars are ignored)
     * @return true for Kuudra armor and the salvageable weapons/accessories
     */
    private fun isSalvageable(name: String): Boolean {
        val clean = name.replace("✪", "").trim()
        return clean in KUUDRA_ARMOR || clean in SALVAGEABLE_EXTRA
    }

    /**
     * Crimson Essence an item salvages into. Non-armor gear uses its fixed value from
     * [SALVAGE_ESSENCE_OVERRIDE]; armor uses 100 base plus a per-star increment that grows with each
     * star (see [ESSENCE_PER_STAR]). The +20% perk applies to both.
     *
     * @param name the item display name; its `✪` count is read as the star level
     * @return the salvage essence amount, including the perk bonus when it is enabled
     */
    private fun salvageEssence(name: String): Long {
        val clean = name.replace("✪", "").trim()
        var essence = SALVAGE_ESSENCE_OVERRIDE[clean]
            ?: run {
                val stars = name.count { it == '✪' }.coerceAtMost(ESSENCE_PER_STAR.size)
                var e = BASE_CRIMSON_ESSENCE.toLong()
                for (i in 0 until stars) e += ESSENCE_PER_STAR[i]
                e
            }
        if (salvagePerk) essence = essence * 12 / 10
        return essence
    }

    /**
     * Prices one Cost lore line. A known key is expanded into its recipe as sub-rows; an unknown key
     * falls back to the coin amount in the line, or 0 if it lists none.
     *
     * @param line a single Cost lore line, e.g. `Infernal Kuudra Key` or `FREE`
     * @return the priced cost entry, with recipe components as its parts where applicable
     */
    private fun costEntry(line: String): Entry {
        if (line.equals("FREE", ignoreCase = true)) return Entry(line, 0L)

        KEY_RECIPES[line]?.let { recipe ->
            val parts = buildList {
                if (recipe.coins > 0) add(Entry("${formatCoins(recipe.coins)} Coins", recipe.coins))
                recipe.components.forEach { (id, qty) ->
                    val value = qty.toLong() * PriceUtils.getPrice(id, sellOffer, includeTaxes).toLong()
                    add(Entry("${qty}x ${prettyName(id)}", value))
                }
            }
            return Entry(line, parts.sumOf { it.value }, parts)
        }

        // Tier keys without a known recipe (Basic/Hot/Burning/Fiery) aren't priced yet, so 0.
        val coins = COINS_REGEX.find(line)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
        return Entry(line, coins ?: 0L)
    }

    /**
     * Resolves a loot display name to its price-API id. Enchanted books and explicit [NAME_TO_ID]
     * overrides take priority, then the generic ` Shard`/` Essence` suffix rules.
     *
     * @param name the loot display name
     * @return the API id, or null if the name maps to no known id
     */
    private fun lootIdFor(name: String): String? {
        ENCHANT_BOOK_REGEX.matchEntire(name)?.let { m ->
            return "${m.groupValues[1].uppercase().replace(' ', '_')}_${m.groupValues[2]}"
        }
        // Explicit overrides win first: some display names (e.g. "Cinderbat Shard") don't follow the
        // generic " Shard"/" Essence" -> ID rule, so the map entry must take priority over the suffix.
        NAME_TO_ID[name]?.let { return it }
        return when {
            name.endsWith(" Shard") -> "SHARD_" + name.removeSuffix(" Shard").uppercase().replace(' ', '_')
            name.endsWith(" Essence") -> "ESSENCE_" + name.removeSuffix(" Essence").uppercase().replace(' ', '_')
            else -> null
        }
    }

    /**
     * Turns a price-API id like `CORRUPTED_NETHER_STAR` into a readable `Corrupted Nether Star`.
     *
     * @param id the upper-snake-case API id
     * @return the human-readable, title-cased name
     */
    private fun prettyName(id: String): String =
        id.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

    /**
     * Tints the best chest's slot and draws its profit as a coin label just below it. The tint is red
     * and the value incomplete when a price is missing.
     *
     * @param ctx draw context
     * @param leftPos left pixel edge of the GUI
     * @param topPos top pixel edge of the GUI
     * @param slot the chest slot to highlight
     * @param profit the profit to render as the label
     * @param hasError true when some content item had no API price
     */
    private fun renderHighlight(
        ctx: GuiGraphicsExtractor,
        leftPos: Int,
        topPos: Int,
        slot: Slot,
        profit: Long,
        hasError: Boolean
    ) {
        val x = leftPos + slot.x
        val y = topPos + slot.y

        // Translucent tint over the chest item — red when the value is incomplete (missing API price).
        ctx.fill(x, y, x + 16, y + 16, if (hasError) ERROR_HIGHLIGHT_COLOR else highlightColor.getRGBA())

        // Coin label centred just below the slot, scaled to fit the 16px cell.
        val color = if (profit >= 0) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
        val text = Component.literal(formatCoins(profit))
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate((x + 8).toFloat(), (y + 18).toFloat())
        pose.scale(0.8f, 0.8f)
        ctx.text(mc.font, text, -mc.font.width(text) / 2, 0, color)
        pose.popMatrix()
    }

    /**
     * Draws the breakdown panel beside the GUI, flipping to the left side if it would clip off-screen
     * (in "Auto" mode) or following the configured side otherwise.
     *
     * @param ctx draw context
     * @param leftPos left pixel edge of the GUI
     * @param topPos top pixel edge of the GUI
     * @param imageWidth width of the GUI, used to place the panel on its right
     * @param screenWidth full screen width, used for the off-screen clip check
     * @param breakdown the priced breakdown to render
     */
    private fun renderBreakdown(
        ctx: GuiGraphicsExtractor,
        leftPos: Int,
        topPos: Int,
        imageWidth: Int,
        screenWidth: Int,
        breakdown: Breakdown
    ) {
        val lines = breakdownLines(breakdown)
        val width = (lines.maxOf { mc.font.width(it) }) + 8
        val lineH = mc.font.lineHeight + 2
        val height = lines.size * lineH + 4

        val rightX = leftPos + imageWidth + 4
        val leftX = leftPos - width - 4
        val x = when (breakdownSide) {
            "Left" -> leftX
            "Right" -> rightX
            else -> if (rightX + width > screenWidth) leftX else rightX // Auto
        }
        val y = topPos

        ctx.fill(x, y, x + width, y + height, 0xD0000000.toInt())
        var ty = y + 3
        for (line in lines) {
            ctx.text(mc.font, line, x + 4, ty, 0xFFFFFFFF.toInt())
            ty += lineH
        }
    }

    /**
     * Formats a breakdown into the colored, ready-to-draw text lines of the panel: a header, the loot
     * rows, the cost rows with their recipe parts, and the profit total.
     *
     * @param breakdown the priced breakdown to format
     * @return the panel's lines as Minecraft [Component]s
     */
    private fun breakdownLines(breakdown: Breakdown): List<Component> {
        val lines = ArrayList<Component>()
        fun add(s: String) = lines.add(Component.literal(s))

        add(if (breakdown.hasApiError) "§c§lProfit Breakdown §4(API error)" else "§6§lProfit Breakdown")
        add("§a§lLoot")
        for (e in breakdown.loot) {
            when {
                e.error -> add("§c${e.label} +${formatCoins(e.value)} §c(API error)")
                e.value > 0 -> add("§7${e.label} §a+${formatCoins(e.value)}")
                else -> add("§8${e.label} §8(no price)")
            }
        }

        add("§c§lCost")
        if (breakdown.costs.isEmpty()) add("§7  none")
        for (e in breakdown.costs) {
            add("§7${e.label} §c-${formatCoins(e.value)}")
            for (p in e.parts) add("§8  ${p.label} §c-${formatCoins(p.value)}")
        }

        val sign = if (breakdown.total >= 0) "§a+" else "§c"
        add("§e§lProfit: $sign${formatCoins(breakdown.total)}")
        return lines
    }

    /**
     * Shortens a coin amount with a k/M/B suffix, e.g. `1500` becomes `1.5k`.
     *
     * @param value the coin amount (may be negative)
     * @return the compact string; amounts below 1,000 are shown in full
     */
    private fun formatCoins(value: Long): String {
        val a = abs(value)
        return when {
            a >= 1_000_000_000 -> "%.1fB".format(value / 1e9)
            a >= 1_000_000 -> "%.1fM".format(value / 1e6)
            a >= 1_000 -> "%.1fk".format(value / 1e3)
            else -> value.toString()
        }
    }

    /**
     * Builds a cache key from the GUI's slot items (name, count and lore) plus the pricing settings,
     * so the best-chest result is only recomputed when the contents or settings actually change.
     *
     * @param screen the chest GUI to fingerprint
     * @return a hash identifying this content-and-settings snapshot
     */
    private fun contentHash(screen: AbstractContainerScreen<*>): Int {
        val slots = screen.menu.slots.joinToString("|") {
            if (!it.hasItem()) return@joinToString "e"
            val stack = it.item
            // Hypixel sends chest items with their name first and fills in the Contents/Cost lore a
            // few ticks later. Hash the lore too, otherwise a name-stable item whose lore just loaded
            // in wouldn't invalidate the cache and the profit would stay stale until the GUI reopens.
            val loreHash = stack.get(DataComponents.LORE)?.styledLines()?.hashCode() ?: 0
            "${stack.hoverName.string}:${stack.count}:$loreHash"
        }
        // Fold in the pricing settings so toggling them re-computes while a GUI stays open.
        return "$slots|$sellOffer|$includeTaxes|$lvl100Kuudra|$armorSalvage|$salvagePerk|$valueMode".hashCode()
    }

    /**
     * Warns in chat once per item which loot the API couldn't price, so the gap is visible in-game.
     *
     * @param missing the display names of items that had no price
     */
    private fun reportMissing(missing: List<String>) {
        for (item in missing) {
            if (reportedErrors.add(item)) {
                modMessage("§cNo price for §f$item §c— profit is incomplete (API).")
            }
        }
    }

    /**
     * Clears the cached best-chest result and the per-item missing-price warnings.
     */
    private fun invalidate() {
        cacheHash = 0
        cacheResult = null
        reportedErrors.clear()
    }
}