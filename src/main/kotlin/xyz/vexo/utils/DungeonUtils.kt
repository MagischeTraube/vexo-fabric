package xyz.vexo.utils

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ServerTickEvent

object DungeonUtils {

    var inDungeon = false
        private set
    var floor: String = ""
        private set

    private var tickCounter = 0

    // "Time Elapsed:" erscheint nur in Catacombs-Dungeons in der Tablist
    private val dungeonIndicator = Regex("""Time Elapsed:""")

    // Format: "Floor I" / "Floor II" ... oder "Master Mode Floor I"
    private val floorPattern = Regex("""(?:Master Mode )?Floor ([IVX]+)""")

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        if (++tickCounter % 20 != 0) return
        val entries = TablistUtils.getEntries()
        inDungeon = entries.any { dungeonIndicator.containsMatchIn(it) }
        floor = if (inDungeon) {
            entries.firstNotNullOfOrNull { floorPattern.find(it)?.value } ?: ""
        } else ""
    }

    data class DungeonMate(val name: String, val dungeonClass: String, val isDead: Boolean)

    // Format: "[432] [MVP++] PlayerName ✓ (Mage L)" oder "(DEAD)"
    private val matePattern = Regex("""\[\d+] (?:\[[^\]]+] )?(\S+)[^(]*\((?:(\S+) \S+|(DEAD))\)""")

    fun getMates(): List<DungeonMate> =
        TablistUtils.getEntries().mapNotNull { line ->
            val m = matePattern.find(line) ?: return@mapNotNull null
            val name = m.groupValues[1]
            val dead = m.groupValues[3] == "DEAD"
            val dungeonClass = if (dead) "DEAD" else m.groupValues[2]
            DungeonMate(name, dungeonClass, dead)
        }

    // Format: "Blessing of Power 5" in der Tablist
    private val blessingPattern = Regex("""Blessing of (\w+) (\d+)""", RegexOption.IGNORE_CASE)

    fun getBlessings(): Map<String, Int> =
        TablistUtils.getEntries()
            .mapNotNull { blessingPattern.find(it) }
            .associate { it.groupValues[1] to it.groupValues[2].toInt() }

    // Format: "Puzzles: (3)" — nur Gesamtzahl
    fun getPuzzleCount(): Int {
        val line = TablistUtils.find(Regex("""Puzzles: \((\d+)\)""")) ?: return 0
        return Regex("""Puzzles: \((\d+)\)""").find(line)?.groupValues?.get(1)?.toInt() ?: 0
    }

    // Format: "Time Elapsed: 4m 32s" oder "Time Elapsed: 32s"
    private val timePattern = Regex("""Time Elapsed: (?:(\d+)m )?(\d+)s""")

    fun getElapsedTime(): String? {
        val line = TablistUtils.find(timePattern) ?: return null
        val m = timePattern.find(line) ?: return null
        val mins = m.groupValues[1]
        val secs = m.groupValues[2]
        return if (mins.isNotEmpty()) "${mins}m ${secs}s" else "${secs}s"
    }

    // Format: "Deaths: 2"
    fun getDeaths(): Int {
        val line = TablistUtils.find(Regex("""Deaths: \d+""")) ?: return 0
        return Regex("""Deaths: (\d+)""").find(line)?.groupValues?.get(1)?.toInt() ?: 0
    }
}
