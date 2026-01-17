package xyz.vexo.events.impl

import net.minecraft.network.protocol.Packet
import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a packet is sent to the server.
 *
 * @param packet The packet being sent
 */
class PacketSendEvent(
    val packet: Packet<*>
) : CancellableEvent()