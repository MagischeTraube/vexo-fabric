package xyz.vexo.utils

import kotlin.math.floor
import kotlinx.coroutines.launch
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.world.entity.monster.zombie.Zombie
import xyz.vexo.Vexo
import xyz.vexo.Vexo.mc
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.PacketReceiveEvent
import xyz.vexo.events.impl.ServerConnectEvent
import xyz.vexo.events.impl.WorldJoinEvent

/**
 * Estimates the current dungeon score from the tab list, the sidebar and chat
 * (skill + exploration + speed + bonus, the same formula other mods use).
 */
object DungeonScore {
    private const val ELECTION_URL = "https://api.hypixel.net/v2/resources/skyblock/election"
    private const val ELECTION_REFRESH_MS = 30 * 60 * 1000L

    /** Highest score reached in the current dungeon, 0 outside dungeons. */
    @Volatile var score = 0
        private set

    @Volatile private var mimicKilled = false
    @Volatile private var princeKilled = false
    @Volatile private var inBoss = false
    @Volatile private var watcherCleared = false
    @Volatile private var chatDeaths = 0

    private var clearedAtWatcher: Int? = null
    private var bloodDone = false
    private var tickCounter = 0

    @Volatile private var paul_ezpz = false
    @Volatile private var lastElectionFetch = 0L

    private val SECRETS_PERCENT_REGEX = Regex("""Secrets Found: ([\d.]+)%""")
    private val CRYPTS_REGEX = Regex("""Crypts: (\d+)""")
    private val COMPLETED_ROOMS_REGEX = Regex("""Completed Rooms: (\d+)""")
    private val DEATHS_REGEX = Regex("""Deaths: \(?(\d+)\)?""")
    private val OPEN_PUZZLE_REGEX = Regex(""": \[[✦✖]]""")
    private val CLEARED_REGEX = Regex("""Cleared: (\d+)%""")
    private val ELAPSED_REGEX = Regex("""Time Elapsed: (?:(\d+)h ?)?(?:(\d+)m ?)?(?:(\d+)s)?""")
    private val BOSS_DIALOGUE_REGEX = Regex("""^\[BOSS] (?!The Watcher:).+$""")
    private val DEATH_REGEX = Regex("""^ ☠ .+ became a ghost\.$""")
    private val MIMIC_CHARM_REGEX = Regex("""charm you charmed a mimic and captured (?:\d+ shards from it|its shard)\.""")

    private const val WATCHER_CLEAR = "[BOSS] The Watcher: You have proven yourself. You may pass."
    private const val PRINCE_KILLED = "a prince falls. +1 bonus score"

    private val mimicMessages = setOf(
        "mimic dead!", "mimic dead", "mimic killed!", "mimic killed", "\$skytils-dungeon-score-mimic$",
        "child destroyed!", "mimic obliterated!", "mimic exorcised!", "mimic destroyed!", "mimic annhilated!",
        "breefing killed", "breefing dead", "bwoafing dead", "mimic"
    )

    private val princeMessages = setOf(
        "prince dead", "prince dead!", "\$skytils-dungeon-score-prince$", "prince killed", "prince slain", "prince killed!"
    )

    private val requiredSecretPercentage = mapOf(
        "E" to 0.3, "F1" to 0.3, "F2" to 0.4, "F3" to 0.5, "F4" to 0.6, "F5" to 0.7, "F6" to 0.85, "F7" to 1.0
    )

