package xyz.vexo.utils.chestprofit

import kotlin.math.abs
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import xyz.vexo.utils.LabelPosition
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.renderSidePanel
import xyz.vexo.utils.renderSlotLabel

/**
 * A single priced row of the profit GUI; [parts] are the sub-rows (e.g. a key's recipe).
 * [error] marks rows containing an item that has no price in the API (rendered red).
 */
data class Entry(
    val label: String,
    val value: Long,
    val parts: List<Entry> = emptyList(),
    val error: Boolean = false
)

/**
 * A priced breakdown of a chest's loot and costs, including the net profit total.
 *
 * @param loot the priced loot rows
 * @param costs the priced cost rows
 * @param total the net profit (loot minus costs)
 */
data class Breakdown(val loot: List<Entry>, val costs: List<Entry>, val total: Long) {
    val hasApiError: Boolean get() = loot.any { it.error } || costs.any { it.error }

    val missingInfo: List<String> get() = (loot + costs).filter { it.error }.map { it.label }
}

/**
 * Shared engine for reward-chest profit trackers: parses the `Contents`/`Cost` lore of chest
 * items, prices the rows through the owning module's [Valuer], caches the best chest per GUI
 * content snapshot, and renders the slot highlight and the breakdown panel. Everything
 * game-specific (name-to-id mapping, key recipes, bonus rules) lives in the [Valuer].
 */
class ChestProfitEngine(private val valuer: Valuer) {

    interface Valuer {
        /** Total coin value of [qty] of the loot named [name], or null when it has no usable price. */
        fun lootValueOrNull(name: String, qty: Long): Long?

        /** Suffix appended to a loot row's label (e.g. a salvage note); empty for none. */
        fun lootTag(name: String): String = ""

        /** Prices one Cost lore line. */
        fun costEntry(line: String): Entry

        /** Replaces the actual-contents breakdown (e.g. an expected-value mode); null keeps the actual one. */
        fun overrideBreakdown(costLines: List<String>, costs: List<Entry>): Breakdown? = null

        /** Pricing-affecting settings, folded into the cache key so changing them re-computes. */
        fun settingsFingerprint(): String
    }

    companion object {
        private val QTY_REGEX = Regex("""[x×]\s*([\d,]+)\s*$""")

        val ERROR_HIGHLIGHT_COLOR = java.awt.Color(250, 18, 0, 120).rgb

        /**
         * Shortens a coin amount with a k/M/B suffix, e.g. `1500` becomes `1.5k`.
         *
         * @param value the coin amount (may be negative)
         * @return the compact string; amounts below 1,000 are shown in full
         */
        fun formatCoins(value: Long): String {
            val a = abs(value)
            return when {
                a >= 1_000_000_000 -> "%.1fB".format(value / 1e9)
                a >= 1_000_000 -> "%.1fM".format(value / 1e6)
                a >= 1_000 -> "%.1fk".format(value / 1e3)
                else -> value.toString()
            }
        }
    }

    private var cacheHash = 0
    private var cacheResult: List<Pair<Slot, Breakdown>> = emptyList()

    private var croesusCacheHash = 0
    private var croesusCacheResult: Map<Slot, Int> = emptyMap()

    private val reportedErrors = HashSet<String>()

    /**
     * Picks the single most profitable chest in the current GUI, re-parsing only when the GUI's
     * contents or the pricing settings changed since the last call.
     *
     * @param screen the chest GUI whose slots are scanned
     * @return the winning slot paired with its breakdown, or null if no slot holds a priceable chest
     */
    fun cachedBestChest(screen: AbstractContainerScreen<*>): Pair<Slot, Breakdown>? =
        cachedChests(screen).firstOrNull()

    /**
     * All priceable chests in the current GUI, ranked from most to least profitable, re-parsing only
     * when the GUI's contents or the pricing settings changed since the last call. Lets callers look
     * past the top pick — e.g. to also surface a second, key-payable chest worth opening.
     *
     * @param screen the chest GUI whose slots are scanned
     * @return the priceable slots paired with their breakdown, sorted by descending profit
     */
    fun cachedChests(screen: AbstractContainerScreen<*>): List<Pair<Slot, Breakdown>> {
        if (!screen.menu.carried.isEmpty) return cacheResult
        val hash = contentHash(screen)
        if (hash != cacheHash) {
            cacheHash = hash
            cacheResult = rankedChests(screen)
        }
        return cacheResult
    }

