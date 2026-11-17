package xyz.vexo.features.impl.chat

import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.multiplayer.chat.GuiMessage
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.KeybindSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.features.Module
import xyz.vexo.mixin.ChatComponentAccessor

object ChatScroll : Module(
    name = "Chat Scroll",
    description = "Scroll through chat without opening it and keep your scroll position when new messages arrive",
    toggled = false
) {
    private val scrollToOpen by BooleanSetting(
        name = "Scroll To Open",
        description = "Mouse wheel opens the chat without the input bar so you can keep moving",
        default = true
    )

    private val requireKey by BooleanSetting(
        name = "Require Keybind",
        description = "Only scroll the chat while holding a key, otherwise the wheel scrolls the hotbar",
        default = false
    ).apply { dependsOn { scrollToOpen } }

    private val scrollKey = KeybindSetting(
        name = "Scroll Keybind",
        description = "Hold this key while scrolling to scroll the chat"
    ).apply { dependsOn { scrollToOpen && requireKey } }

    private val autoClose by SliderSetting(
        name = "Auto Close",
        description = "Seconds without scrolling until the chat closes again",
        default = 5.0,
        min = 1.0,
        max = 30.0,
        increment = 1.0
    ).apply { dependsOn { scrollToOpen } }

    private val lockPosition by BooleanSetting(
        name = "Lock Scroll Position",
        description = "New messages don't move the chat while you're scrolled up",
        default = true
    )

    private const val LINES_PER_STEP = 7

    private var peeking = false
    private var lastScroll = 0L

    private val chat get() = mc.gui.hud.chat

    /** Returns true if the scroll was consumed by the chat. */
    @JvmStatic
    fun onWorldScroll(steps: Int): Boolean {
        if (!enabled || !scrollToOpen || mc.gui.screen() != null) return false
        if (requireKey && !scrollKeyDown()) return false
        if (steps == 0) return true

        val lines = if (mc.hasShiftDown()) steps else steps * LINES_PER_STEP
        if (!peeking) {
            // first scroll only opens the chat, so the newest messages are visible
            if (steps > 0) {
                peeking = true
                lastScroll = System.currentTimeMillis()
            }
            return true
        }

        chat.scrollChat(lines)
        lastScroll = System.currentTimeMillis()
        if ((chat as ChatComponentAccessor).vexoScrollPos() <= 0) close()
        return true
    }

    @JvmStatic
    fun isPeeking(): Boolean {
        if (!peeking) return false
        if (!enabled || !scrollToOpen || mc.gui.screen() != null ||
            System.currentTimeMillis() - lastScroll > autoClose.toLong() * 1000L
        ) {
            close()
            return false
        }
        return true
    }

    @JvmStatic
    fun displayMode(original: ChatComponent.DisplayMode): ChatComponent.DisplayMode =
        if (isPeeking()) ChatComponent.DisplayMode.FOREGROUND else original

    @JvmStatic
    fun shouldLockPosition(): Boolean = enabled && lockPosition

    /** Keeps the view in place when lines below the visible area get removed. */
    @JvmStatic
    fun removeLines(accessor: ChatComponentAccessor, message: GuiMessage) {
        val lines = accessor.vexoTrimmedMessages()
        val pos = accessor.vexoScrollPos()
        var below = 0
        for (i in 0 until minOf(pos, lines.size)) {
            if (lines[i].parent() === message) below++
        }
        lines.removeIf { it.parent() === message }
        if (below > 0 && shouldLockPosition()) accessor.vexoSetScrollPos(pos - below)
    }

    private fun scrollKeyDown(): Boolean {
        val key = scrollKey.getCurrentValue()
        return key != -1 && InputConstants.isKeyDown(key)
    }

    private fun close() {
        if (!peeking) return
        peeking = false
        if (mc.gui.screen() !is ChatScreen) chat.resetChatScroll()
    }
}
