package xyz.vexo.features.impl.dungeons

import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.StringSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.getAllPlayerCoords
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.runAfterServerTicks
import xyz.vexo.utils.writeInFile

object HealerP5LeapAlert : Module(
    name = "Healer P5 Leap Alert",
    description = "Alerts the Healer when all Players have Leaped to p5 in M7"
) {
    private val LeapAlert by StringSetting(
        name = "HUD Text",
        default = "§c§fAll Leaped!"
    )

    private val healerAlert by HudSetting(
        name = "Move HUD",
        defaultText = LeapAlert
    )

    var inP5 = false
    var notifiedAll = false

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        val cleanMessage = event.message.removeFormatting()
        if (cleanMessage == "[BOSS] Necron: Let's make some space!") inP5 = true
    }

    var ticks = 80
    @EventHandler
    fun onTick(event: ServerTickEvent) {

        if (notifiedAll || !inP5) return

        if (getAllPlayerCoords().values.all { it.y < 20.0 }) {
            healerAlert.visible = true
            notifiedAll = true
            runAfterServerTicks(40) { healerAlert.visible = false }
        }
    }


    @EventHandler
    fun worldJoin(event: WorldJoinEvent) {
        inP5 = false
        notifiedAll = false
    }
}

