package xyz.vexo.events.impl

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import xyz.vexo.events.Event

class WorldRenderEvent(val context: WorldRenderContext) : Event()
