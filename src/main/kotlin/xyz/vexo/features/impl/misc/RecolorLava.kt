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

    private var cachedIsColor = false
    private var cachedColor = 0
    private var cachedWater: FluidModel? = null
    private var cachedLava: FluidModel? = null
    private var cachedModel: FluidModel? = null

    /** Rebuilds chunks so the recolor applies. */
    override fun onEnable() {
        refreshChunks()
    }

    /** Rebuilds chunks so lava reverts to vanilla. */
    override fun onDisable() {
        refreshChunks()
    }

    /** Forces a chunk-mesh rebuild so a changed setting takes effect. */
    private fun refreshChunks() {
        runCatching { mc.levelRenderer.allChanged() }
    }

    /**
     * Builds the fluid model lava renders with, cached until the settings or source models change.
     *
     * @param waterModel vanilla water model, used for sprites/tint in "Water" mode
     * @param lavaModel vanilla lava model, used for its render layer
     * @return the fluid model to render lava with
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

    /**
     * Fetches the bundled grayscale lava sprites so a custom tint shows evenly.
     *
     * @return the still and flow sprites, or null if the block atlas isn't ready
     */
    private fun grayLavaMaterials(): Pair<Material.Baked, Material.Baked>? = runCatching {
        val atlasManager = (mc.modelManager as ModelManagerAccessor).atlasManager
        val atlas = atlasManager.getAtlasOrThrow(AtlasIds.BLOCKS)
        val still = Material.Baked(atlas.getSprite(GRAY_STILL), false)
        val flow = Material.Baked(atlas.getSprite(GRAY_FLOW), false)
        still to flow
    }.getOrNull()

    private val GRAY_STILL = Identifier.fromNamespaceAndPath("vexo", "block/lava_gray_still")
    private val GRAY_FLOW = Identifier.fromNamespaceAndPath("vexo", "block/lava_gray_flow")

    /**
     * A [BlockTintSource] that returns the same color everywhere, ignoring position-dependent shading.
     *
     * @property rgb the packed color returned for every query
     */
    private class ConstantTint(private val rgb: Int) : BlockTintSource {
        override fun color(state: BlockState) = rgb
        override fun colorInWorld(state: BlockState, view: BlockAndTintGetter, pos: BlockPos) = rgb
        override fun colorAsTerrainParticle(state: BlockState, view: BlockAndTintGetter, pos: BlockPos) = rgb
    }
}