    /**
     * Clears the cached chest ranking and the per-item missing-price warnings.
     */
    fun invalidate() {
        cacheHash = 0
        cacheResult = emptyList()
        croesusCacheHash = 0
        croesusCacheResult = emptyMap()
        reportedErrors.clear()
    }

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
     * Returns a map of reward slots to highlight colors for Croesus, re-scanning only when slot
     * contents change. Each slot's lore is matched against the provided keyword rules to assign
     * a color.
     *
     * @param screen the Croesus GUI whose slots are scanned
     * @param rules pairs of lore keyword and the RGBA color to assign when matched
     * @return a map of slots to their highlight colors, empty when no slot matches any rule
     */
    fun croesusHighlights(screen: AbstractContainerScreen<*>, rules: List<Pair<String, Int>>): Map<Slot, Int> {
        if (!screen.menu.carried.isEmpty) return croesusCacheResult
        val hash = croesusHash(screen, rules)
        if (hash != croesusCacheHash) {
            croesusCacheHash = hash
            croesusCacheResult = scanCroesus(screen, rules)
        }
        return croesusCacheResult
    }

    /**
     * Scans every reward slot in the given GUI and assigns a highlight color based on lore keywords.
     *
     * @param screen the GUI whose slots are scanned
     * @param rules pairs of lore keyword and the RGBA color to assign when matched
     * @return a map of matched slots to their highlight colors
     */
    private fun scanCroesus(screen: AbstractContainerScreen<*>, rules: List<Pair<String, Int>>): Map<Slot, Int> {
        val result = mutableMapOf<Slot, Int>()
        for (slot in screen.rewardSlots()) {
            if (!slot.hasItem()) continue
            val lore = slot.item.get(DataComponents.LORE)?.styledLines()
                ?.map { it.string.removeFormatting() } ?: continue
            for ((keyword, color) in rules) {
                if (lore.any { keyword in it }) {
                    result[slot] = color
                    break
                }
            }
        }
        return result
    }

    /**
     * Returns only the reward slots of the GUI, excluding the player's inventory and hotbar.
     */
    private fun AbstractContainerScreen<*>.rewardSlots(): List<Slot> =
        menu.slots.filterNot { it.container is Inventory }

    /**
     * Ranks every priceable chest in the current GUI by total profit, most profitable first.
     * Skips empty slots and items whose lore cannot be parsed into a breakdown.
     *
     * @param screen the chest GUI to rank
     * @return the slots paired with their breakdown, sorted by descending profit
     */
    private fun rankedChests(screen: AbstractContainerScreen<*>): List<Pair<Slot, Breakdown>> =
        screen.rewardSlots()
            .filter { it.hasItem() }
            .mapNotNull { slot -> chestBreakdown(slot.item)?.let { slot to it } }
            .sortedByDescending { it.second.total }

    /**
     * Parses the `Contents`/`Cost` lore of a chest item into a priced breakdown. The valuer may
     * replace the actual contents via [Valuer.overrideBreakdown] (e.g. an expected-value mode).
     *
     * @param stack the chest item to parse
     * @return the priced breakdown, or null if the item has no `Contents` lore to value
     */
    fun chestBreakdown(stack: ItemStack): Breakdown? {
        val lore = stack.get(DataComponents.LORE)?.styledLines()
            ?.map { it.string.removeFormatting().trim() }
            ?: return null

        if (lore.contains("Already opened!")) return null

        val contentsIdx = lore.indexOf("Contents")
        if (contentsIdx == -1) return null

        val lootLines = lore.drop(contentsIdx + 1).takeWhile { it.isNotBlank() && it != "Cost" }
        if (lootLines.isEmpty()) return null

        val costIdx = lore.indexOf("Cost")
        val costLines = if (costIdx >= 0) lore.drop(costIdx + 1).takeWhile { it.isNotBlank() } else emptyList()
        val costs = costLines.map { valuer.costEntry(it) }

        valuer.overrideBreakdown(costLines, costs)?.let { return it }

        val loot = lootLines.map { lootEntry(it) }
        val total = loot.sumOf { it.value } - costs.sumOf { it.value }
        return Breakdown(loot, costs, total)
    }

