package xyz.vexo.events.impl

import net.minecraft.network.protocol.Packet
import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a packet is received from the server.
 *
 * @param packet The received packet
 */
class PacketReceiveEvent(
    val packet: Packet<*>
) : CancellableEvent()