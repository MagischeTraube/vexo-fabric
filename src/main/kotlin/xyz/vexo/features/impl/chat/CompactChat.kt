package xyz.vexo.features.impl.chat

import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessageEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage

object CompactChat : Module(
    name = "Compact Chat",
    description = "Rewrites verbose chat messages into shorter, compact versions",
    toggled = false
) {
    private val compactLootShare by BooleanSetting(
        name = "Compact Loot Share",
        default = false
    )

    private val compactCapture by BooleanSetting(
        name = "Compact Capture",
        default = false
    )

    private val lootShareRegex = Regex("""^LOOT SHARE! You received (a|\d+x) (.+) Shard from (.+) catching a .+$""")

    private val captureRegexes = listOf(
        Regex("""^CAPTURE! You found the (.+), and as a reward it gave you (a|\d+x) .+ Shard!$"""),
        Regex("""^CAPTURE! You caught a (.+) and gained (a|\d+x) .+ Shard!$""")
    )

    @EventHandler
    fun onChat(event: ChatMessageEvent) {
        val message = event.unformattedMessage

        if (compactLootShare) {
            lootShareRegex.find(message)?.let { match ->
                val (amount, shard, player) = match.destructured
                event.cancel()
                val amountPrefix = if (amount == "a") "" else "§7$amount "
                modMessage("§e§lLS! $amountPrefix§a$shard §7by §b$player", prefix = "")
                return
            }
        }

        if (compactCapture) {
            for (regex in captureRegexes) {
                val match = regex.find(message) ?: continue
                val creature = match.groupValues[1]
                val amount = match.groupValues.getOrElse(2) { "a" }
                event.cancel()
                val amountPrefix = if (amount == "a") "" else "§7$amount "
                modMessage("§a§lCAP! $amountPrefix§9$creature", prefix = "")
                return
            }
        }
    }
}
