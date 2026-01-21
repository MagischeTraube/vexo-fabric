package xyz.vexo.mixin;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.vexo.events.impl.ParticleSpawnEvent;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {
    @Inject(
            method = "add",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onParticleSpawn(Particle particle, CallbackInfo ci) {
        var event = new ParticleSpawnEvent(particle);
        event.postAndCatch();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}

