package xyz.vexo.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the protected GUI offsets of a container screen so Kotlin features can translate
 * slot-relative coordinates ({@link net.minecraft.world.inventory.Slot#x}/{@code y}) into
 * absolute screen coordinates when drawing overlays (e.g. the Kuudra Profit Tracker).
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
