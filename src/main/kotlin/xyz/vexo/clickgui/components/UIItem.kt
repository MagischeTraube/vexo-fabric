package xyz.vexo.clickgui.components

import gg.essential.elementa.UIComponent
import gg.essential.elementa.dsl.constrain
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import xyz.vexo.clickgui.gpx
import xyz.vexo.utils.logError
import kotlin.math.roundToInt

class UIItem @JvmOverloads constructor(
    var itemStack: ItemStack,
    var seed: Int = 0
) : UIComponent() {

    init {
        constrain {
            width = 16.gpx()
            height = 16.gpx()
        }
    }

    override fun draw(matrixStack: UMatrixStack) {
        beforeDrawCompat(matrixStack)

        if (!itemStack.isEmpty) {
            UIItemRenderer.enqueue(
                UIItemRenderer.Entry(
                    itemStack = itemStack,
                    x = getLeft().roundToInt(),
                    y = getTop().roundToInt(),
                    seed = seed,
                    clip = captureClip()
                )
            )
        }

        super.draw(matrixStack)
    }

    private fun captureClip(): IntArray? {
        val state = ScissorEffect.currentScissorState ?: return null
        val scale = UResolution.scaleFactor
        val viewportHeight = UResolution.viewportHeight

        val left = (state.x / scale).roundToInt()
        val right = ((state.x + state.width) / scale).roundToInt()
        val top = ((viewportHeight - state.y - state.height) / scale).roundToInt()
        val bottom = ((viewportHeight - state.y) / scale).roundToInt()

        return intArrayOf(left, top, right, bottom)
    }
}

object UIItemRenderer {
    class Entry(
        val itemStack: ItemStack,
        val x: Int,
        val y: Int,
        val seed: Int,
        val clip: IntArray?
    )

    private val queue = mutableListOf<Entry>()

    internal fun enqueue(entry: Entry) {
        queue.add(entry)
    }

    fun renderQueued(graphics: GuiGraphicsExtractor) {
        if (queue.isEmpty()) return

        try {
            queue.forEach { entry ->
                try {
                    val clip = entry.clip
                    if (clip != null) {
                        if (clip[2] <= clip[0] || clip[3] <= clip[1]) return@forEach
                        graphics.enableScissor(clip[0], clip[1], clip[2], clip[3])
                    }

                    graphics.item(entry.itemStack, entry.x, entry.y, entry.seed)

                    if (clip != null) {
                        graphics.disableScissor()
                    }
                } catch (e: Exception) {
                    logError(e, "Failed to render UIItem for ${entry.itemStack}")
                }
            }
        } finally {
            queue.clear()
        }
    }
}
