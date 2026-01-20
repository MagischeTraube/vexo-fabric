package xyz.vexo.events

import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.resources.ResourceLocation
import net.minecraft.resources.ResourceLocation.fromNamespaceAndPath
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import xyz.vexo.Vexo
import xyz.vexo.utils.IInitializable
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.events.impl.PacketReceiveEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.events.impl.WorldRenderDataReadyEvent
import xyz.vexo.events.impl.WorldRenderEvent
import xyz.vexo.events.impl.ChatMessageEvent
import xyz.vexo.events.impl.ServerConnectEvent
import xyz.vexo.events.impl.ServerLeaveEvent
import xyz.vexo.events.impl.ParticleReceiveEvent
import xyz.vexo.utils.removeFormatting

object EventDispatcher : IInitializable {
    private val HUD_LAYER: ResourceLocation = fromNamespaceAndPath(Vexo.MOD_ID, "vexo_hud")

    var onServer = false
        private set

    override fun init() {
        EventBus.subscribe(this)

        ClientTickEvents.END_CLIENT_TICK.register { client ->
            ClientTickEvent.postAndCatch()
        }

        HudElementRegistry.attachElementBefore(
            VanillaHudElements.SLEEP,
            HUD_LAYER
        ) { context, _ ->
            HudRenderEvent(context).postAndCatch()
        }

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            WorldJoinEvent.postAndCatch()
            if (!onServer) {
                onServer = true
                ServerConnectEvent.postAndCatch()
            }
        }

        ClientPlayConnectionEvents.DISCONNECT.register { _, _ ->
            onServer = false
            ServerLeaveEvent.postAndCatch()
        }

        WorldRenderEvents.END_EXTRACTION.register {
            WorldRenderDataReadyEvent.postAndCatch()
        }

        WorldRenderEvents.END_MAIN.register { context ->
            WorldRenderEvent.postAndCatch()
        }

        ClientReceiveMessageEvents.ALLOW_GAME.register { text, overlay ->
            if (overlay) return@register true

            val event = ChatMessageEvent(text.string, text.string.removeFormatting())
            event.postAndCatch()
            !event.isCancelled()
        }

    }

    @EventHandler
    fun onPacket(event: PacketReceiveEvent) {
        when(val packet = event.packet) {
            is ClientboundPingPacket -> {
                ServerTickEvent.postAndCatch()
            }

            is ClientboundSystemChatPacket -> {
                ChatMessagePacketEvent(
                    packet.content?.string ?: "",
                    packet.content?.string?.removeFormatting() ?: ""
                ).postAndCatch()
            }


            is ClientboundLevelParticlesPacket -> {
                val particleEvent = ParticleReceiveEvent(packet)
                particleEvent.postAndCatch()
                if (particleEvent.isCancelled()) {
                    event.cancel()
                }
            }
        }
    }
}