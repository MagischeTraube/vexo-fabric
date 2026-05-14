package xyz.vexo.utils

import java.awt.Color
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.BlockPos
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
 * Creates an AABB from two BlockPos coordinates
 *
 * @param fromBlock The starting block position
 * @param toBlock The ending block position
 * @return An AABB that encompasses the block area
 */
fun blockBox(fromBlock: BlockPos, toBlock: BlockPos): AABB {
    return AABB(
        fromBlock.x.toDouble(),
        fromBlock.y.toDouble(),
        fromBlock.z.toDouble(),
        toBlock.x + 1.0,
        toBlock.y + 1.0,
        toBlock.z + 1.0
    )
}

/**
 * Draws an outlined 3D box using the world render context
 *
 * @param box The bounding box to draw
 * @param color The color to use for the outline
 * @param alpha The alpha value (0-255) (default: 255)
 */
/**
 * Draws an outlined 3D box
 */
fun WorldRenderContext.drawBoxOutline(
    box: AABB,
    color: Color,
    alpha: Int = 255
) {
    val expanded = box.inflate(0.002)

    val consumers = consumers()
    val cam = mc.gameRenderer.mainCamera.position()

    val matrices = PoseStack()
    matrices.translate(-cam.x, -cam.y, -cam.z)

    val pose = matrices.last()

    val buf = consumers.getBuffer(RenderTypes.lines())

    val r = color.red
    val g = color.green
    val b = color.blue

    val x0 = expanded.minX.toFloat()
    val y0 = expanded.minY.toFloat()
    val z0 = expanded.minZ.toFloat()

    val x1 = expanded.maxX.toFloat()
    val y1 = expanded.maxY.toFloat()
    val z1 = expanded.maxZ.toFloat()

    // Bottom
    buf.lineVertex(pose, x0, y0, z0, r, g, b, alpha, 1f, 0f, 0f)
    buf.lineVertex(pose, x1, y0, z0, r, g, b, alpha, 1f, 0f, 0f)

    buf.lineVertex(pose, x1, y0, z0, r, g, b, alpha, 0f, 0f, 1f)
    buf.lineVertex(pose, x1, y0, z1, r, g, b, alpha, 0f, 0f, 1f)

    buf.lineVertex(pose, x1, y0, z1, r, g, b, alpha, -1f, 0f, 0f)
    buf.lineVertex(pose, x0, y0, z1, r, g, b, alpha, -1f, 0f, 0f)

    buf.lineVertex(pose, x0, y0, z1, r, g, b, alpha, 0f, 0f, -1f)
    buf.lineVertex(pose, x0, y0, z0, r, g, b, alpha, 0f, 0f, -1f)

    // Top
    buf.lineVertex(pose, x0, y1, z0, r, g, b, alpha, 1f, 0f, 0f)
    buf.lineVertex(pose, x1, y1, z0, r, g, b, alpha, 1f, 0f, 0f)

    buf.lineVertex(pose, x1, y1, z0, r, g, b, alpha, 0f, 0f, 1f)
    buf.lineVertex(pose, x1, y1, z1, r, g, b, alpha, 0f, 0f, 1f)

    buf.lineVertex(pose, x1, y1, z1, r, g, b, alpha, -1f, 0f, 0f)
    buf.lineVertex(pose, x0, y1, z1, r, g, b, alpha, -1f, 0f, 0f)

    buf.lineVertex(pose, x0, y1, z1, r, g, b, alpha, 0f, 0f, -1f)
    buf.lineVertex(pose, x0, y1, z0, r, g, b, alpha, 0f, 0f, -1f)

    // Verticals
    buf.lineVertex(pose, x0, y0, z0, r, g, b, alpha, 0f, 1f, 0f)
    buf.lineVertex(pose, x0, y1, z0, r, g, b, alpha, 0f, 1f, 0f)

    buf.lineVertex(pose, x1, y0, z0, r, g, b, alpha, 0f, 1f, 0f)
    buf.lineVertex(pose, x1, y1, z0, r, g, b, alpha, 0f, 1f, 0f)

    buf.lineVertex(pose, x1, y0, z1, r, g, b, alpha, 0f, 1f, 0f)
    buf.lineVertex(pose, x1, y1, z1, r, g, b, alpha, 0f, 1f, 0f)

    buf.lineVertex(pose, x0, y0, z1, r, g, b, alpha, 0f, 1f, 0f)
    buf.lineVertex(pose, x0, y1, z1, r, g, b, alpha, 0f, 1f, 0f)
}

/**
 * Draws a line vertex
 *
 * @param pose The pose stack pose
 * @param x The x position
 * @param y The y position
 * @param z The z position
 * @param r The red color value
 * @param g The green color value
 * @param b The blue color value
 * @param a The alpha value
 * @param nx The normal x value
 * @param ny The normal y value
 * @param nz The normal z value
 * @param width The line width
 */
private fun VertexConsumer.lineVertex(
    pose: PoseStack.Pose,
    x: Float,
    y: Float,
    z: Float,
    r: Int,
    g: Int,
    b: Int,
    a: Int,
    nx: Float,
    ny: Float,
    nz: Float,
    width: Float = 3f
) {
    addVertex(pose, x, y, z)
        .setColor(r, g, b, a)
        .setNormal(nx, ny, nz)
        .setLineWidth(width)
}