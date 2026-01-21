package xyz.vexo.features.impl.dungeons

import net.minecraft.client.particle.ExplodeParticle
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ParticleSpawnEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.removeFormatting

object ParticleHider : Module (
    name = "Particle Hider",
    description = "Hides various particles"
) {
    private val p5Hider by BooleanSetting("Hide in p5")
    private val p4Hider by BooleanSetting("Hide in p4")
    private val iceSpray by BooleanSetting("Hide Ice Spray")

    var inP5 = false
    var inP4 = false

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (event.unformattedMessage == "[BOSS] Necron: Let's make some space!") inP5 = true
        if (event.unformattedMessage matches Regex("\\[BOSS] Necron: You went further than any human before, congratulations\\.")) inP4 = true
    }

    @EventHandler
    fun onParticle(event: ParticleSpawnEvent){
        if (inP5 && p5Hider) { event.cancel()
            inP4 = false
        }
        if (inP4 && p4Hider) { event.cancel() }
        if (event.particle is ExplodeParticle && iceSpray) event.cancel()

    }

    @EventHandler()
    fun onWorldJoin (event: WorldJoinEvent){
        inP5 = false
        inP4 = false
    }

}