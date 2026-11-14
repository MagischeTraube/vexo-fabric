package xyz.vexo.utils

import kotlin.jvm.optionals.getOrNull
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.PacketReceiveEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.impl.misc.Splits

/**
 * @param requiresPrevious Only match once the previous split has started, for regexes shared by several
 *        splits (e.g. the terminal sections) that must fill in order.
 */
data class Split(
    val regex: Regex,
    val name: String,
    var time: Long = 0L,
    var ticks: Long = 0L,
    val requiresPrevious: Boolean = false,
)
data class SplitsGroup(val splits: List<Split>, val personalBest: PersonalBest?, val area: SplitArea?)
/**
 * @param lag Real time lost to server lag in ms. For running splits it is measured at the last server tick,
 *        so it only changes once per tick instead of jittering every frame.
 */
data class SplitRow(
    val name: String,
    val time: Long,
    val tickTime: Long,
    val isCurrent: Boolean,
    val completed: Boolean = false,
    val lag: Long = 0L,
)

/** Time since run start (real ms and server ticks) at which a milestone was reached. */
data class MilestoneTime(val time: Long, val ticks: Long)

enum class SplitArea { DUNGEON, KUUDRA }

/**
 * Tracks split times for Dungeon and Kuudra runs based on chat messages.
 */
object SplitsManager {

    private data class RunLocation(val area: SplitArea, val id: String)

    @Volatile private var currentSplits = SplitsGroup(emptyList(), null, null)
    @Volatile private var tickCounter = 0L
    @Volatile private var lastTickRealTime = 0L
    @Volatile private var score300: MilestoneTime? = null

    /** Last floor / tier seen in a sidebar team update; survives world changes on purpose (see [onPacket]). */
    @Volatile private var lastSeenLocation: RunLocation? = null

