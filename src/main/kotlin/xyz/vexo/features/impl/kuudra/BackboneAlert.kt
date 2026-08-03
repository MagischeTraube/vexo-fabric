package xyz.vexo.features.impl.kuudra

import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.features.Module
import xyz.vexo.hud.MoveActiveHudsGui
import xyz.vexo.utils.BonemerangRendTracker


object BackboneAlert : Module(
    name = "Backbone Alert",
    description = "Shows a HUD bar timing the Bonemerang backbone and alerts you to Rend",
    toggled = false
) {
    private val hud by HudSetting(
        name = "Move HUD",
        defaultText = "§a████████████ §b60%"
    )

    private val playSound by BooleanSetting(
        "Sound",
        "Play a sound when the alert fires",
        default = true
    )

    val advanceTicks by SliderSetting(
        "Advance (Ticks)",
        "Trigger the Rend this many ticks earlier (recommended 2)",
        default = 2.0, min = 0.0, max = 10.0, increment = 1.0
    )

    val tickAdjustment by SliderSetting(
        "Tick Adjustment",
        "Fine-tune the timing: shift the Rend later (+) or earlier (-)",
        default = 0.0, min = -10.0, max = 10.0, increment = 1.0
    )

    private val rendHud by HudSetting(
        name = "Move REND HUD",
        defaultText = "§l§aREND NOW",
        defaultScale = REND_TEXT_SCALE
    )

    private val rendColor by SelectorSetting(
        "REND Farbe",
        "Farbe der großen REND NOW Anzeige",
        default = "Grün",
        options = listOf("Grün", "Rot")
    )

    private var wasRendActive = false

    @EventHandler
    fun onTick(@Suppress("UNUSED_PARAMETER") event: ClientTickEvent) {
        val isRendActive = BonemerangRendTracker.isRendActive()
        if (isRendActive && !wasRendActive && playSound) {
            mc.player?.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 2.0f, 1.0f)
        }
        wasRendActive = isRendActive
    }

    @EventHandler
    fun onRender(event: HudRenderEvent) {
        if (mc.screen is MoveActiveHudsGui || !BonemerangRendTracker.isTracking()) return

        val ctx = event.context
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(hud.x.toFloat(), hud.y.toFloat())
        pose.scale(hud.scale, hud.scale)

        // The bar only shows progress now; the "REND NOW" alert lives in the separate overlay below.
        renderProgressBar(ctx)

        pose.popMatrix()

        // Separate, larger overlay that screams "REND NOW" the moment the window opens.
        if (BonemerangRendTracker.isRendActive()) renderRendOverlay(ctx)
    }

    private fun renderRendOverlay(ctx: net.minecraft.client.gui.GuiGraphicsExtractor) {
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(rendHud.x.toFloat(), rendHud.y.toFloat())
        // Scale is baked into the HUD's default scale (REND_TEXT_SCALE) so the move-GUI preview matches.
        pose.scale(rendHud.scale, rendHud.scale)

        // Pulse the alpha so the big alert is impossible to miss.
        val pulse = (Math.sin(System.currentTimeMillis() / 90.0) * 0.5 + 0.5).toFloat()
        val alpha = (0xBB + (0x44 * pulse)).toInt() and 0xFF
        val rgb = if (rendColor == "Rot") 0xFF5555 else 0x55FF55
        val color = (alpha shl 24) or rgb

        val label = Component.literal("REND NOW").withStyle { it.withBold(true) }
        ctx.text(mc.font, label, 0, 0, color)

        pose.popMatrix()
    }

    private fun renderProgressBar(ctx: net.minecraft.client.gui.GuiGraphicsExtractor) {
        val percent = BonemerangRendTracker.progress()
        val filled = Math.round(percent * (BAR_WIDTH - 2))

        val barColor = when {
            percent > 0.85f -> 0xFF55FF55.toInt()
            percent > 0.6f -> 0xFFFFAA00.toInt()
            else -> 0xFFFF5555.toInt()
        }

        // Border + track.
        ctx.fill(-1, -1, BAR_WIDTH + 1, BAR_HEIGHT + 1, 0xFF000000.toInt())
        ctx.fill(0, 0, BAR_WIDTH, BAR_HEIGHT, 0xCC1C1C1C.toInt())

        // Filled portion with a lighter highlight line on top.
        if (filled > 0) {
            ctx.fill(1, 1, 1 + filled, BAR_HEIGHT - 1, barColor)
            ctx.fill(1, 1, 1 + filled, 2, (barColor and 0x00FFFFFF) or (0x66 shl 24))
        }

        // Percentage to the right of the bar.
        val text = Component.literal("${Math.round(percent * 100f)}%").withStyle { it.withColor(0x55FFFF) }
        ctx.text(mc.font, text, BAR_WIDTH + 4, (BAR_HEIGHT - mc.font.lineHeight) / 2 + 1, 0xFF55FFFF.toInt())
    }

    private const val BAR_WIDTH = 72
    private const val BAR_HEIGHT = 9
    private const val REND_TEXT_SCALE = 3.0f
}
