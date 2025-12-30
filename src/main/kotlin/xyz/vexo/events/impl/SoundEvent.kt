package xyz.vexo.events.impl

import net.minecraft.client.resources.sounds.SoundInstance
import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a sound is played.
 * Can be cancelled to prevent the sound from playing.
 */
class SoundEvent(
    val sound: SoundInstance
) : CancellableEvent()