package xyz.vexo.events.impl

import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a chat message is about to be sent.
 */
class ChatMessageSendEvent(
    val message: String
) : CancellableEvent()