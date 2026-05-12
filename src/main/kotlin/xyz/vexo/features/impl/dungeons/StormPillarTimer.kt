package xyz.vexo.features.impl.dungeons

import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.StringSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.features.Module

object StormPillarTimer : Module(
    name = "Storm Pillar Timer",
    description = "Shows a title when got crushed",
    toggled = false
) {
    private val labelSetting = StringSetting(
        name = "HUD Text",
        default = "§cStorm Crushed! "
    )

    private val label by labelSetting

    private val hud by HudSetting(
        name = "Move HUD",
        defaultText = label
    )

    init {
        labelSetting.onChange = {
            hud.text = it
        }
    }

    private var ticks = 0

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (event.unformattedMessage != "[BOSS] Storm: Oof" &&
            event.unformattedMessage != "[BOSS] Storm: Ouch, that hurt!") return
        ticks = 20
        hud.visible = true
    }

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        if (ticks <= 0) return
        ticks--
        if (ticks <= 0) hud.visible = false
    }
}
