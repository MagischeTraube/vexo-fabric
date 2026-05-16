package xyz.vexo.utils.chatbuttons

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

/**
 * A button that executes a command when clicked.
 *
 * @param text The text to display on the button.
 * @param command The command to execute when the button is clicked.
 * @param hoverText The text to display when hovering over the button.
 * @param buttonColor The color of the button. Can be any [ChatFormatting] value.
 */
class CommandButton(
    override val text: String,
    val command: String,
    override val hoverText: String? = command,
    override val buttonColor: ChatFormatting = ChatFormatting.AQUA
) : ChatButton() {

    override fun toComponent(groupId: String): Component {
        val style = Style.EMPTY
            .withColor(buttonColor)
            .withClickEvent(ClickEvent.RunCommand(command))
            .withHoverEvent(
                HoverEvent.ShowText(Component.literal(hoverText ?: command))
            )

        return Component.literal("[$text]").setStyle(style)
    }
}
