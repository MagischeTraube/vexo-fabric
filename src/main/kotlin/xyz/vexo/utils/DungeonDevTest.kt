package xyz.vexo.utils

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ServerTickEvent

object DungeonDevTest {

    private var tickCounter = 0

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        if (++tickCounter % 20 != 0) return

        DevMode.ifFlag("printDeaths") {
            modMessage("Deaths: ${DungeonUtils.getDeaths()}")
        }
        DevMode.ifFlag("printBlessings") {
            val b = DungeonUtils.getBlessings()
            modMessage("Blessings: ${if (b.isEmpty()) "none" else b.entries.joinToString { "${it.key} ${it.value}" }}")
        }
        DevMode.ifFlag("printTime") {
            modMessage("Time: ${DungeonUtils.getElapsedTime() ?: "?"}")
        }
        DevMode.ifFlag("printPuzzles") {
            modMessage("Puzzles total: ${DungeonUtils.getPuzzleCount()}")
        }
        DevMode.ifFlag("printMates") {
            val mates = DungeonUtils.getMates()
            if (mates.isEmpty()) modMessage("Mates: none")
            else mates.forEach { modMessage("Mate: ${it.name} (${it.dungeonClass})${if (it.isDead) " DEAD" else ""}") }
        }
        DevMode.ifFlag("printInDungeon") {
            modMessage("inDungeon: ${DungeonUtils.inDungeon}, floor: ${DungeonUtils.floor.ifEmpty { "?" }}")
        }
        DevMode.ifFlag("printTablist") {
            TablistUtils.getEntries().forEachIndexed { i, line ->
                modMessage("[$i] $line")
            }
        }
        DevMode.ifFlag("printScoreboard") {
            ScoreboardUtils.getEntries().forEachIndexed { i, line ->
                modMessage("[$i] $line")
            }
        }
    }
}
