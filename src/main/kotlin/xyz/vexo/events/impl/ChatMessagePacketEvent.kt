package xyz.vexo.events.impl

import xyz.vexo.events.Event

/**
 * Event fired when a chat message packet is received from the server.
 *
 * @param message The message that was received
 */
class ChatMessagePacketEvent(
    val message: String
) : Event()