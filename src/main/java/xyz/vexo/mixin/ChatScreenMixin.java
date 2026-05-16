package xyz.vexo.mixin;

import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.vexo.events.impl.ChatMessageSendEvent;
import static xyz.vexo.utils.chatbuttons.ActionRegistry.runAction;

@Mixin(ChatScreen.class)
public class ChatScreenMixin {
    @Inject(method = "handleChatInput", at = @At("HEAD"), cancellable = true)
    private void onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        var event = new ChatMessageSendEvent(message);
        event.postAndCatch();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void handleClick(Style style, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (style == null || style.getClickEvent() == null) return;

        ClickEvent click = style.getClickEvent();

        if (click instanceof ClickEvent.OpenUrl(var url)) {

            if (url.toString().startsWith("vexo://")) {

                String id = url.toString().substring("vexo://".length());

                runAction(id);

                cir.setReturnValue(true);
            }
        }
    }
}
