package xyz.vexo.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.vexo.events.impl.ContainerSlotClickEvent;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Inject(
            method = "handleContainerInput",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vexo$onContainerInput(int containerId, int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
        if (ContainerSlotClickEvent.getBypass()) {
            return;
        }
        var event = new ContainerSlotClickEvent(containerId, slotId, button, input, player);
        event.postAndCatch();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
