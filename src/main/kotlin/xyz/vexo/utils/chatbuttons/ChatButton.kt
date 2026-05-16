package xyz.vexo.utils.chatbuttons

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

/**
 * A button that can be clicked in the chat.
 */
sealed class ChatButton {

    abstract val text: String
    abstract val hoverText: String?

    open val buttonColor: ChatFormatting = ChatFormatting.AQUA

    abstract fun toComponent(groupId: String = ""): Component
}
