package xyz.vexo.features.impl.misc.screenshots.util

import com.mojang.blaze3d.platform.NativeImage
import xyz.vexo.features.impl.misc.screenshots.RectDrawAction
import kotlin.math.max
import kotlin.math.min

object ScreenshotProcessor {
    /**
     * Draws rectangles on the given image.
     *
     * @param image The image to draw on.
     * @param rects The list of rectangles to draw.
     * @param guiScale The GUI scale factor.
     */
    fun drawRectangles(image: NativeImage, rects: List<RectDrawAction>, guiScale: Double) {
        for (rect in rects) {
            val xLeft   = (rect.bounds.left * guiScale).toInt().coerceIn(0, image.width - 1)
            val xRight  = (rect.bounds.right * guiScale).toInt().coerceIn(0, image.width - 1)
            val yTop    = (rect.bounds.top * guiScale).toInt().coerceIn(0, image.height - 1)
            val yBottom = (rect.bounds.bottom * guiScale).toInt().coerceIn(0, image.height - 1)

            val colorInt = (rect.color.alpha shl 24) or (rect.color.blue shl 16) or (rect.color.green shl 8) or rect.color.red
            val thickness = (2 * guiScale).coerceAtLeast(2.0)

            for (i in 0 until thickness.toInt()) {
                if (yTop + i < image.height)  for (x in xLeft..xRight) image.setPixelABGR(x, yTop + i, colorInt)
                if (yBottom - i >= 0)             for (x in xLeft..xRight) image.setPixelABGR(x, yBottom - i, colorInt)
                if (xLeft + i < image.width)  for (y in yTop..yBottom) image.setPixelABGR(xLeft + i, y, colorInt)
                if (xRight - i >= 0)              for (y in yTop..yBottom) image.setPixelABGR(xRight - i, y, colorInt)
            }
        }
    }

    /**
     * Crops the given image to the specified selection bounds.
     *
     * @param original The original image to crop from.
     * @param currentSelection The current selection area in screen coordinates.
     * @param guiScale The GUI scale factor used to convert screen coordinates to image coordinates.
     * @return A new NativeImage containing only the cropped region.
     */
    fun cropToSelection(original: NativeImage, currentSelection: SelectionBounds, guiScale: Double): NativeImage {
        val screenWidth  = (original.width / guiScale)
        val screenHeight = (original.height / guiScale)

        val clamped = currentSelection.clampedTo(screenWidth - 1, screenHeight - 1)

        val startX = (clamped.left   * guiScale).toInt()
        val startY = (clamped.top    * guiScale).toInt()
        val stopX  = min(original.width - 1, startX + (clamped.width * guiScale).toInt())
        val stopY  = min(original.height - 1, startY + (clamped.height * guiScale).toInt())

        val cropWidth  = max(1, stopX - startX)
        val cropHeight = max(1, stopY - startY)

        val croppedImage = NativeImage(cropWidth, cropHeight, false)
        original.copyRect(croppedImage, startX, startY, 0, 0, cropWidth, cropHeight, false, false)
        return croppedImage
    }
}