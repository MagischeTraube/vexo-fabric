package xyz.vexo.features.impl.misc

import java.awt.Color
import net.minecraft.client.color.block.BlockTintSource
import net.minecraft.client.renderer.block.BlockAndTintGetter
import net.minecraft.client.renderer.block.FluidModel
import net.minecraft.client.resources.model.sprite.Material
import net.minecraft.core.BlockPos
import net.minecraft.data.AtlasIds
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.state.BlockState
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.features.Module
import xyz.vexo.mixin.ModelManagerAccessor

/**
 * Recolors lava for better visibility in Kuudra: as opaque water, or as a custom color.
 *
 * The swap is driven by [xyz.vexo.mixin.FluidStateModelSetMixin], which asks this module for the
 * model lava should use. "Color" mode tints a bundled grayscale lava texture: the tint is
 * multiplicative (can only darken), so a light base is needed for any color to be reachable, white
 * included.
 */
object RecolorLava : Module(
    name = "Recolor Lava",
    description = "Renders lava as water textures for better visibility in Kuudra",
    toggled = false
) {
    val colorMode = SelectorSetting(
        "Color Mode",
        "Keep the water tint or apply a custom color",
        default = "Water",
        options = listOf("Water", "Color")
    ).apply { onChange = { refreshChunks() } }

    val customColor = ColorSetting(
        "Color",
        "Custom tint applied to the lava",
        default = Color(0, 120, 255, 255),
        allowAlpha = false
    ).dependsOn { colorMode.getCurrentValue() == "Color" }
        .apply { onChange = { refreshChunks() } }

    // Invalidated when the mode/color or the baked water/lava models change (the latter on reload).
    private var cachedIsColor = false
    private var cachedColor = 0
    private var cachedWater: FluidModel? = null
    private var cachedLava: FluidModel? = null
    private var cachedModel: FluidModel? = null

    override fun onEnable() {
        refreshChunks()
    }

    override fun onDisable() {
        refreshChunks()
    }

    // Force a chunk re-render so toggling/recoloring takes effect without waiting for a rebuild.
    private fun refreshChunks() {
        runCatching { mc.levelRenderer.allChanged() }
    }

    /**
     * Returns the model lava should render with. Both modes render in lava's opaque layer.
     * "Water" uses water's sprites + biome tint; "Color" tints the grayscale lava sprites with a
     * constant color (falling back to water sprites if the atlas lookup fails).
     */
    fun modelForLava(waterModel: FluidModel, lavaModel: FluidModel): FluidModel {
        val isColor = colorMode.getCurrentValue() == "Color"
        val rgb = customColor.getCurrentValue().rgb
        cachedModel?.let {
            if (cachedIsColor == isColor && cachedColor == rgb &&
                cachedWater === waterModel && cachedLava === lavaModel) return it
        }

        val gray = if (isColor) grayLavaMaterials() else null
        val model = if (isColor && gray != null) {
            FluidModel(lavaModel.layer(), gray.first, gray.second, null, ConstantTint(rgb))
        } else {
            val tint = if (isColor) ConstantTint(rgb) else waterModel.tintSource()
            FluidModel(
                lavaModel.layer(),
                waterModel.stillMaterial(),
                waterModel.flowingMaterial(),
                waterModel.overlayMaterial(),
                tint
            )
        }
        cachedIsColor = isColor
        cachedColor = rgb
        cachedWater = waterModel
        cachedLava = lavaModel
        cachedModel = model
        return model
    }

    /** Fetches the bundled grayscale lava sprites (still, flow), or null if the atlas isn't ready. */
    private fun grayLavaMaterials(): Pair<Material.Baked, Material.Baked>? = runCatching {
        val atlasManager = (mc.modelManager as ModelManagerAccessor).atlasManager
        val atlas = atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS)
        val still = Material.Baked(atlas.getSprite(GRAY_STILL), false)
        val flow = Material.Baked(atlas.getSprite(GRAY_FLOW), false)
        still to flow
    }.getOrNull()

    private val GRAY_STILL = Identifier.fromNamespaceAndPath("vexo", "block/lava_gray_still")
    private val GRAY_FLOW = Identifier.fromNamespaceAndPath("vexo", "block/lava_gray_flow")

    private class ConstantTint(private val rgb: Int) : BlockTintSource {
        override fun color(state: BlockState) = rgb
        override fun colorInWorld(state: BlockState, view: BlockAndTintGetter, pos: BlockPos) = rgb
        override fun colorAsTerrainParticle(state: BlockState, view: BlockAndTintGetter, pos: BlockPos) = rgb
    }
}