    private val KUUDRA_TIER_REGEX = Regex("""Kuudra's Hollow \(T([1-5])\)""")

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        val message = event.unformattedMessage
        if (message == "Starting in 1 second.") startRun() else onSplitMessage(message)
    }

    /**
     * Caches the floor / tier from sidebar team packets as they arrive (like Odin and NoammAddons),
     * instead of relying on a single sidebar read at run start. Not reset on world join: packets are
     * handled on the network thread and can arrive before the join event fires on the main thread.
     */
    @EventHandler
    fun onPacket(event: PacketReceiveEvent) {
        val packet = event.packet as? ClientboundSetPlayerTeamPacket ?: return
        val params = packet.parameters.getOrNull() ?: return
        val text = (params.playerPrefix.string + params.playerSuffix.string).removeFormatting()
        detectLocation(text)?.let { lastSeenLocation = it }
    }

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        tickCounter++
        lastTickRealTime = System.currentTimeMillis()
        checkScore300()
    }

    /** Remembers when the dungeon score first reached 300 during the tracked run. */
    private fun checkScore300() {
        if (score300 != null || currentSplits.area != SplitArea.DUNGEON) return
        val start = currentSplits.splits.firstOrNull()?.takeIf { it.time != 0L } ?: return
        if (currentSplits.splits.last().time != 0L || DungeonScore.score < 300) return
        score300 = MilestoneTime(lastTickRealTime - start.time, tickCounter - start.ticks)
    }

    /** When 300 score was reached in the tracked run, or null if not (yet). */
    fun score300Time(): MilestoneTime? = score300

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        currentSplits = SplitsGroup(emptyList(), null, null)
        tickCounter = 0L
        score300 = null
    }

    private fun detectLocation(text: String): RunLocation? {
        if ("Queue" in text) return null
        DungeonUtils.parseFloor(text)?.let { return RunLocation(SplitArea.DUNGEON, it) }
        KUUDRA_TIER_REGEX.find(text)?.let { return RunLocation(SplitArea.KUUDRA, it.groupValues[1]) }
        return null
    }

    /**
     * Floor / tier from the cached team packets; the sidebar is only read as a fallback. Callers run on the
     * network thread, where reading the scoreboard can race with the main thread, so that read may fail.
     */
    private fun currentLocation(): RunLocation? = lastSeenLocation
        ?: runCatching { ScoreboardUtils.getEntries().firstNotNullOfOrNull { detectLocation(it) } }.getOrNull()

    /**
     * @return The current dungeon floor as shown on the scoreboard ("E", "F1" … "M7"), or null outside dungeons.
     */
    fun currentDungeonFloor(): String? =
        currentLocation()?.takeIf { it.area == SplitArea.DUNGEON }?.id

    private fun startRun() {
        tickCounter = 0L
        score300 = null
        currentSplits = buildSplits() ?: SplitsGroup(emptyList(), null, null)
    }

    private fun buildSplits(): SplitsGroup? {
        val location = currentLocation() ?: return null
        return when (location.area) {
            SplitArea.DUNGEON -> buildDungeonSplits(location.id)
            SplitArea.KUUDRA -> buildKuudraSplits(location.id.toInt())
        }
    }

    /** Starts tracking late if "Starting in 1 second." was missed or the floor wasn't known yet. */
    private fun startRunLate(message: String) {
        if (!MORT_REGEX.matches(message) && !KUUDRA_START_REGEX.matches(message)) return
        tickCounter = 0L
        score300 = null
        buildSplits()?.let { currentSplits = it }
    }

    /**
     * @param floor The floor as shown on the scoreboard, e.g. "E", "F7" or "M7".
     */
    private fun buildDungeonSplits(floor: String): SplitsGroup =
        SplitsGroup(dungeonSplitList(floor), Splits.dungeonPBs[floor], SplitArea.DUNGEON)

    /** Fresh split list for a floor ("E", "F1" … "M7"), the last entry being the total. */
    private fun dungeonSplitList(floor: String): List<Split> {
        val floorNumber = floor.drop(1).toIntOrNull() ?: 0
        return buildList {
            add(Split(MORT_REGEX, "§2Blood Open"))
            add(Split(BLOOD_OPEN_REGEX, "§bBlood Clear"))
            add(Split(PORTAL_ENTRY_REGEX, "§dPortal Entry"))
            val masterMode = floor.startsWith("M")
            addAll(dungeonSplits.getOrElse(floorNumber) { emptyList() }.filterNot { !masterMode && floorNumber == 7 && it.name == "§4Cleared" })
            add(Split(DUNGEON_CLEARED_REGEX, "§1Total"))
        }.map { it.copy() }
    }

    /** Split names (the PB keys) of a floor, the last one being the total. */
    fun dungeonSplitNames(floor: String): List<String> = dungeonSplitList(floor).map { it.name }

    private fun buildKuudraSplits(tier: Int): SplitsGroup {
        val group = if (tier == 5) kuudraT5SplitsGroup else kuudraSplitsGroup
        return SplitsGroup(group.map { it.copy() }, Splits.kuudraPBs[tier], SplitArea.KUUDRA)
    }

    private fun onSplitMessage(message: String) {
        if (currentSplits.splits.isEmpty()) startRunLate(message)
        val splits = currentSplits.splits
        val index = splits.indices.firstOrNull { i ->
            val split = splits[i]
            split.time == 0L && split.regex.matches(message) &&
                (!split.requiresPrevious || (i > 0 && splits[i - 1].time != 0L))
        } ?: return
        val split = splits[index]

        split.time = System.currentTimeMillis()
        split.ticks = tickCounter

        if (index == 0) return

        val previous = splits[index - 1]
        val segmentTime = (split.time - previous.time) / 1000f

        if (!displayAllowed()) return
        if (index == splits.lastIndex) finishRun(index, segmentTime)
        else currentSplits.personalBest?.time(previous.name, segmentTime, "§6${previous.name} §7took §6", Splits.enabled)
    }

    private fun finishRun(totalIndex: Int, lastSegmentTime: Float) {
        val rows = currentRows()
        val splits = currentSplits.splits
        val personalBest = currentSplits.personalBest

        runAfterServerTicks(10) {
            if (rows.isEmpty()) return@runAfterServerTicks
            personalBest?.time(splits[totalIndex - 1].name, lastSegmentTime, "§6${splits[totalIndex - 1].name} §7took §6", Splits.enabled)
            personalBest?.time(splits[totalIndex].name, rows.last().time / 1000f, "§6Total time §7took §6", Splits.enabled)
            if (!Splits.enabled) return@runAfterServerTicks
            rows.forEachIndexed { i, row ->
                modMessage("§6${if (i == rows.lastIndex) "Total" else row.name} §7took §6${formatTime(row.time)}§7.")
            }
        }
    }

    private fun displayAllowed(): Boolean = when (currentSplits.area) {
        SplitArea.DUNGEON -> Splits.splitLocation != "Kuudra Only"
        SplitArea.KUUDRA -> Splits.splitLocation != "Dungeons Only"
        null -> true
    }

    /** Personal bests of the current run's floor / tier, or null if no run is tracked. */
    fun currentPersonalBest(): PersonalBest? = currentSplits.personalBest

    /** Area of the tracked run, or null if no run is tracked. */
    fun currentArea(): SplitArea? = currentSplits.area

    fun currentRows(): List<SplitRow> {
        val splits = currentSplits.splits
        if (splits.isEmpty() || !displayAllowed()) return emptyList()
        if (splits[0].time == 0L) return splits.map { SplitRow(it.name, 0L, 0L, isCurrent = false) }

        val last = splits.last()
        val latestTime = if (last.time != 0L) last.time else System.currentTimeMillis()
        val latestTicks = if (last.ticks != 0L) last.ticks else tickCounter
        // Real time at the last server tick, so the lag of running splits doesn't jitter between ticks.
        val latestTickTime = if (last.time != 0L) last.time else lastTickRealTime.coerceAtLeast(splits.first().time)

        val times = LongArray(splits.size).apply { this[splits.lastIndex] = latestTime - splits.first().time }
        val ticks = LongArray(splits.size).apply { this[splits.lastIndex] = latestTicks - splits.first().ticks }
        val lags = LongArray(splits.size).apply {
            this[splits.lastIndex] = latestTickTime - splits.first().time - ticks[splits.lastIndex] * 50
        }

        var currentIndex = -1
        for (i in 0 until splits.lastIndex) {
            val next = splits[i + 1]
            if (next.time != 0L) {
                times[i] = next.time - splits[i].time
                ticks[i] = next.ticks - splits[i].ticks
                lags[i] = times[i] - ticks[i] * 50
            } else {
                times[i] = latestTime - splits[i].time
                ticks[i] = latestTicks - splits[i].ticks
                lags[i] = latestTickTime.coerceAtLeast(splits[i].time) - splits[i].time - ticks[i] * 50
                currentIndex = i
                break
            }
        }
        return splits.indices.map { i ->
            val completed = if (i == splits.lastIndex) last.time != 0L else splits[i + 1].time != 0L
            SplitRow(
                splits[i].name, times[i], ticks[i],
                isCurrent = i == currentIndex, completed = completed, lag = lags[i].coerceAtLeast(0)
            )
        }
    }
}

