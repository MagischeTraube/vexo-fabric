package xyz.vexo.utils

import net.minecraft.world.scores.DisplaySlot
import xyz.vexo.Vexo.mc

object ScoreboardUtils {
    fun getEntries(): List<String> {
        val scoreboard = mc.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        return scoreboard.listPlayerScores(objective)
            .filter { !it.isHidden }
            .sortedByDescending { it.value() }
            .map { it.owner().removeFormatting() }
    }

    fun find(pattern: Regex): String? =
        getEntries().firstOrNull { pattern.containsMatchIn(it) }

    fun findAll(pattern: Regex): List<String> =
        getEntries().filter { pattern.containsMatchIn(it) }
}