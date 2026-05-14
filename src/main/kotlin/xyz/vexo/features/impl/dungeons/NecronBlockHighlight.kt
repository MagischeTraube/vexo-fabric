package xyz.vexo.features.impl.dungeons

import java.awt.Color
import net.minecraft.core.BlockPos

import xyz.vexo.features.Module
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.WorldRenderEvent
import xyz.vexo.utils.drawBoxOutline
import xyz.vexo.utils.blockBox
import xyz.vexo.config.impl.ColorSetting

object NecronBlockHighlight : Module(
    name = "Necron Block Highlight",
    description = "Highlights which blocks to mine"
) {
    private val waypointColor by ColorSetting(
        name = "Waypoint Color",
        default = Color(255, 0, 0, 255)
    )

    private val necronBlockArea = blockBox(
        BlockPos(53, 63, 113),
        BlockPos(55, 63, 115)
    )

    private val coreMessages = listOf(
        "[BOSS] Goldor: You have done it, you destroyed the factory…",
        "[BOSS] Goldor: But you have nowhere to hide anymore!",
        "[BOSS] Goldor: YOU ARE FACE TO FACE WITH GOLDOR!"
    )

    var inGoldorPhase = false

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        inGoldorPhase = false
    }

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (coreMessages.any { event.message.contains(it) }) {
            inGoldorPhase = true
        }

        if (event.unformattedMessage == "[BOSS] Necron: That's a very impressive trick. I guess I'll have to handle this myself.") {
            inGoldorPhase = false
        }
    }

    @EventHandler
    fun onWorldRender(event: WorldRenderEvent) {
        if (inGoldorPhase) {
            event.context.drawBoxOutline(necronBlockArea, waypointColor, waypointColor.alpha)
        }
    }
}