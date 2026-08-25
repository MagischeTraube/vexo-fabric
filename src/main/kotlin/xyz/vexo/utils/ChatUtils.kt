package xyz.vexo.utils

import java.util.UUID
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import xyz.vexo.utils.chatbuttons.ChatButton
import xyz.vexo.Vexo.mc

/**
 * Sends a command to the server.
 *
 * @param command The command to send.
 */
fun sendCommand(command: String) {
    mc.player?.connection?.sendCommand(command)
}

/**
 * Sends a message to the chat.
 *
 * @param message The message to send.
 * @param prefix The prefix to add to the message.
 */
fun modMessage(message: Any?, prefix: String = "§b[Vexo]§r ") {
    val text = Component.literal("$prefix$message")
    mc.execute { mc.gui.chat.addClientSystemMessage(text) }
}

/**
 * Sends a message to the chat that runs a client command when the whole line is clicked.
 *
 * @param message The message to send.
 * @param command The command to run when the message is clicked (including the leading "/").
 * @param prefix The prefix to add to the message.
 */
fun modClickableMessage(message: String, command: String, prefix: String = "§b[Vexo]§r ") {
    val style = Style.EMPTY.withClickEvent(ClickEvent.RunCommand(command))
    val text = Component.literal("$prefix$message").setStyle(style)
    mc.execute { mc.gui.chat.addClientSystemMessage(text) }
}

/**
 * Sends a message to the chat with buttons.
 *
 * @param message The message to send.
 * @param buttons The buttons to display.
 * @param prefix The prefix to add to the message. Defaults to "§b[Vexo]§r ".
 */
fun modButtonsMessage(
    message: String,
    buttons: List<ChatButton>,
    prefix: String = "§b[Vexo]§r "
) {
    val groupId = UUID.randomUUID().toString()
    val messageText = Component.literal("$prefix$message ")

    buttons.forEachIndexed { i, button ->
        messageText.append(button.toComponent(groupId))

        if (i != buttons.lastIndex) {
            messageText.append(Component.literal(" "))
        }
    }

    mc.execute { mc.gui.chat.addClientSystemMessage(messageText) }
}
