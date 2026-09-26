package xyz.vexo.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessageSource;
import net.minecraft.client.multiplayer.chat.GuiMessageTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.vexo.events.impl.ChatMessageEvent;
import xyz.vexo.utils.UtilsKt;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {

    @Inject(
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void vexo$onAddMessage(Component message, MessageSignature signature, GuiMessageSource source, GuiMessageTag tag, CallbackInfo ci) {
        if (source == GuiMessageSource.SYSTEM_CLIENT) return;

        String formatted = message.getString();
        ChatMessageEvent event = new ChatMessageEvent(formatted, UtilsKt.removeFormatting(formatted));
        event.postAndCatch();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }
}
