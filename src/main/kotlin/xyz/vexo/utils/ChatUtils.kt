package xyz.vexo.utils

import net.minecraft.network.chat.Component
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
    mc.gui?.chat?.addMessage(text)
}