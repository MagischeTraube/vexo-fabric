package xyz.vexo.features.impl.misc

import java.awt.Color
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.features.Module

object LavaAsWater : Module(
    name = "Lava As Water",
    description = "Renders lava as water textures for better visibility in Kuudra",
    toggled = false
) {
    val colorMode = SelectorSetting(
        "Color Mode",
        "Keep the water tint or apply a custom color",
        default = "Water",
        options = listOf("Water", "Color")
    )

    val customColor = ColorSetting(
        "Color",
        "Custom tint and transparency applied to the lava",
        default = Color(0, 120, 255, 200),
        allowAlpha = true
    ).dependsOn { colorMode.getCurrentValue() == "Color" }

    /** Registers the lava render handler. Called once during client init. */
    fun registerFluidHandler() {
        val originalLavaHandler = FluidRenderHandlerRegistry.INSTANCE.get(Fluids.LAVA)
        val handler = object : FluidRenderHandler {
            override fun getFluidSprites(view: BlockAndTintGetter?, pos: BlockPos?, state: FluidState) =
                if (enabled)
                    FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER)
                        ?.getFluidSprites(view, pos, Fluids.WATER.defaultFluidState())
                        ?: originalLavaHandler?.getFluidSprites(view, pos, state)
                        ?: emptyArray()
                else
                    originalLavaHandler?.getFluidSprites(view, pos, state) ?: emptyArray()

            override fun getFluidColor(view: BlockAndTintGetter?, pos: BlockPos?, state: FluidState) =
                if (enabled)
                    if (colorMode.getCurrentValue() == "Color") customColor.getCurrentValue().rgb
                    else FluidRenderHandlerRegistry.INSTANCE.get(Fluids.WATER)
                        ?.getFluidColor(view, pos, Fluids.WATER.defaultFluidState()) ?: -1
                else
                    originalLavaHandler?.getFluidColor(view, pos, state) ?: -1
        }
        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.LAVA, handler)
        FluidRenderHandlerRegistry.INSTANCE.register(Fluids.FLOWING_LAVA, handler)
    }
}