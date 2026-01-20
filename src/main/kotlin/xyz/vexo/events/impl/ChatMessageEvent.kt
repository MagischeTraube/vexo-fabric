package xyz.vexo.events.impl

import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a chat message is processed by the client.
 *
 * @param message The message that was processed
 * @param unformattedMessage The message that was processed without formatting
 */
class ChatMessageEvent(
    val message: String,
    val unformattedMessage: String
) : CancellableEvent()