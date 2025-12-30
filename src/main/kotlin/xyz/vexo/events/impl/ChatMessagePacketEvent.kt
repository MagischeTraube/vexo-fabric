package xyz.vexo.events.impl

import xyz.vexo.events.Event

/**
 * Event fired when a chat message packet is received from the server.
 */
class ChatMessagePacketEvent(
    val message: String
) : Event()