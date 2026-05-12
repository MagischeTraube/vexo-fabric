package xyz.vexo.utils

import com.mojang.blaze3d.vertex.PoseStack
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
import net.minecraft.client.gui.render.state.GuiTextRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
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
            true,
            context.scissorStack.peek()
        )
    )

    context.pose().popMatrix()
}

/**
 * Draws an outlined 3D box using the world render context
 *
 * @param box The bounding box to draw
 * @param rgb The RGB color
 * @param alpha The alpha value (0-255) (default: 255)
 */
fun WorldRenderContext.drawBoxOutline(box: AABB, rgb: Int, alpha: Int = 255) {
    val consumers = consumers()
    val cam = mc.gameRenderer.mainCamera.position()

    val matrices = PoseStack()
    matrices.translate(-cam.x, -cam.y, -cam.z)
    val pose = matrices.last()

    val buf = consumers.getBuffer(RenderTypes.lines())

    val r = (rgb shr 16) and 0xFF
    val g = (rgb shr 8) and 0xFF
    val b = rgb and 0xFF

    val x0 = box.minX.toFloat(); val y0 = box.minY.toFloat(); val z0 = box.minZ.toFloat()
    val x1 = box.maxX.toFloat(); val y1 = box.maxY.toFloat(); val z1 = box.maxZ.toFloat()

    // bottom face
    buf.addVertex(pose, x0, y0, z0).setColor(r, g, b, alpha).setNormal(1f, 0f, 0f)
    buf.addVertex(pose, x1, y0, z0).setColor(r, g, b, alpha).setNormal(1f, 0f, 0f)
    buf.addVertex(pose, x1, y0, z0).setColor(r, g, b, alpha).setNormal(0f, 0f, 1f)
    buf.addVertex(pose, x1, y0, z1).setColor(r, g, b, alpha).setNormal(0f, 0f, 1f)
    buf.addVertex(pose, x1, y0, z1).setColor(r, g, b, alpha).setNormal(-1f, 0f, 0f)
    buf.addVertex(pose, x0, y0, z1).setColor(r, g, b, alpha).setNormal(-1f, 0f, 0f)
    buf.addVertex(pose, x0, y0, z1).setColor(r, g, b, alpha).setNormal(0f, 0f, -1f)
    buf.addVertex(pose, x0, y0, z0).setColor(r, g, b, alpha).setNormal(0f, 0f, -1f)

    // top face
    buf.addVertex(pose, x0, y1, z0).setColor(r, g, b, alpha).setNormal(1f, 0f, 0f)
    buf.addVertex(pose, x1, y1, z0).setColor(r, g, b, alpha).setNormal(1f, 0f, 0f)
    buf.addVertex(pose, x1, y1, z0).setColor(r, g, b, alpha).setNormal(0f, 0f, 1f)
    buf.addVertex(pose, x1, y1, z1).setColor(r, g, b, alpha).setNormal(0f, 0f, 1f)
    buf.addVertex(pose, x1, y1, z1).setColor(r, g, b, alpha).setNormal(-1f, 0f, 0f)
    buf.addVertex(pose, x0, y1, z1).setColor(r, g, b, alpha).setNormal(-1f, 0f, 0f)
    buf.addVertex(pose, x0, y1, z1).setColor(r, g, b, alpha).setNormal(0f, 0f, -1f)
    buf.addVertex(pose, x0, y1, z0).setColor(r, g, b, alpha).setNormal(0f, 0f, -1f)

    // vertical edges
    buf.addVertex(pose, x0, y0, z0).setColor(r, g, b, alpha).setNormal(0f, 1f, 0f)
    buf.addVertex(pose, x0, y1, z0).setColor(r, g, b, alpha).setNormal(0f, 1f, 0f)
    buf.addVertex(pose, x1, y0, z0).setColor(r, g, b, alpha).setNormal(0f, 1f, 0f)
    buf.addVertex(pose, x1, y1, z0).setColor(r, g, b, alpha).setNormal(0f, 1f, 0f)
    buf.addVertex(pose, x1, y0, z1).setColor(r, g, b, alpha).setNormal(0f, 1f, 0f)
    buf.addVertex(pose, x1, y1, z1).setColor(r, g, b, alpha).setNormal(0f, 1f, 0f)
    buf.addVertex(pose, x0, y0, z1).setColor(r, g, b, alpha).setNormal(0f, 1f, 0f)
    buf.addVertex(pose, x0, y1, z1).setColor(r, g, b, alpha).setNormal(0f, 1f, 0f)
}
