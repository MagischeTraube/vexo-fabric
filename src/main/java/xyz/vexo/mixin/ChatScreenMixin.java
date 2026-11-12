package xyz.vexo.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import xyz.vexo.features.impl.chat.ChatCopy;
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
    private void vexo$onSendMessage(String message, boolean addToHistory, CallbackInfo ci) {
        var event = new ChatMessageSendEvent(message);
        event.postAndCatch();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void vexo$onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ChatCopy.onMouseClicked(event.x(), event.y(), event.button())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void vexo$onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ChatCopy.onKeyPressed(event.key())) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void vexo$renderCopyMenu(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        ChatCopy.render(graphics, mouseX, mouseY);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void vexo$onRemoved(CallbackInfo ci) {
        ChatCopy.close();
    }

    @Inject(method = "handleComponentClicked", at = @At("HEAD"), cancellable = true)
    private void vexo$handleClick(Style style, boolean bl, CallbackInfoReturnable<Boolean> cir) {
        if (style == null) return;

        if (style.getClickEvent() == null) return;

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
