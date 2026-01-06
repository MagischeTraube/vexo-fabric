package xyz.vexo.utils

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent


val DungeonEnterMessage = listOf(
    Regex("entered MM The Catacombs, Floor"),
    Regex("entered The Catacombs, Floor")
)
var inDungeon = false

@EventHandler
fun onChat(event: ChatMessagePacketEvent) {
    if (DungeonEnterMessage.any { it.containsMatchIn(event.message) }) {
        inDungeon = true
    }
}

var floor: String = ""
object GetDungeonFloorHelper{
    @EventHandler
        fun getDungeonHelper(event: ChatMessagePacketEvent){
            val msg = event.message.removeFormatting()
            when {
                Regex(""".* entered The Catacombs, Floor I!""").matches(msg) -> floor = "F1"
                Regex(""".* entered The Catacombs, Floor II!""").matches(msg) -> floor = "F2"
                Regex(""".* entered The Catacombs, Floor III!""").matches(msg) -> floor = "F3"
                Regex(""".* entered The Catacombs, Floor IV!""").matches(msg) -> floor = "F4"
                Regex(""".* entered The Catacombs, Floor V!""").matches(msg) -> floor = "F5"
                Regex(""".* entered The Catacombs, Floor VI!""").matches(msg) -> floor = "F6"
                Regex(""".* entered The Catacombs, Floor VII!""").matches(msg) -> floor = "F7"

                Regex(""".* entered MM The Catacombs, Floor I!""").matches(msg) -> floor = "M1"
                Regex(""".* entered MM The Catacombs, Floor II!""").matches(msg) -> floor = "M2"
                Regex(""".* entered MM The Catacombs, Floor III!""").matches(msg) -> floor = "M3"
                Regex(""".* entered MM The Catacombs, Floor IV!""").matches(msg) -> floor = "M4"
                Regex(""".* entered MM The Catacombs, Floor V!""").matches(msg) -> floor = "M5"
                Regex(""".* entered MM The Catacombs, Floor VI!""").matches(msg) -> floor = "M6"
                Regex(""".* entered MM The Catacombs, Floor VII!""").matches(msg) -> floor = "M7"
            }
        }
}
fun getDungeonFloor(): String { return floor }