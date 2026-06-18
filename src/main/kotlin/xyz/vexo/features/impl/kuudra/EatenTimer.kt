package xyz.vexo.features.impl.kuudra

import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.features.Module
import xyz.vexo.hud.MoveActiveHudsGui

object EatenTimer : Module(
    name = "Eeaten Timer",
    description = "Displays a countdown when you get eaten by Kuudra",
    toggled = false
) {
    private val hud by HudSetting(name = "Move HUD", defaultText = "96")

    private var countdown = 0
    private var graceTicks = 0
    private var timerActive = false
    private var wasRiding = false

    @EventHandler
    fun onTick(event: ClientTickEvent) {
        val player = mc.player ?: return reset()

        val riding = player.vehicle?.type?.toString()?.contains("armor_stand") == true

        if (riding && !wasRiding && !timerActive) {
            countdown = 96
            graceTicks = 39
            timerActive = true
        }

        if (graceTicks > 0) {
            graceTicks--
            if (!riding) return reset()
        }

        wasRiding = riding

        if (timerActive && countdown > 0) {
            countdown--
            if (countdown == 0) timerActive = false
        }
    }

    @EventHandler
    fun onRender(event: HudRenderEvent) {
        if (mc.screen is MoveActiveHudsGui || !timerActive || countdown <= 0) return

        val label = countdown.toString()
        val color = if (countdown <= 20) 0xFFFF5555.toInt() else 0xFFFFFFFF.toInt()
        val scale = hud.scale * 3f

        val pose = event.context.pose()
        pose.pushMatrix()
        pose.translate(hud.x.toFloat(), hud.y.toFloat())
        pose.scale(scale, scale)
        event.context.text(mc.font, label, 0, 0, color)
        pose.popMatrix()
    }

    private fun reset() {
        countdown = 0
        graceTicks = 0
        timerActive = false
        wasRiding = false
    }
}
