package xyz.vexo.events.impl

import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a chat message is processed by the client.
 */
class ChatMessageEvent(
    val message: String
) : CancellableEvent()