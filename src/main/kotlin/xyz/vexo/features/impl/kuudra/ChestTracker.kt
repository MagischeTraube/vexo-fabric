package xyz.vexo.features.impl.kuudra

import net.minecraft.network.chat.Component
import xyz.vexo.Vexo.mc
import xyz.vexo.config.ConfigManager
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.features.Module
import xyz.vexo.hud.MoveActiveHudsGui
import xyz.vexo.utils.TablistUtils

object ChestTracker : Module(
    name = "Chest Tracker",
    description = "Tracks your Kuudra chest count for the 60-chest limit",
    toggled = false
) {
    private val hud by HudSetting(name = "HUD", defaultText = "0 (0 / 0)", defaultVisibility = true)

    private var total by SliderSetting("chest_total", default = 0.0, min = 0.0, max = 60.0, increment = 1.0)
        .apply { dependsOn { false } }
    private var success by SliderSetting("chest_success", default = 0.0, min = 0.0, max = 60.0, increment = 1.0)
        .apply { dependsOn { false } }
    private var fail by SliderSetting("chest_fail", default = 0.0, min = 0.0, max = 60.0, increment = 1.0)
        .apply { dependsOn { false } }

    private var lastInKuudraMs = 0L
    private var tickCount = 0

    private val chestTabPattern = Regex("""Chests:\s*(\d+)""", RegexOption.IGNORE_CASE)

    override fun onEnable() {
        super.onEnable()
        refreshHud()
    }

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        val msg = event.unformattedMessage
        when {
            "KUUDRA DOWN" in msg -> {
                success++
                total = minOf(total + 1.0, 60.0)
                persistAndRefresh()
            }
            "DEFEAT" in msg -> {
                fail++
                total = minOf(total + 1.0, 60.0)
                persistAndRefresh()
            }
            "PAID CHEST REWARDS" in msg -> handlePaidChest()
            "FREE CHEST REWARDS" in msg -> handleFreeChest()
        }
    }

    @EventHandler
    fun onTick(event: ClientTickEvent) {
        if (++tickCount % 20 != 0) return
        val tabEntry = TablistUtils.find(chestTabPattern) ?: return
        lastInKuudraMs = System.currentTimeMillis()
        val tabTotal = chestTabPattern.find(tabEntry)?.groupValues?.get(1)?.toDouble() ?: return
        if (tabTotal != total) syncFromTab(tabTotal)
    }

    @EventHandler
    fun onRender(event: HudRenderEvent) {
        if (mc.screen is MoveActiveHudsGui || !shouldShowHud()) return

        val t = total.toInt()
        val s = success.toInt()
        val f = fail.toInt()

        val text = Component.literal("$t ").withStyle { it.withColor(0xFFFFFF) }
        text.append(Component.literal("(").withStyle { it.withColor(0xAAAAAA) })
        text.append(Component.literal("$s").withStyle { it.withColor(0x55FF55) })
        text.append(Component.literal(" / ").withStyle { it.withColor(0xAAAAAA) })
        text.append(Component.literal("$f").withStyle { it.withColor(0xFF5555) })
        text.append(Component.literal(")").withStyle { it.withColor(0xAAAAAA) })

        val pose = event.context.pose()
        pose.pushMatrix()
        pose.translate(hud.x.toFloat(), hud.y.toFloat())
        pose.scale(hud.scale, hud.scale)
        event.context.drawString(mc.font, text, 0, 0, 0xFFFFFF)
        pose.popMatrix()
    }

    private fun handlePaidChest() {
        if (total <= 0.0) return
        total--
        if (success > 0.0) success--
        normalize()
        persistAndRefresh()
    }

    private fun handleFreeChest() {
        if (total <= 0.0) return
        total--
        if (fail > 0.0) fail-- else if (success > 0.0) success--
        normalize()
        persistAndRefresh()
    }

    private fun syncFromTab(tabTotal: Double) {
        if (tabTotal < total) {
            val diff = total - tabTotal
            total = tabTotal
            success = maxOf(0.0, success - diff)
        } else {
            total = tabTotal
        }
        normalize()
        persistAndRefresh()
    }

    private fun normalize() {
        if (total <= 0.0) {
            success = 0.0
            fail = 0.0
        }
    }

    private fun shouldShowHud(): Boolean {
        if (total <= 0.0) return false
        // Rely on the throttled onTick stamp instead of re-scanning the tablist every frame.
        // lastInKuudraMs is refreshed every ~1s while in Kuudra, so the 10-minute window covers
        // both "currently in Kuudra" and "recently left".
        return lastInKuudraMs > 0L && System.currentTimeMillis() - lastInKuudraMs < 600_000L
    }

    private fun persistAndRefresh() {
        refreshHud()
        ConfigManager.save()
    }

    private fun refreshHud() {
        hud.text = "${total.toInt()} (${success.toInt()} / ${fail.toInt()})"
    }

    fun reset() {
        total = 0.0
        success = 0.0
        fail = 0.0
        persistAndRefresh()
    }
}
