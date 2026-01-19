package xyz.vexo.hud

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
}