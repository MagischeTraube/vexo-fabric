package xyz.vexo.events.impl

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import xyz.vexo.events.Event

/**
 * Event fired when world is being rendered
 *
 * @param context The context of the world render
 */
class WorldRenderEvent(val context: WorldRenderContext) : Event()