/**
 * Formats a duration in milliseconds as e.g. "1h 2m 3.45s".
 */
fun formatTime(time: Long, decimalPlaces: Int = 2): String {
    if (time == 0L) return "0s"
    var remaining = time
    val hours = (remaining / 3600000).toInt().let {
        remaining -= it * 3600000L
        if (it > 0) "${it}h " else ""
    }
    val minutes = (remaining / 60000).toInt().let {
        remaining -= it * 60000L
        if (it > 0) "${it}m " else ""
    }
    return "$hours$minutes${"%.${decimalPlaces}f".format(remaining / 1000f)}s"
}

private val TIME_PART_REGEX = Regex("""(\d+(?:\.\d+)?)\s*([hms])""")

/**
 * Parses a time typed by the user into seconds. Accepts "1m 23.45s", "1h 2m 3s", "1:23.45" and "83.45".
 *
 * @return The time in seconds, or null if the text is not a valid positive time.
 */
fun parseTime(text: String): Float? {
    val input = text.trim().lowercase()
    if (input.isEmpty()) return null

    val seconds = when {
        ':' in input -> {
            val parts = input.split(':')
            if (parts.size > 3) return null
            parts.fold(0f) { total, part -> total * 60 + (part.trim().toFloatOrNull() ?: return null) }
        }
        input.toFloatOrNull() != null -> input.toFloat()
        else -> {
            val parts = TIME_PART_REGEX.findAll(input).toList()
            if (parts.joinToString("") { it.value }.replace(" ", "") != input.replace(" ", "")) return null
            parts.sumOf { part ->
                val amount = part.groupValues[1].toDouble()
                when (part.groupValues[2]) {
                    "h" -> amount * 3600
                    "m" -> amount * 60
                    else -> amount
                }
            }.toFloat()
        }
    }
    return seconds.takeIf { it > 0f }
}

