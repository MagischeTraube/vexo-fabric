package xyz.vexo.utils

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent

object DungeonUtils {

    var inDungeon = false
    var floor: String = ""

    @EventHandler
    fun onDungeonChat(event: ChatMessagePacketEvent) {
        val msg = event.message.removeFormatting()

        val enterDungeonMatch = Regex(""".* entered (MM )?The Catacombs, Floor ([IVX]+)!""").find(msg)
        if (enterDungeonMatch != null) {
            inDungeon = true

            val mmPrefix = enterDungeonMatch.groupValues[1]
            val roman = enterDungeonMatch.groupValues[2]

            val num = when (roman) {
                "I" -> "1"
                "II" -> "2"
                "III" -> "3"
                "IV" -> "4"
                "V" -> "5"
                "VI" -> "6"
                "VII" -> "7"
                else -> ""
            }

            floor = if (mmPrefix.isNotEmpty()) "M$num" else "F$num"
        }
    }


    fun getDungeonFloor(): String { return floor }
}