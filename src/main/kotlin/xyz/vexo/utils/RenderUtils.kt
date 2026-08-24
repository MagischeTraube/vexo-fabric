package xyz.vexo.utils

import java.awt.Color
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.core.BlockPos
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.renderer.state.gui.GuiTextRenderState
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3x2f
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.Slot
import xyz.vexo.Vexo.mc
import xyz.vexo.mixin.AbstractContainerScreenAccessor

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
    context: GuiGraphicsExtractor,
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

    context.guiRenderState.addText(
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
 * Draws translucent tints over highlighted slots in a GUI.
 *
 * @param context the GUI graphics context
 * @param screen the GUI whose slots are rendered
 * @param highlights mapping of slot to RGBA tint color
 */
fun renderSlotHighlights(
    context: GuiGraphicsExtractor,
    screen: AbstractContainerScreen<*>,
    highlights: Map<Slot, Int>
) {
    val accessor = screen as AbstractContainerScreenAccessor
    val leftPos = accessor.vexoLeftPos()
    val topPos = accessor.vexoTopPos()

    for ((slot, color) in highlights) {
        val x = leftPos + slot.x
        val y = topPos + slot.y
        context.fill(x, y, x + 16, y + 16, color)
    }
}

/**
 * Draws a straight line between two points in GUI (screen) space.
 *
 * @param context The GUI graphics context
 * @param x1 Start X
 * @param y1 Start Y
 * @param x2 End X
 * @param y2 End Y
 * @param color The line color (ARGB, e.g. from [xyz.vexo.config.impl.ColorSetting.getRGBA])
 * @param width The line thickness in pixels
 */
fun drawLine2D(
    context: GuiGraphicsExtractor,
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Int,
    width: Float
) {
    val dx = x2 - x1
    val dy = y2 - y1
    val length = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    if (length <= 0f) return

    val angle = Math.atan2(dy.toDouble(), dx.toDouble()).toFloat()
    val half = (width / 2f).coerceAtLeast(0.5f)

    context.pose().pushMatrix()
    context.pose().translate(x1, y1)
    context.pose().rotate(angle)

    context.fill(0, -half.toInt(), length.toInt(), half.toInt().coerceAtLeast(1), color)

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
    val minX = minOf(fromBlock.x, toBlock.x).toDouble()
    val minY = minOf(fromBlock.y, toBlock.y).toDouble()
    val minZ = minOf(fromBlock.z, toBlock.z).toDouble()

    val maxX = maxOf(fromBlock.x, toBlock.x) + 1.0
    val maxY = maxOf(fromBlock.y, toBlock.y) + 1.0
    val maxZ = maxOf(fromBlock.z, toBlock.z) + 1.0

    return AABB(
        minX,
        minY,
        minZ,
        maxX,
        maxY,
        maxZ
    )
}

/**
 * Draws an outlined 3D box using the world render context
 *
 * @param box The bounding box to draw
 * @param color The color to use for the outline
 * @param alpha The alpha value (0-255) (default: 255)
 */
fun LevelRenderContext.drawBoxOutline(
    box: AABB,
    color: Color,
    alpha: Int = 255
) {
    val expanded = box.inflate(0.002)

    val buffer = bufferSource()

    val cam = mc.gameRenderer.mainCamera.position()

    val matrices = PoseStack()
    matrices.translate(-cam.x, -cam.y, -cam.z)

    val pose = matrices.last()

    val buf = buffer.getBuffer(RenderTypes.lines())

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

    buffer.endBatch(RenderTypes.lines())
}

/**
 * Draws a horizontal circle outline (flat on the XZ plane)
 *
 * @param center The center of the circle in world space
 * @param radius The radius in blocks
 * @param color The color of the outline
 * @param alpha The alpha value (0-255) (default: 255)
 * @param segments How many line segments make up the circle
 * @param width The line width
 */
fun LevelRenderContext.drawCircle(
    center: Vec3,
    radius: Double,
    color: Color,
    alpha: Int = 255,
    segments: Int = 72,
    width: Double = 1.0
) {
    // The level renderer's own buffer source: its batch is drawn while the camera transform is
    // still active, so vertices must be camera-relative.
    val buffer = bufferSource()

    val cam = mc.gameRenderer.mainCamera.position()

    val pose = PoseStack().last()

    val buf = buffer.getBuffer(RenderTypes.lines())

    val r = color.red
    val g = color.green
    val b = color.blue

    val cx = center.x - cam.x
    val cz = center.z - cam.z
    val y = (center.y - cam.y).toFloat()
    val step = (Math.PI * 2.0) / segments

    for (i in 0 until segments) {
        val a0 = step * i
        val a1 = step * (i + 1)

        val x0 = (cx + Math.cos(a0) * radius).toFloat()
        val z0 = (cz + Math.sin(a0) * radius).toFloat()
        val x1 = (cx + Math.cos(a1) * radius).toFloat()
        val z1 = (cz + Math.sin(a1) * radius).toFloat()

        // Normal points along the segment so the line shader keeps a constant screen-space width.
        val nx = x1 - x0
        val nz = z1 - z0
        val len = Math.sqrt((nx * nx + nz * nz).toDouble()).toFloat().coerceAtLeast(1e-5f)

        val width = (width*2).toFloat()

        buf.lineVertex(pose, x0, y, z0, r, g, b, alpha, nx / len, 0f, nz / len, width)
        buf.lineVertex(pose, x1, y, z1, r, g, b, alpha, nx / len, 0f, nz / len, width)
    }

    buffer.endBatch(RenderTypes.lines())
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
internal fun VertexConsumer.lineVertex(
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