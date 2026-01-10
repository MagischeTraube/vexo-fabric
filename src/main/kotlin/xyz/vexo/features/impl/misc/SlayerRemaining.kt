package xyz.vexo.features.impl.misc

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import kotlin.math.ceil



object SlayerRemaining : Module (
    name = "Slayer Bosses Remaining",
    description = "Tells you how many Slayer bosses you need to kill to reach the new level",
    toggled = false
) {
    private var currentXp: Int = 0
    private var previousXp: Int = 0
    private var xpPerKill: Int = 0

    val SLAYER_XP_REGEX = """- Next LVL in ([\d,]+) XP!""".toRegex()

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        val message = event.message.removeFormatting()

        // XP aus der Slayer-Info extrahieren
        var matchedRegex = updateSlayerStatistics(message)

        // Bei Quest-Complete die XP pro Kill berechnen und verbleibende Bosse ausgeben
        if (matchedRegex && xpPerKill > 0) {

            val bossesRemaining = ceil(currentXp.toDouble() / xpPerKill.toDouble()).toInt()

            modMessage("You need $bossesRemaining more bosses to reach the next level (${xpPerKill} XP per kill)")

        }
    }



    fun updateSlayerStatistics(message: String): Boolean {
        val matchResult = SLAYER_XP_REGEX.find(message)

        if (matchResult != null) {
            val xpString = matchResult.groupValues[1]
            previousXp = currentXp
            currentXp = xpString.replace(",", "").toInt()
            xpPerKill = previousXp - currentXp

            return true
        }

        return false
    }
}