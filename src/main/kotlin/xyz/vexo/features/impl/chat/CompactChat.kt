package xyz.vexo.features.impl.chat

import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessageEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modClickableMessage
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

    private val compactTorrhus by BooleanSetting(
        name = "Compact Torrhus Messages",
        default = false
    )

    private val compactHoppity by BooleanSetting(
        name = "Compact Hoppity Messages",
        default = false
    )

    private val compactAutopet by BooleanSetting(
        name = "Compact Autopet Messages",
        default = false
    )

    private val compactStash by BooleanSetting(
        name = "Compact Stash Messages",
        default = false
    )

    private val lootShareRegex = Regex("""^LOOT SHARE! You received (a|\d+x) (.+) Shard from (.+) catching a .+$""")

    private val captureRegexes = listOf(
        Regex("""^CAPTURE! You found the (.+), and as a reward it gave you (a|\d+x) .+ Shard!$"""),
        Regex("""^CAPTURE! You caught a (.+) and gained (a|\d+x) .+ Shard!$""")
    )

    private val hiveRegex = Regex("""^HIVE! You found (.+?)(?: x(\d+))? and (.+?)(?: x(\d+))?!$""")

    private val hoppityFindRegex = Regex("""^HOPPITY'S HUNT You found (.+) \((\w+)\)!$""")
    private val hoppityDuplicateRegex = Regex("""^DUPLICATE RABBIT! \+([\d,]+) Chocolate$""")

    private val autopetRegex = Regex("""^Autopet equipped your \[Lvl (\d+)] (.+)! VIEW RULE(?: \(x(\d+)\))?$""")

    private val stashRegex = Regex("""^You have (\d+) (materials?|items?) stashed away!$""")

    private var pendingHoppityRabbit: String? = null
    private var pendingHoppityRaw: String? = null

    private fun abbreviateNumber(raw: String): String {
        val value = raw.replace(",", "").toLongOrNull() ?: return raw
        return when {
            value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
            value >= 1_000 -> "%.1fK".format(value / 1_000.0)
            else -> value.toString()
        }
    }

    @EventHandler
    fun onChat(event: ChatMessageEvent) {
        val message = event.unformattedMessage

        if (compactHoppity) {
            val rabbit = pendingHoppityRabbit
            if (rabbit != null) {
                pendingHoppityRabbit = null
                val raw = pendingHoppityRaw
                pendingHoppityRaw = null

                val duplicate = hoppityDuplicateRegex.find(message)
                if (duplicate != null) {
                    event.cancel()
                    val chocolate = abbreviateNumber(duplicate.groupValues[1])
                    modMessage("§d§lHOPPITY! §f$rabbit §7Duplicate §8· §6$chocolate §7Chocolate", prefix = "")
                    return
                }

                raw?.let { modMessage(it, prefix = "") }
            }

            hoppityFindRegex.find(message)?.let { match ->
                event.cancel()
                pendingHoppityRabbit = match.groupValues[1]
                pendingHoppityRaw = event.message
                return
            }
        }

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

        if (compactTorrhus) {
            hiveRegex.find(message)?.let { match ->
                val (item1, count1, item2, count2) = match.destructured
                event.cancel()
                val countPrefix1 = if (count1.isEmpty()) "" else "§7${count1}x "
                val countPrefix2 = if (count2.isEmpty()) "" else "§7${count2}x "
                modMessage("§e§lHIVE! $countPrefix1§a$item1§7, $countPrefix2§a$item2", prefix = "")
                return
            }
        }

        if (compactAutopet) {
            autopetRegex.find(message)?.let { match ->
                val (level, pet, count) = match.destructured
                event.cancel()
                val countSuffix = if (count.isEmpty()) "" else " §7x$count"
                modMessage("§b§lPET! §f$pet §7Lvl $level$countSuffix", prefix = "")
                return
            }
        }

        if (compactStash) {
            stashRegex.find(message.trim())?.let { match ->
                val (count, kind) = match.destructured
                event.cancel()
                val stashType = if (kind.startsWith("material")) "material" else "item"
                modClickableMessage(
                    "§6§lSTASH! §f${count}x $kind §7— click to view",
                    "/viewstash $stashType",
                    prefix = ""
                )
                return
            }
        }
    }
}
