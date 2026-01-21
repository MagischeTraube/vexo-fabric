package xyz.vexo.events.impl

import net.minecraft.client.particle.Particle
import xyz.vexo.events.CancellableEvent

/**
 * event fired
 */
class ParticleSpawnEvent (
    val particle : Particle
) : CancellableEvent()