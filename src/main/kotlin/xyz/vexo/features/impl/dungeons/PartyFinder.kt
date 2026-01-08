package xyz.vexo.features.impl.dungeons

import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import xyz.vexo.Vexo.mc
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

    private val showFairyPerk by BooleanSetting("Show Fairy Perk", "Shows Fairy Perk in the tooltip")
    private val showSecrets by BooleanSetting("Show Secrets", "Shows Secrets in the tooltip")

    private val playerDataCache = ConcurrentHashMap<String, PlayerData.PlayerDataObject>()
    private val fetchingPlayers = ConcurrentHashMap<String, Boolean>()
    private val originalLinesCache = ConcurrentHashMap<String, Component>()
    private val playerUuidCache = ConcurrentHashMap<String, String>()
    private val currentPartyMembers = mutableSetOf<String>()

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
        parsePartyMembers(lines)
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

    /**
     * Parses the floor from the tooltip
     *
     * @param text The text of the tooltip
     * @return The floor
     */
    private fun parseFloor(text: String): Int? = when (text.substringAfter("Floor:").trim()) {
        "Entrance" -> 0
        "Floor I" -> 1
        "Floor II" -> 2
        "Floor III" -> 3
        "Floor IV" -> 4
        "Floor V" -> 5
        "Floor VI" -> 6
        "Floor VII" -> 7
        else -> null
    }

    /**
     * Parses the party members from the tooltip
     *
     * @param lines The lines of the tooltip
     */
    private fun parsePartyMembers(lines: List<Component>) {
        currentPartyMembers.clear()
        var foundMembers = false

        for (line in lines) {
            val text = line.string.removeFormatting().trim()
            if (text == "Members:") {
                foundMembers = true
                continue
            }

            if (foundMembers) {
                if (text.isEmpty() || text.startsWith("Click to join")) break
                if (text.startsWith("Empty")) continue

                val playerName = text.substringBefore(':').trim()
                if (playerName.isNotEmpty()) currentPartyMembers.add(playerName)
            }
        }
    }

    /**
     * Updates the tooltip lines
     *
     * @param lines The lines of the tooltip
     * @param floor The floor of the dungeon
     * @param isMaster Whether the dungeon is in master mode
     */
    private fun updateTooltipLines(lines: MutableList<Component>, floor: Int?, isMaster: Boolean) {
        var foundMembers = false

        for (i in lines.indices) {
            val lineText = lines[i].string.removeFormatting().trim()

            if (lineText == "Members:") {
                foundMembers = true
                continue
            }

            if (!foundMembers) continue
            if (lineText.isEmpty() || lineText.startsWith("Click to join") || lineText.startsWith("Empty")) break

            val playerName = lineText.substringBefore(':').trim()
            if (playerName.isEmpty()) continue

            if (!originalLinesCache.containsKey(playerName)) {
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
        val cachedData = playerDataCache[playerName]

        if (cachedData != null) {
            if (cachedData.isError) {
                return Component.empty()
                    .append(originalLine)
                    .append(Component.literal(" [API DOWN]").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000))))
            }

            val uuidCached = playerUuidCache[playerName]
            if (uuidCached != null) {
                return formatTooltipComponent(originalLine, playerName, cachedData, floor, isMaster)
            }
        }

        if (!fetchingPlayers.containsKey(playerName)) {
            fetchingPlayers[playerName] = true
            PlayerData.getPlayerData(playerName) { data ->
                mc.execute {
                    fetchingPlayers.remove(playerName)
                    data?.let {
                        playerDataCache[playerName] = it
                        if (!it.isError) {
                            playerUuidCache[playerName] = it.uuid
                        }
                    }
                }
            }
        }

        return Component.empty()
            .append(originalLine)
            .append(Component.literal(" [...]").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080))))
    }

    /**
     * Formats the tooltip component
     *
     * @param originalLine The original line of the tooltip
     * @param playerName The name of the player
     * @param data The player data
     * @param floor The floor of the dungeon
     * @param isMaster Whether the dungeon is in master mode
     * @return The formatted tooltip component
     */
    private fun formatTooltipComponent(
        originalLine: Component,
        playerName: String,
        data: PlayerData.PlayerDataObject,
        floor: Int?,
        isMaster: Boolean
    ): Component {
        val base = Component.empty()

        base.append(recolorClassComponent(originalLine))

        base.append(
            Component.literal(" ${getLevelColor(data.catacombsLevel)}C${data.catacombsLevel}")
        )

        val pbText = floor
            ?.let { data.getBestTime(it, isMaster)?.let { formatTime(it) } ?: "NO PB" }
            ?: "ERROR"

        base.append(
            Component.literal(" $pbText")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)))
        )

        if (showSecrets) {
            base.append(
                Component.literal(" ${data.totalSecrets}")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF)))
            )
        }

        if (showFairyPerk && data.hasFairyPerk) {
            val pos = calculateUuidPosition(playerName)
            base.append(
                Component.literal(" §7[§a$pos§7]")
            )
        }

        return base
    }

    /**
     * Calculates the position of a player in the party based on their UUID
     *
     * @param playerName The name of the player to calculate the position for
     * @return The position of the player in the party
     */
    private fun calculateUuidPosition(playerName: String): Int {
        val uuids = currentPartyMembers.mapNotNull { member ->
            val data = playerDataCache[member]
            val uuid = playerUuidCache[member]
            if (data != null && uuid != null && data.hasFairyPerk) member to uuid else null
        }.toMap().toMutableMap()

        mc.user?.name?.let { own ->
            val ownData = playerDataCache[own]
            val ownUuid = playerUuidCache[own]
            if (ownData != null && ownUuid != null && ownData.hasFairyPerk) uuids.putIfAbsent(own, ownUuid)
        }

        val targetUuid = uuids[playerName] ?: return 0
        val normalizedTarget = targetUuid.replace("-", "").lowercase()
        val sorted = uuids.values.map { it.replace("-", "").lowercase() }.sorted()

        return sorted.indexOf(normalizedTarget) + 1
    }

    /**
     * Formats a time in milliseconds to a string in the format "XmYs"
     *
     * @param ms The time in milliseconds
     * @return The formatted time string
     */
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

    /**
     * Recolors the class component
     *
     * @param original The original component
     * @return The recolored component
     */
    private fun recolorClassComponent(original: Component): Component {
        val regex = Regex("(Mage|Archer|Berserk|Healer|Tank) \\((\\d+)\\)")
        val base = Component.empty()

        val fullText = buildString {
            append(original.string)
            original.siblings.forEach { append(it.string) }
        }.removeFormatting()

        val match = regex.find(fullText)

        if (match == null) {
            return original.copy()
        }

        val clazz = match.groupValues[1]
        val level = match.groupValues[2].toIntOrNull() ?: 0

        var foundClass = false
        for (sibling in original.siblings) {
            val text = sibling.string

            if (!foundClass && !text.removeFormatting().contains(Regex("Mage|Archer|Berserk|Healer|Tank"))) {
                base.append(sibling.copy())
            } else if (!foundClass) {
                foundClass = true
                break
            }
        }

        base.append(Component.literal("${getLevelColor(level)}$clazz ($level)"))

        return base
    }

    override fun onEnable() {
        mc.user?.name?.let { own ->
            if (!playerDataCache.containsKey(own)) {
                PlayerData.getPlayerData(own) { data ->
                    mc.execute {
                        data?.let {
                            playerDataCache[own] = it
                            if (!it.isError) {
                                playerUuidCache[own] = it.uuid
                            }
                        }
                    }
                }
            }
        }
        super.onEnable()
    }

    /**
     * Clears all caches
     */
    private fun clearCaches() {
        val ownName = mc.user?.name
        val ownData = ownName?.let { playerDataCache[it] }
        val ownUuid = ownName?.let { playerUuidCache[it] }

        playerDataCache.clear()
        fetchingPlayers.clear()
        originalLinesCache.clear()
        playerUuidCache.clear()
        currentPartyMembers.clear()

        if (ownName != null && ownData != null && ownUuid != null) {
            playerDataCache[ownName] = ownData
            playerUuidCache[ownName] = ownUuid
        }
    }
}