package xyz.vexo.features.impl.misc.screenshots.config

object ButtonConfig {
    const val WIDTH = 60
    const val HEIGHT = 16
    const val GAP = 4
    const val TOTAL_WIDTH = (WIDTH * 5) + (GAP * 4)
    const val Y_OFFSET_BELOW_SELECTION = 6
}

object HandleConfig {
    const val SIZE = 7
    const val HITBOX_RADIUS = 16.0
}

object ColorPickerConfig {
    const val SIZE = 10
    const val GAP = 4
    val TOTAL_WIDTH = (ScreenshotColors.drawColors.size * SIZE) +
            ((ScreenshotColors.drawColors.size - 1) * GAP)
    const val Y_OFFSET_BELOW_BUTTONS = 6
}