    /**
     * Turns one Contents lore line into a priced loot row, appending the valuer's tag where one
     * applies.
     *
     * @param line a single Contents lore line, e.g. `Aurora Helmet` or `Crimson Essence x2,500`
     * @return the priced entry, flagged as an error if the item has no API price
     */
    private fun lootEntry(line: String): Entry {
        val qtyMatch = QTY_REGEX.find(line)
        val qty = qtyMatch?.groupValues?.get(1)?.replace(",", "")?.toLongOrNull() ?: 1L
        val name = (if (qtyMatch != null) line.substring(0, qtyMatch.range.first) else line).trim()
        val value = valuer.lootValueOrNull(name, qty) ?: return Entry(line, 0L, error = true)
        return Entry("$line${valuer.lootTag(name)}", value)
    }

    /**
     * Draws a profit label anchored to a slot using the given [position]. Green for positive
     * profit, red for negative. Delegates to [renderSlotLabel].
     *
     * @param ctx draw context
     * @param leftPos left pixel edge of the GUI
     * @param topPos top pixel edge of the GUI
     * @param slot the chest slot to label
     * @param profit the profit to display
     * @param position where to place the label relative to the slot
     */
    fun renderProfitLabel(
        ctx: GuiGraphicsExtractor,
        leftPos: Int,
        topPos: Int,
        slot: Slot,
        profit: Long,
        position: LabelPosition
    ) {
        val color = if (profit >= 0) 0xFF55FF55.toInt() else 0xFFFF5555.toInt()
        renderSlotLabel(ctx, leftPos, topPos, slot, formatCoins(profit), position, color)
    }

    /**
     * Draws the breakdown panel beside the GUI using the shared [renderSidePanel] helper.
     *
     * @param ctx draw context
     * @param leftPos left pixel edge of the GUI
     * @param topPos top pixel edge of the GUI
     * @param imageWidth width of the GUI, used to place the panel on its right
     * @param screenWidth full screen width, used for the off-screen clip check
     * @param breakdown the priced breakdown to render
     * @param side "Right", "Left" or "Auto"
     */
    fun renderBreakdown(
        ctx: GuiGraphicsExtractor,
        leftPos: Int,
        topPos: Int,
        imageWidth: Int,
        screenWidth: Int,
        breakdown: Breakdown,
        side: String
    ) {
        renderSidePanel(ctx, leftPos, topPos, imageWidth, screenWidth, breakdownLines(breakdown), side)
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
     * Builds a cache key from the GUI's slot items (name, count and lore) plus the valuer's pricing
     * settings, so the best-chest result is only recomputed when either actually changes.
     *
     * @param screen the chest GUI to fingerprint
     * @return a hash identifying this content-and-settings snapshot
     */
    private fun contentHash(screen: AbstractContainerScreen<*>): Int {
        val slots = screen.rewardSlots().joinToString("|") {
            if (!it.hasItem()) return@joinToString "e"
            val stack = it.item
            val loreHash = stack.get(DataComponents.LORE)?.styledLines()?.hashCode() ?: 0
            "${stack.hoverName.string}:${stack.count}:$loreHash"
        }
        return "$slots|${valuer.settingsFingerprint()}".hashCode()
    }

    /**
     * Builds a cache key from the GUI's reward slot items (name and lore) and the highlight rules,
     * so the Croesus slot colors are only recomputed when contents or rules actually change.
     *
     * @param screen the GUI to fingerprint
     * @param rules the highlight keyword rules included in the hash
     * @return a hash identifying this content-and-rules snapshot
     */
    private fun croesusHash(screen: AbstractContainerScreen<*>, rules: List<Pair<String, Int>>): Int {
        val slots = screen.rewardSlots().joinToString("|") {
            if (!it.hasItem()) return@joinToString "e"
            val stack = it.item
            val loreHash = stack.get(DataComponents.LORE)?.styledLines()?.hashCode() ?: 0
            "${stack.hoverName.string}:$loreHash"
        }
        return "$slots|${rules.hashCode()}".hashCode()
    }

    /**
     * Warns in chat once per item which loot the API couldn't price, so the gap is visible in-game.
     *
     * @param missing the display names of items that had no price
     */
    fun reportMissing(missing: List<String>) {
        for (item in missing) {
            if (reportedErrors.add(item)) {
                modMessage("§cNo price for §f$item §c— profit is incomplete (API).")
            }
        }
    }
}