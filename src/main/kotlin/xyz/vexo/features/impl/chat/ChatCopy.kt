package xyz.vexo.features.impl.chat

import com.google.common.collect.MapMaker
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import xyz.vexo.Vexo.mc
import xyz.vexo.clickgui.components.ClickGuiColor
import xyz.vexo.clickgui.theme.Theme
import java.awt.Color
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.features.Module
import xyz.vexo.mixin.ChatComponentAccessor
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Optional
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min

/** Right-click menu to copy chat messages. */
object ChatCopy : Module(
    name = "Chat Copy",
    description = "Right-click a chat message to copy it, with or without time and color codes",
    toggled = false
) {
    private val timestampFormat by SelectorSetting(
        name = "Timestamp Format",
        description = "Format of the time when copying with timestamp",
        default = "HH:mm:ss",
        options = listOf("HH:mm:ss", "HH:mm", "dd.MM.yyyy HH:mm:ss", "hh:mm:ss a")
    )

    private val copyFeedback by BooleanSetting(
        name = "Copy Feedback",
        description = "Show a chat message after copying",
        default = true
    )


    private val timestamps: MutableMap<GuiMessage, Long> = MapMaker().weakKeys().makeMap()

    @JvmStatic
    fun recordTime(message: GuiMessage, time: Long = System.currentTimeMillis()) {
        timestamps.putIfAbsent(message, time)
    }

    @JvmStatic
    fun timeOf(message: GuiMessage): Long? = timestamps[message]


    private class Option(val label: String, val withTime: Boolean, val formatted: Boolean)

    private val options = listOf(
        Option("Copy", withTime = false, formatted = false),
        Option("Copy (Formatted)", withTime = false, formatted = true),
        Option("Copy with Time", withTime = true, formatted = false),
    )

    private const val ROW_HEIGHT = 14
    private const val PADDING = 6
    private const val ACCENT_BAR_WIDTH = 2
    private const val BACKGROUND_ALPHA = 191

    private var target: GuiMessage? = null
    private var menuX = 0
    private var menuY = 0

    private val menuWidth get() = options.maxOf { mc.font.width(it.label) } + PADDING * 2 + ACCENT_BAR_WIDTH
    private val menuHeight get() = options.size * ROW_HEIGHT

    @JvmStatic
    fun onMouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (!enabled) return false

        val open = target
        if (open != null) {
            if (button == InputConstants.MOUSE_BUTTON_LEFT) optionAt(mouseX, mouseY)?.let { copy(open, it) }
            close()
            if (button != InputConstants.MOUSE_BUTTON_RIGHT) return true
        }

        if (button != InputConstants.MOUSE_BUTTON_RIGHT) return false
        val message = messageAt(mouseX, mouseY) ?: return false

        target = message
        val screenW = mc.window.guiScaledWidth
        val screenH = mc.window.guiScaledHeight
        menuX = min(mouseX.toInt(), screenW - menuWidth - 1).coerceAtLeast(0)
        menuY = min(mouseY.toInt(), screenH - menuHeight - 1).coerceAtLeast(0)
        return true
    }

    @JvmStatic
    fun onKeyPressed(key: Int): Boolean {
        if (target == null || key != InputConstants.KEY_ESCAPE) return false
        close()
        return true
    }

    @JvmStatic
    fun close() {
        target = null
    }

    @JvmStatic
    fun render(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int) {
        val message = target
        if (!enabled || message == null) return

        graphics.nextStratum()
        outlineMessage(graphics, message)

        val w = menuWidth
        val h = menuHeight
        val border = argb(ClickGuiColor.SEPARATOR_COLOR, BACKGROUND_ALPHA)
        graphics.fill(menuX, menuY - 1, menuX + w + 1, menuY, border)
        graphics.fill(menuX, menuY + h, menuX + w + 1, menuY + h + 1, border)
        graphics.fill(menuX + w, menuY, menuX + w + 1, menuY + h, border)
        graphics.fill(menuX + ACCENT_BAR_WIDTH, menuY, menuX + w, menuY + h, argb(ClickGuiColor.CARD_BG, BACKGROUND_ALPHA))
        graphics.fill(menuX, menuY - 1, menuX + ACCENT_BAR_WIDTH, menuY + h + 1, argb(Theme.accent()))

        val hovered = optionAt(mouseX.toDouble(), mouseY.toDouble())
        options.forEachIndexed { i, option ->
            val y = menuY + i * ROW_HEIGHT
            if (option === hovered) {
                graphics.fill(menuX + ACCENT_BAR_WIDTH, y, menuX + w, y + ROW_HEIGHT, argb(ClickGuiColor.CARD_HOVER, BACKGROUND_ALPHA))
            }
            val color = if (option === hovered) ClickGuiColor.WHITE_TEXT_COLOR else ClickGuiColor.GRAY_TEXT_COLOR.brighter()
            graphics.text(mc.font, option.label, menuX + ACCENT_BAR_WIDTH + PADDING, y + (ROW_HEIGHT - 8) / 2, argb(color))
        }
    }

    private fun argb(color: Color, alpha: Int = 0xFF): Int = (alpha shl 24) or (color.rgb and 0xFFFFFF)

    private fun optionAt(mouseX: Double, mouseY: Double): Option? {
        if (mouseX < menuX || mouseX >= menuX + menuWidth || mouseY < menuY) return null
        return options.getOrNull(((mouseY - menuY) / ROW_HEIGHT).toInt())
    }

    private class ChatLayout(
        val scale: Double,
        val lineHeight: Int,
        val chatWidth: Double,
        val bottom: Double,
        val lines: List<GuiMessage.Line>,
        val scroll: Int,
        val visible: Int,
    ) {
        fun line(index: Int): GuiMessage.Line = lines[index + scroll]
    }

    private fun layout(): ChatLayout? {
        val chat = mc.gui.hud.chat
        val accessor = chat as ChatComponentAccessor
        val scale = accessor.vexoGetScale()
        val lineHeight = accessor.vexoGetLineHeight()
        if (scale <= 0.0 || lineHeight <= 0) return null

        val lines = accessor.vexoTrimmedMessages()
        val scroll = accessor.vexoScrollPos()
        return ChatLayout(
            scale = scale,
            lineHeight = lineHeight,
            chatWidth = ceil(accessor.vexoGetWidth() / scale),
            bottom = floor((mc.window.guiScaledHeight - 40) / scale),
            lines = lines,
            scroll = scroll,
            visible = min(lines.size - scroll, chat.linesPerPage),
        )
    }

    private fun messageAt(mouseX: Double, mouseY: Double): GuiMessage? {
        val layout = layout() ?: return null

        val localX = mouseX / layout.scale - 4
        if (localX < -4 || localX > layout.chatWidth + 8) return null

        val index = floor((layout.bottom - mouseY / layout.scale) / layout.lineHeight).toInt()
        if (index < 0 || index >= layout.visible) return null

        return layout.line(index).parent()
    }

    private fun outlineMessage(graphics: GuiGraphicsExtractor, message: GuiMessage) {
        val layout = layout() ?: return
        val indices = (0 until layout.visible).filter { layout.line(it).parent() === message }
        if (indices.isEmpty()) return

        val s = layout.scale
        val x1 = 0
        val x2 = ceil((layout.chatWidth + 12) * s).toInt()
        val y1 = floor((layout.bottom - (indices.max() + 1) * layout.lineHeight) * s).toInt()
        val y2 = ceil((layout.bottom - indices.min() * layout.lineHeight) * s).toInt() + 1

        val white = 0xFFFFFFFF.toInt()
        graphics.fill(x1, y1, x2, y1 + 1, white)
        graphics.fill(x1, y2 - 1, x2, y2, white)
        graphics.fill(x1, y1, x1 + 1, y2, white)
        graphics.fill(x2 - 1, y1, x2, y2, white)
    }

    private fun copy(message: GuiMessage, option: Option) {
        val text = if (option.formatted) toLegacy(message.content()) else message.content().string.removeFormatting()
        val result = if (option.withTime) {
            val time = timeOf(message)
            val stamp = time?.let {
                runCatching {
                    DateTimeFormatter.ofPattern(timestampFormat).format(Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()))
                }.getOrNull()
            } ?: "??:??:??"
            "[$stamp] $text"
        } else text

        mc.keyboardHandler.clipboard = result
        if (copyFeedback) modMessage("§aCopied message to clipboard")
    }


    private val legacyColors: Map<Int, ChatFormatting> = ChatFormatting.entries
        .mapNotNull { f -> TextColor.fromLegacyFormat(f)?.let { it.value to f } }
        .toMap()

    private fun toLegacy(component: Component): String {
        val out = StringBuilder()
        var last: String? = null
        component.visit(FormattedText.StyledContentConsumer<Unit> { style, text ->
            if (text.isNotEmpty()) {
                val codes = codesFor(style)
                if (codes != last) {
                    out.append(codes)
                    last = codes
                }
                out.append(text)
            }
            Optional.empty()
        }, Style.EMPTY)
        return out.toString().removePrefix("§r")
    }

    private fun codesFor(style: Style): String = buildString {
        append("§r")
        style.color?.let { color -> legacyColors[color.value]?.let { append(it.toString()) } }
        if (style.isObfuscated) append(ChatFormatting.OBFUSCATED)
        if (style.isBold) append(ChatFormatting.BOLD)
        if (style.isStrikethrough) append(ChatFormatting.STRIKETHROUGH)
        if (style.isUnderlined) append(ChatFormatting.UNDERLINE)
        if (style.isItalic) append(ChatFormatting.ITALIC)
    }
}
