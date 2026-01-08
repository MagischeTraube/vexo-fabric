package xyz.vexo.mixin;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.vexo.events.impl.GuiRenderEvent;
import xyz.vexo.events.impl.TooltipEvent;


@Mixin(AbstractContainerScreen.class)
public class AbstractContainerScreenMixin {

    @Inject(method = "render", at = @At("TAIL"))
    protected void onRender(GuiGraphics context, int mouseX, int mouseY, float deltaTicks, CallbackInfo ci) {
        new GuiRenderEvent((Screen) (Object) this, context, mouseX, mouseY).postAndCatch();
    }

    @Inject(method = "getTooltipFromContainerItem", at = @At("RETURN"), cancellable = true)
    private void modifyTooltip(ItemStack itemStack, CallbackInfoReturnable<List<Component>> cir) {
        List<Component> lines = new ArrayList<>(cir.getReturnValue());

        TooltipEvent event = new TooltipEvent(
                (Screen) (Object) this,
                itemStack,
                lines
        );

        event.postAndCatch();

        cir.setReturnValue(event.lines);
    }
}