// Declared before the split groups below, which use them while initializing.

/** Names of the Goldor terminal section splits, in order. */
val terminalSectionNames = listOf("§6S1", "§6S2", "§6S3", "§6S4")

/** Last terminal / lever / device of a section, e.g. "Player activated a terminal! (7/7)". */
private val TERMINAL_SECTION_DONE_REGEX = Regex("""^.{1,16} (?:activated|completed) a (?:terminal|lever|device)! \((\d)/\1\)$""")

private val kuudraT5SplitsGroup = listOf(
    Split(Regex("^\\[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!$"), "§2Supplies"),
    Split(Regex("^\\[NPC] Elle: OMG! Great work collecting my supplies!$"), "§bBuild"),
    Split(Regex("^\\[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!$"), "§dEaten"),
    Split(Regex("^(?!Elle has been eaten by Kuudra!$)(.{1,16}) has been eaten by Kuudra!$"), "§cStun"),
    Split(Regex("^(.{1,16}) destroyed one of Kuudra's pods!$"), "§4DPS"),
    Split(Regex("^\\[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!$"), "§4Cleared"),
    Split(Regex("^\\[NPC] Elle: Good job everyone. A hard fought battle come to an end. Let's get out of here before we run into any more trouble!$"), "Total"))

private val kuudraSplitsGroup = listOf(
    Split(Regex("^\\[NPC] Elle: Okay adventurers, I will go and fish up Kuudra!$"), "§2Supplies"),
    Split(Regex("^\\[NPC] Elle: OMG! Great work collecting my supplies!$"), "§bBuild"),
    Split(Regex("^\\[NPC] Elle: Phew! The Ballista is finally ready! It should be strong enough to tank Kuudra's blows now!$"), "§cStun"),
    Split(Regex("^\\[NPC] Elle: POW! SURELY THAT'S IT! I don't think he has any more in him!$"), "§4Cleared"),
    Split(Regex("^\\[NPC] Elle: Good job everyone. A hard fought battle come to an end. Let's get out of here before we run into any more trouble!$"), "Total"))

private val entryRegexes = listOf(
    Regex("^\\[BOSS] Bonzo: Gratz for making it this far, but I'm basically unbeatable\\.$"),
    Regex("^\\[BOSS] Scarf: This is where the journey ends for you, Adventurers\\.$"),
    Regex("^\\[BOSS] The Professor: I was burdened with terrible news recently\\.\\.\\.$"),
    Regex("^\\[BOSS] Thorn: Welcome Adventurers! I am Thorn, the Spirit! And host of the Vegan Trials!$"),
    Regex("^\\[BOSS] Livid: Welcome, you've arrived right on time\\. I am Livid, the Master of Shadows\\.$"),
    Regex("^\\[BOSS] Sadan: So you made it all the way here\\.\\.\\. Now you wish to defy me\\? Sadan\\?!$"),
    Regex("^\\[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!$")
)

