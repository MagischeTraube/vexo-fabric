package xyz.vexo.events.impl

import com.mojang.blaze3d.platform.InputConstants.Key
import xyz.vexo.events.Event

/**
 * Event fired when a key is pressed.
 */
class InputEvent(val key: Key) : Event()