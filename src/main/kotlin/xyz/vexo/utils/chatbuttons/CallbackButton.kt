package xyz.vexo.utils.chatbuttons

import java.net.URI
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

/**
 * A button that executes a callback when clicked.
 *
 * @param text The text to display on the button.
 * @param hoverText The text to display when hovering over the button.
 * @param buttonColor The color of the button. Can be any [ChatFormatting] value.
 * @param callback The callback to execute when the button is clicked.
 */
class CallbackButton(
    override val text: String,
    override val hoverText: String? = null,
    override val buttonColor: ChatFormatting = ChatFormatting.AQUA,
    val callback: () -> Unit
) : ChatButton() {

    override fun toComponent(groupId: String): Component {
        val id = ActionRegistry.register(callback, groupId)

        val style = Style.EMPTY
            .withColor(buttonColor)
            .withClickEvent(ClickEvent.OpenUrl(URI.create("vexo://$id")))
            .withHoverEvent(
                HoverEvent.ShowText(Component.literal(hoverText ?: text))
            )

        return Component.literal("[$text]").setStyle(style)
    }
}
