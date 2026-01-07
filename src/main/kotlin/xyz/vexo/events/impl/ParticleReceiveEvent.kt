package xyz.vexo.events.impl

import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a particle packet is received from the server.
 * @param particlePacket The received packet
 */
class ParticleReceiveEvent(
    val particlePacket: ClientboundLevelParticlesPacket
) : CancellableEvent()