package xyz.vexo.features.impl.dungeons

import java.awt.Color
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.GuiRenderEvent
import xyz.vexo.events.impl.PriceDataUpdateEvent
import xyz.vexo.features.Module
import xyz.vexo.mixin.AbstractContainerScreenAccessor
import xyz.vexo.utils.PriceUtils
import xyz.vexo.utils.chestprofit.ChestProfitEngine
import xyz.vexo.utils.chestprofit.Entry
import xyz.vexo.utils.removeFormatting


object DungeonProfitTracker : Module(
    name = "Dungeon Profit Tracker",
    description = "Shows Croesus chest profit for dungeon runs",
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
    private val showSecondChest by BooleanSetting(
        "Show Second Chest",
        "Also highlights a 2nd, key-payable chest if opening it would still be profitable",
        default = true
    )
    private val secondChestColor = ColorSetting(
        "Second Chest Highlight",
        "Color of the second-chest highlight",
        default = Color(250, 200, 0, 120)
    ).apply { dependsOn { showSecondChest } }


    private val COINS_REGEX = Regex("""([\d,]+)\s*Coins?""", RegexOption.IGNORE_CASE)
    private val ENCHANT_BOOK_REGEX = Regex("""Enchanted Book \((.+) ([IVXLCDM]+)\)""")
    private val KEY_QTY_REGEX = Regex("""[x×]\s*([\d,]+)""")

    private val CATACOMBS_TITLE = Regex("""Catacombs - Floor""")
    private val CHEST_TITLES = setOf("Wood", "Gold", "Diamond", "Emerald", "Obsidian", "Bedrock")

    private val NAME_TO_ID = mapOf(
        "Necron's Handle" to "NECRON_HANDLE",
        "Shiny Necron's Handle" to "SHINY_NECRON_HANDLE",
        "Necron Dye" to "DYE_NECRON",
        "Bonzo's Staff" to "BONZO_STAFF",
        "Bonzo's Mask" to "BONZO_MASK",
        "Scarf's Studies" to "SCARF_STUDIES",
        "Adaptive Blade" to "STONE_BLADE",
        "Shiny Wither Helmet" to "SHINY_WITHER_HELMET",
        "Shiny Wither Chestplate" to "SHINY_WITHER_CHESTPLATE",
        "Shiny Wither Leggings" to "SHINY_WITHER_LEGGINGS",
        "Shiny Wither Boots" to "SHINY_WITHER_BOOTS",
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
        val title = screen.title.string.removeFormatting().trim()

        if (!CATACOMBS_TITLE.containsMatchIn(title) && title.removeSuffix(" Chest") !in CHEST_TITLES) {
            return
        }

        val chests = engine.cachedChests(screen)
        val (slot, breakdown) = chests.firstOrNull() ?: return
        val ctx = event.context
        val accessor = screen as AbstractContainerScreenAccessor

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

        if (showSecondChest) {
            val (slot2, breakdown2) = chests.drop(1).firstOrNull() ?: return
            engine.renderHighlight(
                ctx, accessor.vexoLeftPos(), accessor.vexoTopPos(),
                slot2, breakdown2.total, breakdown2.hasApiError, secondChestColor.getRGBA()
            )
        }
    }

    override fun lootValueOrNull(name: String, qty: Long): Long? {
        val id = lootIdFor(name) ?: return null
        val unit = PriceUtils.getPrice(id, sellOffer, includeTaxes)
        if (unit < 0) return null
        return qty * unit.toLong()
    }

    override fun costEntry(line: String): Entry {
        if (line.equals("FREE", ignoreCase = true)) return Entry(line, 0L)

        if (line.startsWith("Dungeon Chest Key")) {
            val qty = KEY_QTY_REGEX.find(line)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 1L
            val unit = PriceUtils.getPrice("DUNGEON_CHEST_KEY", sellOffer, includeTaxes).toLong()
            return Entry(line, qty * unit, error = unit < 0)
        }

        val coins = COINS_REGEX.find(line)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull()
        return Entry(line, coins ?: 0L)
    }

    override fun settingsFingerprint(): String = "$sellOffer|$includeTaxes"

    private fun lootIdFor(name: String): String? {
        ENCHANT_BOOK_REGEX.matchEntire(name)?.let { m ->
            return "${m.groupValues[1].uppercase().replace(' ', '_')}_${m.groupValues[2]}"
        }
        NAME_TO_ID[name]?.let { return it }
        if (name.endsWith(" Shard")) {
            return "SHARD_" + name.removeSuffix(" Shard").uppercase().replace(' ', '_')
        }
        if (name.endsWith(" Essence")) {
            return "ESSENCE_" + name.removeSuffix(" Essence").uppercase().replace(' ', '_')
        }
        return normalizedIdGuess(name)
    }

    private fun normalizedIdGuess(name: String): String? =
        name.replace("'", "")
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .uppercase()
            .ifEmpty { null }
}
