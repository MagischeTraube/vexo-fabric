package xyz.vexo.features.impl.dungeons

import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.inventory.Slot
import xyz.vexo.Vexo
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.SlotGuiRenderEvent
import xyz.vexo.events.impl.TooltipEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.features.Module
import xyz.vexo.utils.PlayerData
import xyz.vexo.utils.DungeonUtils
import xyz.vexo.utils.removeFormatting
import net.minecraft.network.chat.MutableComponent


object DungeonPartyFinder : Module(
    name = "Dungeon Party Finder",
    description = "Adds more information to the party finder",
    toggled = false
) {
    private val showSecrets by BooleanSetting("Show Secrets", "Shows Secrets in the tooltip")
    private val showFairyPerk by BooleanSetting("Show Fairy Perk", "Shows Fairy Perk in the tooltip")

    private val highlightMissingClass by BooleanSetting(
        "Highlight Missing Class",
        "Highlights parties in the Party Finder where your selected class hasn't been taken yet",
        default = true
    )
    private val missingClassColor = ColorSetting(
        "Missing Class Highlight",
        "Color used to highlight parties where your class is still free",
        default = Color(18, 250, 0, 100)
    ).apply { dependsOn { highlightMissingClass } }

    private const val MAX_CACHE_SIZE = 300
    private const val PREFETCH_INTERVAL_TICKS = 10

    private val originalLinesCache = ConcurrentHashMap<String, Component>()

    private val playerDataComponentCache = ConcurrentHashMap<PlayerDataCacheKey, PlayerDataComponent>()

    private val slotHighlights = mutableMapOf<Slot, Int>()

    private var tickCounter = 0
    private var lastScreen: Screen? = null

    private data class PlayerDataCacheKey(
        val playerName: String,
        val floor: Int?,
        val isMaster: Boolean
    )

    private data class PlayerDataComponent(
        val levelText: Component,
        val pbText: Component,
        val secretsCount: Int,
        val hasFairy: Boolean
    )

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        clearCaches()
        lastScreen = null
        slotHighlights.clear()
    }

    /**
     * Prefetches player data for everyone listed in any dungeon party finder
     * lore, so it's already cached by the time the player hovers a tooltip.
     * Also figures out, per party, which classes are already taken so parties
     * missing the player's own class can be highlighted.
     */
    @EventHandler
    fun onTick(event: ClientTickEvent) {
        val screen = Vexo.mc.screen
        if (screen !is AbstractContainerScreen<*> || screen.title.string.removeFormatting() != "Party Finder") {
            lastScreen = null
            slotHighlights.clear()
            return
        }

        val justOpened = screen !== lastScreen
        if (!justOpened && ++tickCounter % PREFETCH_INTERVAL_TICKS != 0) return
        lastScreen = screen

        slotHighlights.clear()
        val myClass = DungeonUtils.selectedClass

        val queued = HashSet<String>()
        for (slot in screen.menu.slots) {
            if (!slot.hasItem()) continue
            val lore = slot.item.get(DataComponents.LORE)?.styledLines()
                ?.map { it.string.removeFormatting().trim() }
                ?: continue

            for (name in dungeonPartyMembers(lore)) if (queued.add(name)) PlayerData.fetchAndCachePlayerData(name)

            if (highlightMissingClass && myClass != null && isDungeonParty(lore)) {
                val itemName = slot.item.hoverName.string.removeFormatting().trim()
                if (isPartyItem(itemName)) {
                    val classesPresent = partyMemberClasses(lore)
                    if (myClass !in classesPresent) {
                        slotHighlights[slot] = missingClassColor.getRGBA()
                    }
                }
            }
        }
    }

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

    private const val PARTY_NAME_SUFFIX = "'s Party"

    private fun isPartyItem(itemName: String): Boolean = itemName.endsWith(PARTY_NAME_SUFFIX)

    private fun isDungeonParty(lore: List<String>): Boolean {
        if (lore.none { it.startsWith("Dungeon:") }) return false
        if (lore.any { it.startsWith("Tier:") }) return false
        return true
    }

    private fun dungeonPartyMembers(lore: List<String>): List<String> {
        if (!isDungeonParty(lore)) return emptyList()

        val membersIndex = lore.indexOfFirst { it == "Members:" }
        if (membersIndex == -1) return emptyList()

        val names = ArrayList<String>()
        for (i in (membersIndex + 1) until lore.size) {
            val text = lore[i]
            if (text.isEmpty() || text.startsWith("Click to join") || text.startsWith("Empty")) break

            val name = text.substringBefore(':').trim()
            if (name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' }) names.add(name)
        }
        return names
    }

    private val MEMBER_CLASS_REGEX = Regex(""":\s*(Mage|Archer|Berserk(?:er)?|Healer|Tank)\s*\(\d+\)""")

    private fun partyMemberClasses(lore: List<String>): Set<DungeonUtils.DungeonClass> {
        if (!isDungeonParty(lore)) return emptySet()

        val membersIndex = lore.indexOfFirst { it == "Members:" }
        if (membersIndex == -1) return emptySet()

        val classes = mutableSetOf<DungeonUtils.DungeonClass>()
        for (i in (membersIndex + 1) until lore.size) {
            val text = lore[i]
            if (text.isEmpty() || text.startsWith("Click to join") || text.startsWith("Empty")) break

            MEMBER_CLASS_REGEX.find(text)?.groupValues?.get(1)?.let { clazzName ->
                DungeonUtils.DungeonClass.fromDisplayName(clazzName)?.let { classes.add(it) }
            }
        }
        return classes
    }

    @EventHandler
    fun onTooltip(event: TooltipEvent) {
        val screen = event.screen
        if (screen !is AbstractContainerScreen<*>) return

        val title = screen.title.string.removeFormatting()
        if (title != "Party Finder") return

        val lines = event.lines
        if (lines.isEmpty()) return

        val firstLine = lines[0].string.removeFormatting()
        if (!firstLine.endsWith("Party")) return

        if (lines.any { it.string.removeFormatting().trim().startsWith("Tier:") }) return
        val dungeonInfo = parseDungeonInfo(lines)
        updateTooltipLines(lines, dungeonInfo.floor, dungeonInfo.isMaster)
    }

    private data class DungeonInfo(val floor: Int?, val isMaster: Boolean)

    private fun parseDungeonInfo(lines: List<Component>): DungeonInfo {
        var floor: Int? = null
        var isMaster = false

        for (line in lines) {
            val text = line.string.removeFormatting()
            when {
                text.startsWith("Dungeon:") -> isMaster = text.contains("Master Mode")
                text.startsWith("Floor:") -> floor = parseFloor(text)
            }
        }

        return DungeonInfo(floor, isMaster)
    }

    private val FLOOR_MAP = mapOf(
        "Entrance" to 0,
        "Floor I" to 1,
        "Floor II" to 2,
        "Floor III" to 3,
        "Floor IV" to 4,
        "Floor V" to 5,
        "Floor VI" to 6,
        "Floor VII" to 7
    )

    private fun parseFloor(text: String): Int? =
        FLOOR_MAP[text.substringAfter("Floor:").trim()]

    private fun updateTooltipLines(
        lines: MutableList<Component>,
        floor: Int?,
        isMaster: Boolean
    ) {
        val membersIndex = lines.indexOfFirst {
            it.string.removeFormatting().trim() == "Members:"
        }

        if (membersIndex == -1) return

        for (i in (membersIndex + 1) until lines.size) {
            val lineText = lines[i].string.removeFormatting().trim()

            if (lineText.isEmpty() || lineText.startsWith("Click to join") || lineText.startsWith("Empty")) {
                break
            }

            val playerName = lineText.substringBefore(':').trim()
            if (playerName.isEmpty()) continue

            if (!originalLinesCache.containsKey(playerName)) {
                if (originalLinesCache.size >= MAX_CACHE_SIZE) {
                    originalLinesCache.clear()
                }
                originalLinesCache[playerName] = lines[i]
            }

            val originalLine = originalLinesCache[playerName]!!

            lines[i] = buildLineComponent(originalLine, playerName, floor, isMaster)
        }
    }

    /**
     * Builds the tooltip component
     *
     * @param originalLine The original line of the tooltip
     * @param playerName The name of the player
     * @param floor The floor of the dungeon
     * @param isMaster Whether the dungeon is in master mode
     * @return The tooltip component
     */
    private fun buildLineComponent(
        originalLine: Component,
        playerName: String,
        floor: Int?,
        isMaster: Boolean
    ): Component {
        val uuid = PlayerData.uuidCache[playerName.lowercase()]?.uuid
        val cachedData = uuid?.let { PlayerData.playerCache[it]?.data }

        if (cachedData != null) {
            if (cachedData.isError) {
                return Component.empty()
                    .append(originalLine)
                    .append(Component.literal(" [API DOWN]")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000))))
            }

            return formatTooltipComponent(originalLine, cachedData, floor, isMaster)
        }

        PlayerData.fetchAndCachePlayerData(playerName)

        return Component.empty()
            .append(originalLine)
            .append(Component.literal(" [...]")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080))))
    }

    /**
     * Formats the tooltip component
     *
     * @param originalLine The original line of the tooltip
     * @param data The player data
     * @param floor The floor of the dungeon
     * @param isMaster Whether the dungeon is in master mode
     * @return The formatted tooltip component
     */
    private fun formatTooltipComponent(
        originalLine: Component,
        data: PlayerData.PlayerDataObject,
        floor: Int?,
        isMaster: Boolean
    ): Component {
        val cacheKey = PlayerDataCacheKey(data.ign, floor, isMaster)

        val playerDataComp = playerDataComponentCache.computeIfAbsent(cacheKey) {
            if (playerDataComponentCache.size >= MAX_CACHE_SIZE) {
                playerDataComponentCache.clear()
            }

            val pbText = floor
                ?.let { data.getBestTime(it, isMaster)?.let { formatTime(it) } ?: "NO PB" }
                ?: "ERROR"

            PlayerDataComponent(
                levelText = Component.literal(" ${getLevelColor(data.catacombsLevel)}C${data.catacombsLevel}"),
                pbText = Component.literal(" $pbText")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55))),
                secretsCount = data.totalSecrets,
                hasFairy = data.hasFairyPerk
            )
        }

        val base = Component.empty()
        base.append(recolorClassComponent(originalLine))
        base.append(playerDataComp.levelText)
        base.append(playerDataComp.pbText)

        if (showSecrets) {
            base.append(
                Component.literal(" ${playerDataComp.secretsCount}")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF)))
            )
        }

        if (showFairyPerk && playerDataComp.hasFairy) {
            base.append(
                Component.literal(" [")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xAAAAAA)))
                    .append(Component.literal("❤")
                        .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55FF55))))
                    .append(Component.literal("]"))
            )
        }

        return base
    }

    private fun formatTime(ms: Int): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return "%dm%02ds".format(min, sec)
    }

    fun getLevelColor(level: Int): String = when {
        level >= 50 -> "§c§l"
        level >= 45 -> "§c"
        level >= 40 -> "§6"
        level >= 35 -> "§d"
        level >= 30 -> "§9"
        level >= 25 -> "§b"
        level >= 20 -> "§2"
        level >= 15 -> "§a"
        level >= 10 -> "§e"
        level >= 5 -> "§f"
        else -> "§7"
    }

    private val CLASS_REGEX = Regex("(Mage|Archer|Berserk|Healer|Tank) \\((\\d+)\\)")

    private fun recolorClassComponent(original: Component): Component {
        val text = original.string.removeFormatting()
        val match = CLASS_REGEX.find(text) ?: return original.copy()

        val clazz = match.groupValues[1]
        val level = match.groupValues[2].toIntOrNull() ?: 0

        val base: MutableComponent = buildPrefixComponent(original, match.range.first)
        base.append(
            Component.literal("${getLevelColor(level)}$clazz ($level)")
        )

        return base
    }

    private fun buildPrefixComponent(original: Component, matchStart: Int): MutableComponent {
        val base: MutableComponent = Component.empty()
        var consumed = 0

        original.visit<Unit>({ style, rawText ->
            val segmentText = rawText.removeFormatting()

            if (consumed < matchStart) {
                val remaining = matchStart - consumed
                val part = if (segmentText.length <= remaining) segmentText else segmentText.substring(0, remaining)
                if (part.isNotEmpty()) {
                    base.append(Component.literal(part).withStyle(style))
                }
            }

            consumed += segmentText.length
            java.util.Optional.empty()
        }, Style.EMPTY)

        return base
    }

    private fun firstColor(component: Component): TextColor? {
        component.style.color?.let { return it }
        for (sibling in component.siblings) firstColor(sibling)?.let { return it }
        return null
    }

    private fun clearCaches() {
        originalLinesCache.clear()
        playerDataComponentCache.clear()
    }
}