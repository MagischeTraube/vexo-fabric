package xyz.vexo.features.impl.misc

import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import kotlin.math.ceil

object SlayerHelper : Module(
    name = "Slayer Bosses Remaining",
    description = "Shows remaining Slayer bosses and estimated time",
    toggled = false
) {

    private val bossesRemainingMessage by BooleanSetting(
        name = "Bosses Remaining",
        description = "Bosses needed for next Slayer level"
    )

    private val timeRemainingMessage by BooleanSetting(
        name = "Time Remaining",
        description = "Time needed for next Slayer level"
    )

    private var currentXp = 0
    private var previousXp = 0
    private var xpPerKill = 0

    private var lastKillTime = 0L
    private var avgKillTimeMs = 0L

    private val recentKillTimes = mutableListOf<Long>()
    private const val MAX_RECENT_KILLS = 5

    private val slayerXpRegex = """- Next LVL in ([\d,]+) XP!""".toRegex()

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        val message = event.message.removeFormatting()

        val matched = updateSlayerStatistics(message)
        if (!matched) return

        if (xpPerKill <= 0 || avgKillTimeMs <= 0L) {
            modMessage("Calculating... slay more bosses to get more Data")
            return
        }

        val bossesRemaining = ceil(currentXp.toDouble() / xpPerKill.toDouble()).toInt()

        val totalSeconds = (bossesRemaining * avgKillTimeMs) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val timeString = buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0 || hours > 0) append("${minutes}m")
            if (hours == 0L && minutes == 0L) append("${seconds}s")
        }.trim()

        when {
            bossesRemainingMessage && timeRemainingMessage ->
                modMessage("You need $bossesRemaining more bosses taking ~$timeString to level up")
            bossesRemainingMessage ->
                modMessage("You need $bossesRemaining more bosses")
            timeRemainingMessage ->
                modMessage("You need ~$timeString to level up")
        }
    }

    private fun updateSlayerStatistics(message: String): Boolean {
        val match = slayerXpRegex.find(message) ?: return false

        val now = System.currentTimeMillis()

        val xpString = match.groupValues[1]
        previousXp = currentXp
        currentXp = xpString.replace(",", "").toInt()

        xpPerKill = previousXp - currentXp
        if (xpPerKill <= 0) return false

        if (lastKillTime != 0L) {
            val timeDiff = now - lastKillTime

            recentKillTimes.add(timeDiff)

            if (recentKillTimes.size > MAX_RECENT_KILLS) {
                recentKillTimes.removeAt(0)
            }

            avgKillTimeMs = recentKillTimes.average().toLong()
        }

        lastKillTime = now
        return true
    }
}