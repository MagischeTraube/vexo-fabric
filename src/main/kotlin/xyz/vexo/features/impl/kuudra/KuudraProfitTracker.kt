package xyz.vexo.features.impl.kuudra

import java.awt.Color
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.GuiRenderEvent
import xyz.vexo.events.impl.PriceDataUpdateEvent
import xyz.vexo.features.Module
import xyz.vexo.mixin.AbstractContainerScreenAccessor
import xyz.vexo.utils.PriceUtils
import xyz.vexo.utils.chestprofit.Breakdown
import xyz.vexo.utils.chestprofit.ChestProfitEngine
import xyz.vexo.utils.chestprofit.Entry
import xyz.vexo.utils.removeFormatting


object KuudraProfitTracker : Module(
    name = "Kuudra Profit Tracker",
    description = "Shows Croesus chest profit for Kuudra runs",
    toggled = false
), ChestProfitEngine.Valuer {
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

    private val engine = ChestProfitEngine(this)

    override fun onDisable() {
        super.onDisable()
        engine.invalidate()
    }

    @EventHandler
    fun onPriceUpdate(event: PriceDataUpdateEvent) = engine.invalidate()

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

        val (slot, breakdown) = engine.cachedBestChest(screen) ?: return
        val ctx = event.context

        if (breakdown.hasApiError) engine.reportMissing(breakdown.missingInfo)

        engine.renderHighlight(
            ctx, accessor.vexoLeftPos(), accessor.vexoTopPos(),
            slot, breakdown.total, breakdown.hasApiError, highlightColor.getRGBA()
        )

        if (showBreakdown) {
            engine.renderBreakdown(
                ctx, accessor.vexoLeftPos(), accessor.vexoTopPos(),
                accessor.vexoImageWidth(), screen.width, breakdown, breakdownSide
            )
        }
    }

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

    fun priceOf(name: String): Long? = lootValueOrNull(name, 1L)

    fun chestProfit(stack: ItemStack): Long? = engine.chestBreakdown(stack)?.total

    fun chestProfitReady(stack: ItemStack): Long? =
        engine.chestBreakdown(stack)?.takeUnless { it.hasApiError }?.total

    fun chestLootNames(stack: ItemStack): List<String> = engine.chestLootNames(stack)

    override fun overrideBreakdown(costLines: List<String>, costs: List<Entry>): Breakdown? {
        if (valueMode != "Expected") return null
        val table = costLines.firstNotNullOfOrNull { KEY_TIER_REGEX.find(it)?.groupValues?.get(1) }
            ?.let { TIER_TABLES[it] }
            ?: return null
        return expectedBreakdown(table, costs)
    }

    override fun settingsFingerprint(): String =
        "$sellOffer|$includeTaxes|$lvl100Kuudra|$armorSalvage|$salvagePerk|$valueMode"

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

    private fun expectedSlot(drops: List<Drop>): Pair<Long, Boolean> {
        var sum = 0.0
        var error = false
        for (d in drops) {
            val value = lootValueOrNull(d.name, d.qty.toLong())
            if (value == null) error = true else sum += (d.chancePct / 100.0) * value
        }
        return sum.toLong() to error
    }

    override fun lootTag(name: String): String = when {
        armorSalvage && isSalvageable(name) -> " §8(Salvage ${salvageEssence(name)} Ess)"
        lvl100Kuudra && lootIdFor(name)?.startsWith("ESSENCE_") == true -> " §d(+20%)"
        else -> ""
    }

    override fun lootValueOrNull(name: String, qty: Long): Long? {
        if (name in ZERO_VALUE) return 0L

        if (armorSalvage && isSalvageable(name)) {
            val price = crimsonPrice()
            return if (price <= 0) null else qty * salvageEssence(name) * price
        }

        val id = lootIdFor(name) ?: return null
        val unit = PriceUtils.getPrice(id, sellOffer, includeTaxes)
        if (unit <= 0) return null
        var value = qty * unit.toLong()
        if (lvl100Kuudra && id.startsWith("ESSENCE_")) value = value * 12 / 10
        return value
    }

    private fun crimsonPrice(): Long = PriceUtils.getPrice("ESSENCE_CRIMSON", sellOffer, includeTaxes).toLong()

    private fun isSalvageable(name: String): Boolean {
        val clean = name.replace("✪", "").trim()
        return clean in KUUDRA_ARMOR || clean in SALVAGEABLE_EXTRA
    }

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

    override fun costEntry(line: String): Entry {
        if (line.equals("FREE", ignoreCase = true)) return Entry(line, 0L)

        KEY_RECIPES[line]?.let { recipe ->
            val parts = buildList {
                if (recipe.coins > 0) add(Entry("${ChestProfitEngine.formatCoins(recipe.coins)} Coins", recipe.coins))
                recipe.components.forEach { (id, qty) ->
                    val value = qty.toLong() * PriceUtils.getPrice(id, sellOffer, includeTaxes).toLong()
                    add(Entry("${qty}x ${prettyName(id)}", value))
                }
            }
            return Entry(line, parts.sumOf { it.value }, parts)
        }

        val coins = COINS_REGEX.find(line)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
        return Entry(line, coins ?: 0L)
    }

    private fun lootIdFor(name: String): String? {
        ENCHANT_BOOK_REGEX.matchEntire(name)?.let { m ->
            return "${m.groupValues[1].uppercase().replace(' ', '_')}_${m.groupValues[2]}"
        }
        NAME_TO_ID[name]?.let { return it }
        return when {
            name.endsWith(" Shard") -> "SHARD_" + name.removeSuffix(" Shard").uppercase().replace(' ', '_')
            name.endsWith(" Essence") -> "ESSENCE_" + name.removeSuffix(" Essence").uppercase().replace(' ', '_')
            else -> null
        }
    }

    private fun prettyName(id: String): String =
        id.split('_').joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }
}
