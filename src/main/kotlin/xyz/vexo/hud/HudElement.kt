package xyz.vexo.hud

data class HudElement(
    val name: String,
    var text: String,
    var x: Int = 5,
    var y: Int = 5,
    var scale: Float = 1f,
    var visible: Boolean = false
)