package xyz.vexo.events.impl

import net.minecraft.client.particle.Particle
import xyz.vexo.events.CancellableEvent

/**
 * Event fired when a Particle has spawned
 * Can be cancelled to prevent the Particles from spawning.
 *
 * @param particle The Particle which spawned
 */
class ParticleSpawnEvent (
    val particle : Particle
) : CancellableEvent()