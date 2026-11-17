package xyz.vexo.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.ScrollWheelHandler;
import org.joml.Vector2i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import xyz.vexo.features.impl.chat.ChatScroll;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {

    @WrapOperation(
            method = "onScroll",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/ScrollWheelHandler;onMouseScroll(DD)Lorg/joml/Vector2i;")
    )
    private Vector2i vexo$scrollChat(ScrollWheelHandler handler, double x, double y, Operation<Vector2i> original) {
        Vector2i steps = original.call(handler, x, y);
        return ChatScroll.onWorldScroll(steps.y) ? new Vector2i(0, 0) : steps;
    }
}
