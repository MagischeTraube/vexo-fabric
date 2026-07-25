package xyz.vexo.clickgui

import com.google.gson.JsonObject
import xyz.vexo.Vexo
import xyz.vexo.utils.logError
import java.awt.Color
import java.io.File

object GuiPrefs {
    private val file = File(Vexo.configDir, "gui.json")

    // Effect toggles
    var animations: Boolean = true
    var blurGlass: Boolean = true
    var shadows: Boolean = true
    var glow: Boolean = true
    var tooltips: Boolean = true

    /** Accent / brand color used across the GUI. Defaults to the Vexo indigo. */
    var accentColor: Color = Color(99, 102, 241)

    /** GUI scale level 1..5 (3 = default). Maps to a width/height multiplier in ClickGui. */
    var scale: Int = 3
        set(value) {
            field = value.coerceIn(1, 5)
        }

    /** Module names the user starred. */
    val favorites: MutableSet<String> = linkedSetOf()

    fun isFavorite(name: String): Boolean = name in favorites

    fun toggleFavorite(name: String) {
        if (!favorites.add(name)) favorites.remove(name)
        save()
    }

    /** Multiplier applied to the base GUI size, derived from [scale]. */
    fun sizeMultiplier(): Float = when (scale) {
        1 -> 0.75f
        2 -> 0.88f
        3 -> 1.0f
        4 -> 1.1f
        else -> 1.2f
    }

    fun load() {
        try {
            if (!file.exists()) return
            val root = Vexo.gson.fromJson(file.readText(), JsonObject::class.java) ?: return
            root.get("animations")?.let { animations = it.asBoolean }
            root.get("blurGlass")?.let { blurGlass = it.asBoolean }
            root.get("shadows")?.let { shadows = it.asBoolean }
            root.get("glow")?.let { glow = it.asBoolean }
            root.get("tooltips")?.let { tooltips = it.asBoolean }
            root.get("scale")?.let { scale = it.asInt }
            root.getAsJsonObject("accent")?.let { c ->
                accentColor = Color(
                    c.get("r")?.asInt ?: 99,
                    c.get("g")?.asInt ?: 102,
                    c.get("b")?.asInt ?: 241
                )
            }
            root.getAsJsonArray("favorites")?.forEach { favorites.add(it.asString) }
        } catch (e: Exception) {
            logError(e, this)
        }
    }

    fun save() {
        try {
            val root = JsonObject()
            root.addProperty("animations", animations)
            root.addProperty("blurGlass", blurGlass)
            root.addProperty("shadows", shadows)
            root.addProperty("glow", glow)
            root.addProperty("tooltips", tooltips)
            root.addProperty("scale", scale)
            JsonObject().apply {
                addProperty("r", accentColor.red)
                addProperty("g", accentColor.green)
                addProperty("b", accentColor.blue)
                root.add("accent", this)
            }
            com.google.gson.JsonArray().apply {
                favorites.forEach { add(it) }
                root.add("favorites", this)
            }
            file.writeText(Vexo.gson.toJson(root))
        } catch (e: Exception) {
            logError(e, this)
        }
    }
}
