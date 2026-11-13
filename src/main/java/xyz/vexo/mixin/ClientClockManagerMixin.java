package xyz.vexo.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.ClientClockManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.vexo.features.impl.misc.TimeChanger;

@Mixin(ClientClockManager.class)
public class ClientClockManagerMixin {

    @ModifyReturnValue(method = "getTotalTicks", at = @At("RETURN"))
    private long getTotalTicks(long original) {
        return TimeChanger.INSTANCE.getEnabled()
                ? TimeChanger.INSTANCE.getTicks()
                : original;
    }
}