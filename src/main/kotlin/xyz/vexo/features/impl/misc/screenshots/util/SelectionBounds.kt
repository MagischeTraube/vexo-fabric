package xyz.vexo.features.impl.misc.screenshots.util

import kotlin.math.max
import kotlin.math.min

data class SelectionBounds(
    val left: Double = 0.0,
    val right: Double = 0.0,
    val top: Double = 0.0,
    val bottom: Double = 0.0
) {
    val width get() = right - left
    val height get() = bottom - top
    val hasArea get() = width != 0.0 && height != 0.0
    val isLargeEnough get() = width >= 5 && height >= 5

    /**
     * Clamps the selection bounds to the screen dimensions.
     *
     * @param screenWidth The width of the screen.
     * @param screenHeight The height of the screen.
     * @return The clamped selection bounds.
     */
    fun clampedTo(screenWidth: Double, screenHeight: Double) = SelectionBounds(
        left   = left.coerceIn(0.0, screenWidth),
        right  = right.coerceIn(0.0, screenWidth),
        top    = top.coerceIn(0.0, screenHeight),
        bottom = bottom.coerceIn(0.0, screenHeight)
    )

    /**
     * Normalizes the selection bounds.
     *
     * @return The normalized selection bounds.
     */
    fun normalized() = SelectionBounds(
        left   = min(left, right),
        right  = max(left, right),
        top    = min(top, bottom),
        bottom = max(top, bottom)
    )
}