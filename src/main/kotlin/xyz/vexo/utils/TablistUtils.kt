package xyz.vexo.utils

import xyz.vexo.Vexo.mc

object TablistUtils {
    fun getEntries(): List<String> =
        mc.connection?.getListedOnlinePlayers()
            ?.map { it.tabListDisplayName?.string?.removeFormatting() ?: it.profile.name }
            ?: emptyList()

    fun find(pattern: Regex): String? =
        getEntries().firstOrNull { pattern.containsMatchIn(it) }

    fun findAll(pattern: Regex): List<String> =
        getEntries().filter { pattern.containsMatchIn(it) }
}
