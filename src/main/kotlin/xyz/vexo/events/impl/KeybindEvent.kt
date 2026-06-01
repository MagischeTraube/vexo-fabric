package xyz.vexo.events.impl

import xyz.vexo.events.Event
import com.mojang.blaze3d.platform.InputConstants

/**
 * Event fired when a keybind is pressed.
 *
 * @param key The key that was pressed
 */
class KeybindEvent(
    val key: InputConstants.Key
) : Event()
