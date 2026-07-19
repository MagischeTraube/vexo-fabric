package xyz.vexo.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Gives Kotlin features read access to a container screen's protected position fields, so they can
 * find out where a slot sits on screen and draw overlays on top of it (e.g. the Kuudra Profit Tracker).
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {
    @Accessor("leftPos")
    int vexoLeftPos();

    @Accessor("topPos")
    int vexoTopPos();

    @Accessor("imageWidth")
    int vexoImageWidth();

    @Accessor("hoveredSlot")
    @Nullable
    Slot vexoHoveredSlot();
}
