package xyz.vexo.mixin;

import java.util.Map;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.block.FluidStateModelSet;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.vexo.features.impl.misc.RecolorLava;

/**
 * Drives the {@link RecolorLava} feature by swapping the {@link FluidModel} returned for lava while
 * the module is enabled. Reads the model map directly to avoid recursing back into {@code get}.
 */
@Mixin(FluidStateModelSet.class)
public abstract class FluidStateModelSetMixin {
	@Shadow @Final private Map<Fluid, FluidModel> modelByFluid;

	@Inject(method = "get", at = @At("HEAD"), cancellable = true)
	private void vexo$recolorLava(FluidState state, CallbackInfoReturnable<FluidModel> cir) {
		if (!RecolorLava.INSTANCE.getEnabled()) return;

		Fluid type = state.getType();
		if (type != Fluids.LAVA && type != Fluids.FLOWING_LAVA) return;

		FluidModel water = modelByFluid.get(Fluids.WATER);
		FluidModel lava = modelByFluid.get(Fluids.LAVA);
		if (water == null || lava == null) return;

		cir.setReturnValue(RecolorLava.INSTANCE.modelForLava(water, lava));
	}
}
