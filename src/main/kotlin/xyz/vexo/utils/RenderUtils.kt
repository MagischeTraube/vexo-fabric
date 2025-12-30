package xyz.vexo.utils

import net.minecraft.client.gui.render.state.GuiTextRenderState
import net.minecraft.network.chat.Component
import org.joml.Matrix3x2f
import net.minecraft.client.gui.GuiGraphics
import xyz.vexo.Vexo.mc

/**
 * Renders a string with transformation support
 * @param context The GUI graphics context
 * @param text The text to render
 * @param x The x position
 * @param y The y position
 * @param scale The scale factor
 * @param color The default text color
 */
fun renderString(
    context: GuiGraphics,
    text: String,
    x: Float,
    y: Float,
    scale: Float,
    color: Int = 0xFFFFFFFF.toInt(),
shadow: Boolean = true
) {
    context.pose().pushMatrix()

    context.pose().translate(x, y)
    context.pose().scale(scale, scale)

    val mat = Matrix3x2f(context.pose())

    val comp = Component.literal(text)

    context.guiRenderState.submitText(
        GuiTextRenderState(
            mc.font,
            comp.visualOrderText,
            mat,
            0,
            0,
            color,
            0,
            shadow,
            context.scissorStack.peek()
        )
    )

    context.pose().popMatrix()
}

