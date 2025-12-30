package xyz.vexo.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.vexo.events.impl.InputEvent;

@Mixin(KeyMapping.class)
public class KeyMappingMixin {

    @Inject(
            method = "click",
            at = @At("HEAD")
    )
    private static void onKeyPressed(InputConstants.Key key, CallbackInfo ci) {
        new InputEvent(key).postAndCatch();
    }
}