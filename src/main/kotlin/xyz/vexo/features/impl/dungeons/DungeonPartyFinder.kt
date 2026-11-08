package xyz.vexo.features.impl.dungeons

import java.awt.Color
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import net.minecraft.world.inventory.Slot
import xyz.vexo.Vexo
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.SlotGuiRenderEvent
import xyz.vexo.events.impl.TooltipEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.DungeonUtils
import xyz.vexo.utils.UuidResolver
import xyz.vexo.utils.api.DungeonProfile
import xyz.vexo.utils.api.DungeonsApi
import xyz.vexo.utils.logError
import xyz.vexo.utils.removeFormatting

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
    private const val PROFILE_TTL_MS = 60 * 60 * 1000L
    private const val FAILURE_RETRY_MS = 30 * 1000L
    private const val MAX_PARALLEL_FETCHES = 10

    /**
     * Cache entry for one username. A null [profile] marks a failed lookup
     * (name not resolvable or API unreachable), which is retried after [FAILURE_RETRY_MS].
     */
    private class ProfileEntry(
        val profile: DungeonProfile?,
        val fetchedAt: Long,
        @Volatile var lastAccessed: Long
    ) {
        fun isStale(now: Long): Boolean =
            now - fetchedAt >= if (profile == null) FAILURE_RETRY_MS else PROFILE_TTL_MS
    }

    /** Lowercase username -> profile entry. */
    private val profiles = ConcurrentHashMap<String, ProfileEntry>()

    /** Lowercase usernames that are currently part of a running batch request. */
    private val pending: MutableSet<String> = ConcurrentHashMap.newKeySet()

    private val originalLinesCache = ConcurrentHashMap<String, Component>()
    private val slotHighlights = mutableMapOf<Slot, Int>()

    private var tickCounter = 0
    private var lastScreen: Screen? = null

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        originalLinesCache.clear()
        lastScreen = null
        slotHighlights.clear()
    }

    /**
     * Scans the whole party finder GUI, collects every player listed in any
     * dungeon party and requests all missing profiles in one batch, so the data
     * is already cached by the time the player hovers a tooltip.
     * Also figures out, per party, which classes are already taken so parties
     * missing the player's own class can be highlighted.
     */
    @EventHandler
    fun onTick(event: ClientTickEvent) {
        val screen = Vexo.mc.gui.screen()
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

        val names = LinkedHashSet<String>()
        for (slot in screen.menu.slots) {
            if (!slot.hasItem()) continue
            val lore = slot.item.get(DataComponents.LORE)?.styledLines()
                ?.map { it.string.removeFormatting().trim() }
                ?: continue

            names.addAll(dungeonPartyMembers(lore))

            if (highlightMissingClass && myClass != null && isDungeonParty(lore)) {
                val itemName = slot.item.hoverName.string.removeFormatting().trim()
                if (isPartyItem(itemName) && myClass !in partyMemberClasses(lore)) {
                    slotHighlights[slot] = missingClassColor.getRGBA()
                }
            }
        }

        requestMissing(names)
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

    /**
     * Requests all players that are neither cached (and fresh) nor already being
     * loaded, as a single batch.
     *
     * @param names The usernames collected from the GUI
     */
    private fun requestMissing(names: Collection<String>) {
        val now = System.currentTimeMillis()
        val batch = names
            .map { it.lowercase() }
            .distinct()
            .filter { key -> profiles[key]?.isStale(now) != false }
            .filter { pending.add(it) }

        if (batch.isEmpty()) return

        Vexo.scope.launch {
            try {
                val uuids = UuidResolver.resolveAll(batch)
                val limiter = Semaphore(MAX_PARALLEL_FETCHES)

                coroutineScope {
                    batch.map { key ->
                        async(Dispatchers.IO) {
                            val profile = uuids[key]?.let { uuid ->
                                limiter.withPermit { DungeonsApi.fetch(uuid) }
                            }
                            store(key, profile)
                        }
                    }.awaitAll()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logError(e, this@DungeonPartyFinder)
                val failedAt = System.currentTimeMillis()
                for (key in batch) profiles.putIfAbsent(key, ProfileEntry(null, failedAt, failedAt))
            } finally {
                pending.removeAll(batch.toSet())
            }
        }
    }

    /**
     * Stores a lookup result in the cache.
     *
     * If the cache reaches its maximum capacity, the least recently accessed
     * entries are evicted before storing the new entry.
     *
     * @param key The lowercase Minecraft username
     * @param profile The loaded profile, or null if the lookup failed
     */
    private fun store(key: String, profile: DungeonProfile?) {
        if (profiles.size >= MAX_CACHE_SIZE) evictLeastRecentlyUsed()
        val now = System.currentTimeMillis()
        profiles[key] = ProfileEntry(profile, now, now)
    }

    /**
     * Removes the 25% least recently accessed cache entries.
     */
    private fun evictLeastRecentlyUsed() {
        val sorted = profiles.entries.sortedBy { it.value.lastAccessed }
        sorted.take((sorted.size / 4).coerceAtLeast(1))
            .forEach { profiles.remove(it.key, it.value) }
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
     * Builds the tooltip component for a single party member.
     * Never triggers network requests itself; loading is handled by [onTick].
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
        val entry = profiles[playerName.lowercase()]
            ?: return Component.empty()
                .append(originalLine)
                .append(Component.literal(" [...]")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x808080))))

        entry.lastAccessed = System.currentTimeMillis()

        val profile = entry.profile
            ?: return Component.empty()
                .append(originalLine)
                .append(Component.literal(" [ERROR]")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF0000))))

        return formatTooltipComponent(originalLine, profile, floor, isMaster)
    }

    /**
     * Formats the tooltip component with level, personal best, secrets and fairy perk.
     *
     * @param originalLine The original line of the tooltip
     * @param profile The player's dungeon profile
     * @param floor The floor of the dungeon
     * @param isMaster Whether the dungeon is in master mode
     * @return The formatted tooltip component
     */
    private fun formatTooltipComponent(
        originalLine: Component,
        profile: DungeonProfile,
        floor: Int?,
        isMaster: Boolean
    ): Component {
        val pbText = floor
            ?.let { profile.bestTime(it, isMaster)?.let { ms -> formatTime(ms) } ?: "NO PB" }
            ?: "ERROR"

        val base = Component.empty()
        base.append(recolorClassComponent(originalLine))
        base.append(Component.literal(" ${getLevelColor(profile.catacombsLevel)}C${profile.catacombsLevel}"))
        base.append(
            Component.literal(" $pbText")
                .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFFFF55)))
        )

        if (showSecrets) {
            base.append(
                Component.literal(" ${profile.totalSecrets}")
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(0xFF55FF)))
            )
        }

        if (showFairyPerk && profile.hasFairyPerk) {
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
}