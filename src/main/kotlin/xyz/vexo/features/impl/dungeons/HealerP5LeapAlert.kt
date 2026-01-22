package xyz.vexo.features.impl.dungeons

import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.StringSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.getAllPlayerCoords
import xyz.vexo.utils.runAfterServerTicks

object HealerP5LeapAlert : Module(
    name = "Healer P5 Leap Alert",
    description = "Alerts the Healer when all Players have Leaped to p5 in M7"
) {
    private val leapAlertTextSetting = StringSetting(
        name = "HUD Text",
        default = "§c§fAll Leaped!"
    )

    private val leapAlertText by leapAlertTextSetting

    private val healerAlertHud by HudSetting(
        name = "Move HUD",
        defaultText = leapAlertText
    )

    init {
        leapAlertTextSetting.onChange = {
            healerAlertHud.text = it
        }
    }

    var inP5 = false
    var notifiedAll = false

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (event.unformattedMessage == "[BOSS] Necron: Let's make some space!") { inP5 = true }
    }

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        if (notifiedAll || !inP5) return

        if (getAllPlayerCoords().values.all { it.y < 20.0 }) {
            healerAlertHud.showForXServerTicks(40)
            notifiedAll = true

        }
    }

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        inP5 = false
        notifiedAll = false
    }
}

