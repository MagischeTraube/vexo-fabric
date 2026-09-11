package xyz.vexo.utils

import xyz.vexo.Vexo.mc

object TablistUtils {
    /**
     * Returns a list of entries from the tablist.
     *
     * @return A list of entries from the tablist.
     */
    fun getEntries(): List<String> =
        mc.connection?.getListedOnlinePlayers()
            ?.map { it.tabListDisplayName?.string?.removeFormatting() ?: it.profile.name }
            ?: emptyList()

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
