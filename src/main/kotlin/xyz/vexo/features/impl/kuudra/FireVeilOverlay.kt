package xyz.vexo.features.impl.kuudra

import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.util.Mth
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.Vec3
import xyz.vexo.Vexo
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.events.impl.PacketSendEvent
import xyz.vexo.events.impl.WorldRenderEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.drawCircle
import xyz.vexo.utils.removeFormatting
import java.awt.Color
import kotlin.math.roundToInt
import xyz.vexo.utils.PlayerUtils.isFirstPerson

/**
 * Draws a ring around the player for the duration of the Fire Veil Wand's ability and
 * tints the wand's hotbar slot green while the veil is active.
 */
object FireVeilOverlay : Module(
    name = "Fire Veil Overlay",
    description = "Shows the Fire Veil radius as a ring and marks the wand in the hotbar while active",
    toggled = false
) {
    private val onlyThirdPerson by BooleanSetting(
        name = "Only Third Person",
        description = "Won't show the overlay while in First Person",
        default = false,
    )
    private val thickness by SliderSetting(
        "Line Thickness",
        "Thickness of the Radius indicator",
        default = 3.0, min = 1.0, max = 10.0, increment = 0.5
    )
    private val ringColor by ColorSetting(
        name = "Ring Color",
        default = Color(255, 85, 0, 255)
    )

    private val segmentAmount = (thickness*72).roundToInt()
    private const val VEIL_TICKS = 100 // 5s
    private const val COOLDOWN_TICKS = 20 // 1s ability cooldown

    private var ticksRemaining = 0
    private var cooldownTicks = 0
    private var activeSlot = -1

    override fun onDisable() {
        super.onDisable()
        reset()
    }

    @EventHandler
    fun onPacket(event: PacketSendEvent) {
        val packet = event.packet
        if (packet !is ServerboundUseItemPacket || packet.hand != InteractionHand.MAIN_HAND) return
        if (cooldownTicks > 0) return

        val player = Vexo.mc.player ?: return
        val held = player.mainHandItem
        if (held.isEmpty) return
        if ("fire veil wand" !in held.hoverName.string.removeFormatting().lowercase()) return

        cooldownTicks = COOLDOWN_TICKS
        ticksRemaining = VEIL_TICKS
        activeSlot = player.inventory.selectedSlot
    }

    @EventHandler
    fun onTick(event: ClientTickEvent) {
        if (Vexo.mc.player == null || Vexo.mc.level == null)  return reset()

        if (cooldownTicks > 0) cooldownTicks--
        if (ticksRemaining > 0 && --ticksRemaining <= 0) activeSlot = -1
    }

    @EventHandler
    fun onWorldRender(event: WorldRenderEvent) {
        if (isFirstPerson() && onlyThirdPerson) return
        if (ticksRemaining <= 0) return
        val player = Vexo.mc.player ?: return

        val partial = Vexo.mc.deltaTracker.getGameTimeDeltaPartialTick(true)
        val center = Vec3(
            Mth.lerp(partial.toDouble(), player.xo, player.x),
            Mth.lerp(partial.toDouble(), player.yo, player.y),
            Mth.lerp(partial.toDouble(), player.zo, player.z)
        )

        event.context.drawCircle(center, 3.5, ringColor, ringColor.alpha, width = thickness, segments = segmentAmount)
    }

    @EventHandler
    fun onHudRender(event: HudRenderEvent) {
        if (ticksRemaining <= 0 || activeSlot !in 0..8) return

        // Hotbar slots are 20px apart, the item icon sits 3px inside the 22px slot frame.
        val x = Vexo.mc.window.guiScaledWidth / 2 - 91 + 3 + activeSlot * 20
        val y = Vexo.mc.window.guiScaledHeight - 16 - 3

        event.context.fill(x, y, x + 16, y + 16, 0x8055FF55.toInt())
    }

    private fun reset() {
        ticksRemaining = 0
        cooldownTicks = 0
        activeSlot = -1
    }
}