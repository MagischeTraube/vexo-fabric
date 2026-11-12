package xyz.vexo.mixin;

import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.vexo.features.impl.chat.ChatStacking;
import xyz.vexo.features.impl.chat.ContinuousChatLog;

@Mixin(Hud.class)
public class HudMixin {

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void vexo$saveChatOnDisconnect(CallbackInfo ci) {
        ContinuousChatLog.INSTANCE.onDisconnect();
        ChatStacking.onDisconnect();
    }

    @Redirect(
            method = "onDisconnected",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;clearMessages(Z)V")
    )
    private void vexo$keepChatOnDisconnect(ChatComponent chat, boolean clearSentHistory) {
        if (ContinuousChatLog.INSTANCE.shouldKeepOnDisconnect()) return;
        chat.clearMessages(clearSentHistory);
    }
}
