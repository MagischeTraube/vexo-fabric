package xyz.vexo.utils.chatbuttons

import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import java.net.URI

/**
 * A button that opens a URL when clicked.
 *
 * @param text The text to display on the button.
 * @param url The URL to open when the button is clicked.
 * @param hoverText The text to display when hovering over the button.
 * @param buttonColor The color of the button. Can be any [ChatFormatting] value.
 */
class UrlButton(
    override val text: String,
    val url: String,
    override val hoverText: String? = url,
    override val buttonColor: ChatFormatting = ChatFormatting.AQUA
) : ChatButton() {

    override fun toComponent(groupId: String): Component {
        val style = Style.EMPTY
            .withColor(buttonColor)
            .withClickEvent(ClickEvent.OpenUrl(URI.create(url)))
            .withHoverEvent(
                HoverEvent.ShowText(Component.literal(hoverText ?: url))
            )

        return Component.literal("[$text]").setStyle(style)
    }
}
