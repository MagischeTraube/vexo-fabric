package xyz.vexo.mixin;

import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.sprite.AtlasManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes {@link ModelManager}'s private {@code atlasManager} to fetch baked sprites at runtime. */
@Mixin(ModelManager.class)
public interface ModelManagerAccessor {
	@Accessor("atlasManager")
	AtlasManager getAtlasManager();
}
