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

/**
 * Shows the Croesus chest profit for Kuudra runs, ported from the 1.8.9 ProfitTracker (dungeons).
 *
 * Croesus flow: `/croesus` -> click a Kuudra run -> `Kuudra - <Tier>` overview -> `Paid Chest` /
 * `Free Chest`. The openable chest carries an "Open Reward Chest" item whose lore lists `Contents`
 * and `Cost`. We parse that lore, value the loot via [PriceUtils], subtract the cost, highlight the
 * single most profitable chest, and (optionally) draw a breakdown panel explaining the number.
 */
object ProfitTracker : Module(
    name = "Profit Tracker",
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

    // Hoisted: GuiRenderEvent fires every frame while a menu is open.
    private val QTY_REGEX = Regex("""[x×]\s*([\d,]+)\s*$""")
    private val COINS_REGEX = Regex("""([\d,]+)\s*Coins?""", RegexOption.IGNORE_CASE)
    private val KUUDRA_TIER_TITLE = Regex("""Kuudra - (Basic|Hot|Burning|Fiery|Infernal)""")
    // "Enchanted Book (Mana Vampire V)" -> MANA_VAMPIRE_V (the API keys books as <ENCHANT>_<ROMAN>).
    private val ENCHANT_BOOK_REGEX = Regex("""Enchanted Book \((.+) ([IVXLCDM]+)\)""")

    // Loot whose display name maps to a price-API id by a fixed rule. " Shard" -> SHARD_<NAME> and
    // " Essence" -> ESSENCE_<NAME> are handled generically; only the rest need an explicit entry.
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
        "Hellstorm Staff" to "HELLSTORM_STAFF",
        "Wheel of Fate" to "WHEEL_OF_FATE",
        // Display name differs from the API id ("Cinderbat" vs "CINDER_BAT").
        "Cinderbat Shard" to "SHARD_CINDER_BAT",
        // Display uses British "Spectre"; the bazaar id uses American "SPECTER".
        "Wither Spectre Shard" to "SHARD_WITHER_SPECTER",
    )

    // Items that are intentionally worthless (no meaningful market value) — counted as 0, not an error.
    private val ZERO_VALUE = setOf("Magma Slug Shard", "Dusty Travel Scroll to the Kuudra Skull")

    // Tier keys aren't sold directly, so they're valued by their craft recipe: a flat coin cost plus
    // material components priced live via the API. Recomputed whenever prices update (cache invalidates).
    private data class KeyRecipe(val coins: Long, val components: List<Pair<String, Int>>)

    // Kuudra armor salvage: a piece is worth the Crimson Essence it salvages into — a flat 100 base
    // plus 20 per star (linear), optionally boosted by the +20% salvage perk (see [salvagePerk]).
    private const val BASE_CRIMSON_ESSENCE = 100
    private const val ESSENCE_PER_STAR = 20

    private val KUUDRA_ARMOR: Set<String> = buildSet {
        for (set in listOf("Aurora", "Crimson", "Terror", "Hollow", "Fervor")) {
            for (piece in listOf("Helmet", "Chestplate", "Leggings", "Boots")) add("$set $piece")
        }
    }

    // Non-armor Kuudra gear that is also valued by its Crimson Essence salvage (weapons/accessories).
    private val SALVAGEABLE_EXTRA = setOf(
        "Aurora Staff", "Hollow Wand",
        "Molten Belt", "Molten Bracelet", "Molten Cloak", "Molten Necklace",
    )

    private val KEY_RECIPES = mapOf(
        "Infernal Kuudra Key" to KeyRecipe(
            coins = 2_328_000L,
            components = listOf("ENCHANTED_MYCELIUM" to 80, "CORRUPTED_NETHER_STAR" to 2)
        ),
    )

    // --- Expected-value drop tables (wiki-sourced) -------------------------------------------------
    // A paid chest = guaranteed items + one slot-1 roll + one slot-2 roll. Expected value computes
    // each slot's average independently (Σ chance × value) and sums them with the guaranteed value.
    private data class Drop(val name: String, val qty: Int, val chancePct: Double)

    private data class TierTable(
        val guaranteed: List<Pair<String, Int>>,
        val slot1: List<Drop>,
        val slot2: List<Drop>
    )

    private val KEY_TIER_REGEX = Regex("(Basic|Hot|Burning|Fiery|Infernal) Kuudra Key")

    // Guaranteed amounts taken from a real Infernal paid chest tooltip.
    private val INFERNAL_GUARANTEED = listOf(
        "Crimson Essence" to 2000,
        "Kuudra Teeth" to 3,
        "Kraken Shard" to 1,
    )

    // Slot 1 sums to ~100% (20 armor pieces @ 2.96% + the shards/items below).
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

    // Slot 2 is partial (~35% of mass listed); the unlisted remainder is cheap filler valued at 0.
    private val INFERNAL_SLOT2: List<Drop> = listOf(
        // Paid-chest tentacle drop (~10% per the wiki); valued via KUUDRA_TENTACLE bazaar price.
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
        "Infernal" to TierTable(INFERNAL_GUARANTEED, INFERNAL_SLOT1, INFERNAL_SLOT2),
    )

    /**
     * A single priced row of the breakdown; [parts] are the sub-rows (e.g. a key's recipe).
     * [error] marks rows containing an item that has no price in the API (rendered red).
     */
    private data class Entry(
        val label: String,
        val value: Long,
        val parts: List<Entry> = emptyList(),
        val error: Boolean = false
    )

    private data class Breakdown(val loot: List<Entry>, val costs: List<Entry>, val total: Long) {
        /** True if any priced row references an item with no usable API price. */
        val hasError: Boolean get() = loot.any { it.error } || costs.any { it.error }

        /** Labels of the rows that failed to price — used for the chat warning. */
        val missing: List<String> get() = (loot + costs).filter { it.error }.map { it.label }
    }

    // Cache the computed best chest per GUI content snapshot so we only re-parse when slots change.
    private var cacheHash = 0
    private var cacheResult: Pair<Slot, Breakdown>? = null

    // Translucent red tint for the highlighted chest when its value couldn't be fully priced.
    private val ERROR_HIGHLIGHT = Color(250, 18, 0, 120).rgb

    // Missing items already announced in chat, so we warn once per item instead of every frame.
    // Cleared when prices refresh (so a now-priced item won't keep nagging) and on disable.
    private val reportedErrors = HashSet<String>()

    override fun onDisable() {
        super.onDisable()
        invalidate()
    }

    @EventHandler
    fun onPriceUpdate(event: PriceDataUpdateEvent) = invalidate()

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

        if (breakdown.hasError) reportMissing(breakdown.missing)

        renderHighlight(ctx, accessor.vexoLeftPos(), accessor.vexoTopPos(), slot, breakdown.total, breakdown.hasError)

        if (showBreakdown) {
            renderBreakdown(ctx, accessor.vexoLeftPos(), accessor.vexoTopPos(), accessor.vexoImageWidth(), screen.width, breakdown)
        }
    }

    /** Tints each Croesus run head by how many of its chests are still openable. */
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
     * Best openable-chest profit for [screen], or null if it isn't a Kuudra chest GUI or has no
     * priceable chest. Public so the private AutoProfitOpen module can reuse the pricing logic
     * without depending on this module being enabled.
     */
    fun bestProfitFor(screen: AbstractContainerScreen<*>): Long? {
        val title = screen.title.string.removeFormatting()
        if (title != "Paid Chest" && title != "Free Chest" && !KUUDRA_TIER_TITLE.containsMatchIn(title)) return null
        return bestChest(screen)?.second?.total
    }

    /** Picks the single most profitable openable chest in the current GUI, or null if none. */
    private fun bestChest(screen: AbstractContainerScreen<*>): Pair<Slot, Breakdown>? {
        var best: Pair<Slot, Breakdown>? = null
        for (slot in screen.menu.slots) {
            if (!slot.hasItem()) continue
            val breakdown = chestBreakdown(slot.item) ?: continue
            if (best == null || breakdown.total > best.second.total) best = slot to breakdown
        }
        return best
    }

    /** Parses the `Contents`/`Cost` lore of a chest item into a priced breakdown. */
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

    /** Expected value of one slot roll; the flag is true if any drop has no API price. */
    private fun expectedSlot(drops: List<Drop>): Pair<Long, Boolean> {
        var sum = 0.0
        var error = false
        for (d in drops) {
            val value = lootValueOrNull(d.name, d.qty.toLong())
            if (value == null) error = true else sum += (d.chancePct / 100.0) * value
        }
        return sum.toLong() to error
    }

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
     * Returns null when the item has no usable API price (so callers can flag it as an error).
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

    private fun crimsonPrice(): Long = PriceUtils.getPrice("ESSENCE_CRIMSON", sellOffer, includeTaxes).toLong()

    private fun isSalvageable(name: String): Boolean {
        val clean = name.replace("✪", "").trim()
        return clean in KUUDRA_ARMOR || clean in SALVAGEABLE_EXTRA
    }

    /** Crimson Essence an armor piece salvages into: 100 base + 20 per star, +20% with the perk. */
    private fun salvageEssence(name: String): Long {
        val stars = name.count { it == '✪' }
        var essence = (BASE_CRIMSON_ESSENCE + ESSENCE_PER_STAR * stars).toLong()
        if (salvagePerk) essence = essence * 12 / 10
        return essence
    }

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

    /** Turns a price-API id like `CORRUPTED_NETHER_STAR` into a readable `Corrupted Nether Star`. */
    private fun prettyName(id: String): String =
        id.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

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
        ctx.fill(x, y, x + 16, y + 16, if (hasError) ERROR_HIGHLIGHT else highlightColor.getRGBA())

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

    /** Draws the breakdown panel beside the GUI, flipping to the left side if it would clip off-screen. */
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

    private fun breakdownLines(breakdown: Breakdown): List<Component> {
        val lines = ArrayList<Component>()
        fun add(s: String) = lines.add(Component.literal(s))

        add(if (breakdown.hasError) "§c§lProfit Breakdown §4(API error)" else "§6§lProfit Breakdown")
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

    private fun formatCoins(value: Long): String {
        val a = abs(value)
        return when {
            a >= 1_000_000_000 -> "%.1fB".format(value / 1e9)
            a >= 1_000_000 -> "%.1fM".format(value / 1e6)
            a >= 1_000 -> "%.1fk".format(value / 1e3)
            else -> value.toString()
        }
    }

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

    /** Warns once per missing item which loot the API couldn't price, so the gap is visible in-game. */
    private fun reportMissing(missing: List<String>) {
        for (item in missing) {
            if (reportedErrors.add(item)) {
                modMessage("§cKein Preis für §f$item §c— Profit ist unvollständig (API).")
            }
        }
    }

    private fun invalidate() {
        cacheHash = 0
        cacheResult = null
        reportedErrors.clear()
    }
}