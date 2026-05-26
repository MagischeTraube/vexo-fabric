package xyz.vexo.events.impl

import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a chat message is processed by the client.
 * Can be cancelled to prevent the messages from appearing in chat.
 *
 * @param message The message that was processed
 * @param unformattedMessage The message that was processed without formatting
 */
class ChatMessageEvent(
    val message: String,
    val unformattedMessage: String
) : CancellableEvent()