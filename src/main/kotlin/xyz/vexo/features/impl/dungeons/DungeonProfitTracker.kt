package xyz.vexo.features.impl.dungeons

import java.awt.Color
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.GuiRenderEvent
import xyz.vexo.events.impl.PriceDataUpdateEvent
import xyz.vexo.events.impl.SlotGuiRenderEvent
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

    private val highlightCroesus by BooleanSetting(
        "Highlight Croesus",
        "Tints runs in the Croesus menu by chest status",
        default = true
    )

    private val unopenedColor = ColorSetting(
        "Color: Unopened",
        "Run with 2 chests left to open",
        default = Color(18, 250, 0, 130)
    ).apply { dependsOn { highlightCroesus } }

    private val openableColor = ColorSetting(
        "Color: Openable",
        "Run with 1 chests left to open",
        default = Color(250, 250, 0, 130)
    ).apply { dependsOn { highlightCroesus } }

    private val doneColor = ColorSetting(
        "Color: Done",
        "Run with no chests left",
        default = Color(78, 0, 2, 150)
    ).apply { dependsOn { highlightCroesus } }

    private val KEY_QTY_REGEX = Regex("""[x×]\s*([\d,]+)""")

    private val CATACOMBS_TITLE = Regex("""Catacombs - Floor""")
    private val CHEST_TITLES = setOf("Wood", "Gold", "Diamond", "Emerald", "Obsidian", "Bedrock")

    private val engine = ChestProfitEngine(this)
    private val slotHighlights = mutableMapOf<Slot, Int>()

    override fun onDisable() {
        super.onDisable()
        engine.invalidate()
    }

    @EventHandler
    fun onPriceUpdate(event: PriceDataUpdateEvent) = engine.invalidate()

    @EventHandler
    fun onSlotGuiRender(event: SlotGuiRenderEvent) {
        val color = slotHighlights[event.slot] ?: return
        event.context.fill(
            event.slot.x,
            event.slot.y,
            event.slot.x + 16,
            event.slot.y + 16,
            color
        )
    }

    @EventHandler
    fun onGuiRender(event: GuiRenderEvent) {
        val screen = event.screen as? AbstractContainerScreen<*> ?: return
        val title = screen.title.string.removeFormatting().trim()
        val accessor = screen as AbstractContainerScreenAccessor

        slotHighlights.clear()

        if (title.endsWith("Croesus")) {
            if (highlightCroesus) {
                slotHighlights += engine.croesusHighlights(
                    screen,
                    listOf(
                        "No chests opened yet!" to unopenedColor.getRGBA(),
                        "Chests expire in" to openableColor.getRGBA(),
                        "No more chests to open" to doneColor.getRGBA(),
                    ),
                    nameFilter = "The Catacombs"
                )
            }
            return
        }

        if (!CATACOMBS_TITLE.containsMatchIn(title) && title.removeSuffix(" Chest") !in CHEST_TITLES) {
            return
        }

        slotHighlights += engine.renderChestGui(
            screen, event.context,
            accessor.vexoLeftPos(), accessor.vexoTopPos(), accessor.vexoImageWidth(), screen.width,
            highlightColor.getRGBA(),
            if (showSecondChest) secondChestColor.getRGBA() else null,
            showBreakdown, breakdownSide
        )
    }

    override fun lootValueOrNull(name: String, qty: Long): Long? {
        val id = ChestProfitEngine.lootIdFor(name) ?: return null
        val unit = PriceUtils.getPrice(id, sellOffer, includeTaxes)
        if (unit < 0) return null
        return qty * unit.toLong()
    }

    override fun costEntry(line: String): Entry {
        if (line.startsWith("Dungeon Chest Key")) {
            val qty = KEY_QTY_REGEX.find(line)?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 1L
            val unit = PriceUtils.getPrice("DUNGEON_CHEST_KEY", sellOffer, includeTaxes).toLong()
            return Entry(line, qty * unit, error = unit < 0)
        }
        return ChestProfitEngine.coinEntry(line)
    }

    override fun settingsFingerprint(): String = "$sellOffer|$includeTaxes"
}
