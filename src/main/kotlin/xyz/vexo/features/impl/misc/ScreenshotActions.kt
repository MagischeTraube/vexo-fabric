package xyz.vexo.features.impl.misc

import xyz.vexo.features.Module
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.KeybindEvent
import org.lwjgl.glfw.GLFW
import xyz.vexo.Vexo.mc
import xyz.vexo.features.impl.misc.screenshots.ScreenshotHud
import xyz.vexo.features.impl.misc.screenshots.ScreenshotScreen
import xyz.vexo.config.impl.KeybindSetting

object ScreenshotActions : Module(
    name = "Screenshot Actions",
    description = "Adds actions to screenshot messages in the chat",
    toggled = false
) {
    val onlySaveEditedScreenshot by BooleanSetting(
        "Only Save Edited Screenshot",
        description = "Only save the edited screenshot, not the original",
        default = true
    )

    val autoCopyToClipboard by BooleanSetting(
        "Auto Copy to Clipboard",
        description = "Automatically copy the saved screenshot to your clipboard",
        default = false
    )

    val editableSelection by BooleanSetting(
        "Editable Screenshot",
        description = "Draw rectangles or crop the screenshot before saving",
        default = true
    )

    val screenshotKeybind by KeybindSetting(
        "Screenshot Keybind",
        description = "Keybind to open screenshot hud",
        default = GLFW.GLFW_KEY_F6
    )

    @JvmStatic
    var displayScreenshotHud = false

    @EventHandler
    fun onKeyPress(event: KeybindEvent) {
        if (event.key.value == screenshotKeybind) {
            if (mc.screen is ScreenshotScreen) return
            val previousScreen = mc.screen
            val renderTarget   = mc.mainRenderTarget
            displayScreenshotHud = true
            ScreenshotHud.reset()

            ScreenshotHud.updateBackgroundImage(renderTarget) {
                if (mc.screen == null || mc.screen !is ScreenshotScreen) {
                    mc.setScreen(ScreenshotScreen(previousScreen))
                }
            }
        }
    }
}
