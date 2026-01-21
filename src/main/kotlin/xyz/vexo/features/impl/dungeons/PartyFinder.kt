package xyz.vexo.features.impl.dungeons

import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.TooltipEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.features.Module
import xyz.vexo.utils.PlayerData
import xyz.vexo.utils.removeFormatting


object PartyFinder : Module(
    name = "Party Finder",
    description = "Adds more information to the party finder",
    toggled = false
) {
    private val showSecrets by BooleanSetting("Show Secrets", "Shows Secrets in the tooltip")
    private val showFairyPerk by BooleanSetting("Show Fairy Perk", "Shows Fairy Perk in the tooltip")

    private const val MAX_CACHE_SIZE = 300

    private val originalLinesCache = ConcurrentHashMap<String, Component>()

    private val playerDataComponentCache = ConcurrentHashMap<PlayerDataCacheKey, PlayerDataComponent>()

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

        val dungeonInfo = parseDungeonInfo(lines)
        updateTooltipLines(lines, dungeonInfo.floor, dungeonInfo.isMaster)
    }

    private data class DungeonInfo(val floor: Int?, val isMaster: Boolean)

    /**
     * Parses the dungeon info from the tooltip
     *
     * @param lines The lines of the tooltip
     * @return The dungeon info
     */
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

    /**
     * Parses the floor from the tooltip
     *
     * @param text The text of the tooltip
     * @return The floor
     */
    private fun parseFloor(text: String): Int? =
        FLOOR_MAP[text.substringAfter("Floor:").trim()]

    /**
     * Updates the tooltip lines
     *
     * @param lines The lines of the tooltip
     * @param floor The floor of the dungeon
     * @param isMaster Whether the dungeon is in master mode
     */
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

    /**
     * Gets the color for a given level
     *
     * @param level The level to get the color for
     * @return The color for the given level
     */
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

    /**
     * Recolors the class component
     *
     * @param original The original component
     * @return The recolored component
     */
    private fun recolorClassComponent(original: Component): Component {
        val text = original.string.removeFormatting()
        val match = CLASS_REGEX.find(text)

        if (match == null) return original.copy()

        val clazz = match.groupValues[1]
        val level = match.groupValues[2].toIntOrNull() ?: 0

        val base = Component.empty()

        if (original.siblings.size >= 3) {
            base.append(original.siblings[0].copy())
            base.append(original.siblings[1].copy())
            base.append(original.siblings[2].copy())
        }

        base.append(
            Component.literal("${getLevelColor(level)}$clazz ($level)")
        )

        return base
    }

    /**
     * Clears all caches
     */
    private fun clearCaches() {
        originalLinesCache.clear()
        playerDataComponentCache.clear()
    }
}
