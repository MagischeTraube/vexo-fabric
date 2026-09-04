package xyz.vexo.mixin;

import xyz.vexo.events.impl.GuiRenderEvent;
import xyz.vexo.events.impl.SlotGuiRenderEvent;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {
    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void vexo$onDrawSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        var event = new SlotGuiRenderEvent((Screen) (Object) this, graphics, slot);
        event.postAndCatch();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "extractTooltip", at = @At("HEAD"))
    private void vexo$onExtractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY, CallbackInfo ci) {
        new GuiRenderEvent((Screen) (Object) this, graphics, mouseX, mouseY).postAndCatch();
    }
}