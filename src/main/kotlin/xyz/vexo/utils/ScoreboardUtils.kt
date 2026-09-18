package xyz.vexo.utils

import net.minecraft.world.scores.DisplaySlot
import xyz.vexo.Vexo.mc

object ScoreboardUtils {
    /**
     * Returns a list of entries from the sidebar scoreboard.
     *
     * @return A list of entries from the sidebar scoreboard.
     */
    fun getEntries(): List<String> {
        val scoreboard = mc.level?.scoreboard ?: return emptyList()
        val objective = scoreboard.getDisplayObjective(DisplaySlot.SIDEBAR) ?: return emptyList()
        return scoreboard.listPlayerScores(objective)
            .filter { !it.isHidden }
            .sortedByDescending { it.value() }
            .map { it.owner().removeFormatting() }
    }

    /**
     * Returns the first entry that matches the given regex.
     *
     * @param pattern The regex pattern to match.
     * @return The first entry that matches the pattern, or null if no match is found.
     */
    fun find(pattern: Regex): String? =
        getEntries().firstOrNull { pattern.containsMatchIn(it) }

    /**
     * Returns a list of entries that match the given regex.
     *
     * @param pattern The regex pattern to match.
     * @return A list of entries that match the pattern.
     */
    fun findAll(pattern: Regex): List<String> =
        getEntries().filter { pattern.containsMatchIn(it) }
}