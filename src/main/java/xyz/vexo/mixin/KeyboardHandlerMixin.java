package xyz.vexo.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyboardHandler;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.vexo.config.impl.KeybindSetting;
import xyz.vexo.events.impl.KeybindEvent;

@Mixin(KeyboardHandler.class)
public class KeyboardHandlerMixin {

    @Inject(
            method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
            at = @At("HEAD")
    )
    private void vexo$onKeyPress(long windowPtr, int action,
                            net.minecraft.client.input.KeyEvent keyEvent,
                            CallbackInfo ci) {

        if (action != GLFW.GLFW_PRESS) {
            return;
        }

        if (KeybindSetting.isListeningForKeybinds()) {
            return;
        }

        InputConstants.Key key = InputConstants.Type.KEYSYM.getOrCreate(keyEvent.key());

        new KeybindEvent(key).postAndCatch();
    }
}
