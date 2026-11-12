package xyz.vexo.mixin;

import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("allMessages")
    List<GuiMessage> vexoAllMessages();

    @Accessor("trimmedMessages")
    List<GuiMessage.Line> vexoTrimmedMessages();

    @Accessor("chatScrollbarPos")
    int vexoScrollPos();

    @Invoker("refreshTrimmedMessages")
    void vexoRefreshTrimmedMessages();

    @Invoker("getScale")
    double vexoGetScale();

    @Invoker("getWidth")
    int vexoGetWidth();

    @Invoker("getLineHeight")
    int vexoGetLineHeight();
}