private val floor1SplitGroup = listOf(
    Split(entryRegexes[0], "§cBonzo's Sike"),
    Split(Regex("\\[BOSS] Bonzo: Oh I'm dead!"), "§4Cleared"),
)

private val floor2SplitGroup = listOf(
    Split(entryRegexes[1], "§cScarf's minions"),
    Split(Regex("^\\[BOSS] Scarf: Did you forget\\? I was taught by the best! Let's dance\\.$"), "§4Cleared"),
)

private val floor3SplitGroup = listOf(
    Split(entryRegexes[2], "§cThe Guardians"),
    Split(Regex("^\\[BOSS] The Professor: Oh\\? You found my Guardians' one weakness\\?$"), "§aThe Professor"),
    Split(Regex("^\\[BOSS] The Professor: What\\?! My Guardian power is unbeatable!$"), "§4Cleared"),
)

private val floor4SplitGroup = listOf(
    Split(entryRegexes[3], "§4Cleared"),
)

private val floor5SplitGroup = listOf(
    Split(entryRegexes[4], "§4Cleared"),
)

private val floor6SplitGroup = listOf(
    Split(entryRegexes[5], "§cTerracottas"),
    Split(Regex("^\\[BOSS] Sadan: ENOUGH!$"), "§aGiants"),
    Split(Regex("^\\[BOSS] Sadan: You did it\\. I understand now, you have earned my respect\\.$"), "§4Cleared"),
)

val floor7SplitGroup = listOf(
    Split(entryRegexes[6], "§5Maxor"),
    Split(Regex("\\[BOSS] Storm: Pathetic Maxor, just like expected\\."), "§3Storm"),
    Split(Regex("\\[BOSS] Goldor: Who dares trespass into my domain\\?"), "§6S1"),
    Split(TERMINAL_SECTION_DONE_REGEX, "§6S2", requiresPrevious = true),
    Split(TERMINAL_SECTION_DONE_REGEX, "§6S3", requiresPrevious = true),
    Split(TERMINAL_SECTION_DONE_REGEX, "§6S4", requiresPrevious = true),
    Split(Regex("The Core entrance is opening!"), "§7Goldor"),
    Split(Regex("\\[BOSS] Necron: You went further than any human before, congratulations\\."), "§cNecron"),
    Split(Regex("^\\[BOSS] Wither King: You\\.\\.\\. again\\?$"), "§4Cleared"),
)

private val dungeonSplits = listOf(
    emptyList(),
    floor1SplitGroup,
    floor2SplitGroup,
    floor3SplitGroup,
    floor4SplitGroup,
    floor5SplitGroup,
    floor6SplitGroup,
    floor7SplitGroup,
)

private val PORTAL_ENTRY_REGEX = Regex("\\[BOSS] The Watcher: You have proven yourself\\. You may pass\\.")
private val DUNGEON_CLEARED_REGEX = Regex("^\\s*☠ Defeated (.+) in 0?([\\dhms ]+?)\\s*(\\(NEW RECORD!\\))?$")

// https://regex101.com/r/BXKhOI/1
private val BLOOD_OPEN_REGEX = Regex("^\\[BOSS] The Watcher: (Congratulations, you made it through the Entrance\\.|Ah, you've finally arrived\\.|Ah, we meet again\\.\\.\\.|So you made it this far\\.\\.\\. interesting\\.|You've managed to scratch and claw your way here, eh\\?|I'm starting to get tired of seeing you around here\\.\\.\\.|Oh\\.\\. hello\\?|Things feel a little more roomy now, eh\\?)$|^The BLOOD DOOR has been opened!$")
private val KUUDRA_START_REGEX = kuudraSplitsGroup.first().regex
private val MORT_REGEX = Regex("\\[NPC] Mort: Here, I found this map when I first entered the dungeon\\.|\\[NPC] Mort: Right-click the Orb for spells, and Left-click \\(or Drop\\) to use your Ultimate!")
