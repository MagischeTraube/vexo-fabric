package xyz.vexo.events.impl

import xyz.vexo.events.Event
import com.mojang.blaze3d.platform.InputConstants

class KeybindReleaseEvent(
    val key: InputConstants.Key
) : Event()