    private val timeLimit = mapOf(
        "E" to 600, "F1" to 600, "F2" to 600, "F3" to 600, "F4" to 720, "F5" to 600, "F6" to 720, "F7" to 840,
        "M1" to 480, "M2" to 480, "M3" to 480, "M4" to 480, "M5" to 480, "M6" to 600, "M7" to 840
    )

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        score = 0
        mimicKilled = false
        princeKilled = false
        inBoss = false
        watcherCleared = false
        chatDeaths = 0
        clearedAtWatcher = null
        bloodDone = false
    }

    @EventHandler
    fun onServerConnect(event: ServerConnectEvent) {
        if (System.currentTimeMillis() - lastElectionFetch < ELECTION_REFRESH_MS) return
        lastElectionFetch = System.currentTimeMillis()
        Vexo.scope.launch {
            val json = RequestUtils.fetchJsonWithRetry(ELECTION_URL) ?: return@launch
            runCatching {
                val mayor = json.getAsJsonObject("mayor")
                val mayorEzpz = mayor.getAsJsonArray("perks")
                    ?.any { it.asJsonObject.get("name")?.asString == "EZPZ" } == true
                val ministerEzpz = mayor.getAsJsonObject("minister")
                    ?.getAsJsonObject("perk")?.get("name")?.asString == "EZPZ"
                paul_ezpz = mayorEzpz || ministerEzpz
            }.onFailure { logError(it, this@DungeonScore) }
        }
    }

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        val message = event.unformattedMessage
        val lower = message.lowercase()
        when {
            message == WATCHER_CLEAR -> watcherCleared = true
            BOSS_DIALOGUE_REGEX.matches(message) -> inBoss = true
            DEATH_REGEX.matches(message) -> chatDeaths++
            lower == PRINCE_KILLED || princeMessages.any { lower.contains(it) } -> princeKilled = true
            !inBoss && (MIMIC_CHARM_REGEX.matches(lower) || mimicMessages.any { lower.contains(it) }) -> mimicKilled = true
        }
    }

    @EventHandler
    fun onPacket(event: PacketReceiveEvent) {
        val packet = event.packet as? ClientboundEntityEventPacket ?: return
        if (packet.eventId.toInt() != 3 || mimicKilled || inBoss) return
        mc.execute {
            val level = mc.level ?: return@execute
            if ((packet.getEntity(level) as? Zombie)?.isBaby == true) mimicKilled = true
        }
    }

    @EventHandler
    fun onClientTick(event: ClientTickEvent) {
        if (++tickCounter % 10 != 0 || !DungeonUtils.inDungeon) return
        runCatching { calculate() }.getOrNull()?.let { if (it > score) score = it }
    }

    private fun calculate(): Int? {
        val floor = SplitsManager.currentDungeonFloor() ?: return null
        val floorNumber = floor.drop(1).toIntOrNull() ?: 0
        val tab = TablistUtils.getEntries()
        val sidebar = ScoreboardUtils.getEntries()
        fun find(regex: Regex, lines: List<String> = tab) = lines.firstNotNullOfOrNull { regex.find(it) }

        val cleared = find(CLEARED_REGEX, sidebar)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (cleared <= 0) return null
        val completedRooms = find(COMPLETED_ROOMS_REGEX)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val secretPercent = find(SECRETS_PERCENT_REGEX)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        val crypts = find(CRYPTS_REGEX)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val tabDeaths = find(DEATHS_REGEX)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val openPuzzles = tab.count { OPEN_PUZZLE_REGEX.containsMatchIn(it) }
        val elapsed = (find(ELAPSED_REGEX, sidebar) ?: find(ELAPSED_REGEX))?.groupValues
            ?.let { (it[1].toIntOrNull() ?: 0) * 3600 + (it[2].toIntOrNull() ?: 0) * 60 + (it[3].toIntOrNull() ?: 0) } ?: 0

        if (watcherCleared && !bloodDone) {
            val atWatcher = clearedAtWatcher ?: cleared.also { clearedAtWatcher = it }
            if (cleared != atWatcher) bloodDone = true
        }

        val totalRooms = floor(completedRooms / (cleared / 100.0) + 0.4)
        if (totalRooms <= 0) return null
        val effectiveRooms = completedRooms + (if (!bloodDone) 1 else 0) + (if (!inBoss) 1 else 0)

        val requiredSecrets = requiredSecretPercentage[floor] ?: 1.0
        val secretsScore = floor(secretPercent / requiredSecrets / 100.0 * 40.0).coerceIn(0.0, 40.0).toInt()
        val roomsScore = (effectiveRooms / totalRooms * 60.0).coerceIn(0.0, 60.0).toInt()
        val skillRooms = floor(effectiveRooms / totalRooms * 80.0).coerceIn(0.0, 80.0).toInt()

        val deathPenalty = (maxOf(tabDeaths, chatDeaths) * 2 - 1).coerceAtLeast(0)
        val skillScore = (20 + skillRooms - openPuzzles * 10 - deathPenalty).coerceIn(20, 100)

        val limit = timeLimit[floor] ?: 600
        val speedScore = if (elapsed <= limit) 100
        else (100 - speedDeduction((elapsed - limit) * 100f / limit)).toInt().coerceAtLeast(0)

        var bonus = crypts.coerceAtMost(5)
        if (mimicKilled && floorNumber > 5) bonus += 2
        if (princeKilled) bonus += 1
        if (paul_ezpz) bonus += 10

        return secretsScore + roomsScore + skillScore + speedScore + bonus
    }

    private fun speedDeduction(percentageOver: Float): Float {
        var remaining = percentageOver
        var deduction = 0f
        for ((cap, divisor) in listOf(20f to 2f, 20f to 3.5f, 10f to 4f, 10f to 5f)) {
            if (remaining <= 0) return deduction
            deduction += remaining.coerceAtMost(cap) / divisor
            remaining -= cap
        }
        if (remaining > 0) deduction += remaining / 6f
        return deduction
    }
}
