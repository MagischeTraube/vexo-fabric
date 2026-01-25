package xyz.vexo.hud

import xyz.vexo.utils.runAfterClientTicks
import xyz.vexo.utils.runAfterServerTicks

class HudElement(
    val name: String,
    var text: String,
    var x: Int = 5,
    var y: Int = 5,
    var scale: Float = 1f,
    initialVisible: Boolean = false
) {
    private var _visible: Boolean = initialVisible
    private var onVisibilityChanged: (() -> Unit)? = null

    var visible: Boolean
        get() = _visible
        set(value) {
            if (_visible != value) {
                _visible = value
                onVisibilityChanged?.invoke()
            }
        }

    internal fun setVisibilityCallback(callback: () -> Unit) {
        onVisibilityChanged = callback
    }

    /**
     * Shows the HudElement for a specified amount of ticks.
     *
     * @param ticks The amount of ticks to show the HudElement for.
     */
    fun showForXServerTicks(ticks: Int) {
        visible = true
        runAfterServerTicks(ticks) { visible = false }
    }

    /**
     * Shows the HudElement for a specified amount of ticks with a timer.
     * The timer will be updated every 2 ticks. (0.1 seconds)
     *
     * @param ticks The amount of ticks to show the HudElement for.
     */
    fun showForXServerTicksWithTimer(ticks: Int) {
        val originalText = text
        visible = true

        var ticksLeft = ticks

        fun updateTimer() {
            if (ticksLeft <= 0) {
                text = originalText
                visible = false
                return
            }

            val secondsLeft = ticksLeft / 20.0
            text = "$originalText ${String.format("%.1f", secondsLeft)}s"

            ticksLeft -= 2

            runAfterServerTicks(2) {
                updateTimer()
            }
        }

        updateTimer()
    }


    /**
     * Shows the HudElement for a specified amount of ticks.
     *
     * @param ticks The amount of ticks to show the HudElement for.
     */
    fun showForXClientTicks(ticks: Int) {
        visible = true
        runAfterClientTicks(ticks) { visible = false }
    }
}
