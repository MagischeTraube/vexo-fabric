package xyz.vexo.features.impl.misc.screenshots

import java.awt.Color
import javax.imageio.ImageIO

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.NativeImage

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents

import net.minecraft.client.Screenshot
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier.fromNamespaceAndPath

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import xyz.vexo.Vexo
import xyz.vexo.Vexo.mc
import xyz.vexo.features.impl.misc.ScreenshotActions
import xyz.vexo.features.impl.misc.screenshots.config.ScreenshotColors
import xyz.vexo.features.impl.misc.screenshots.ui.ScreenshotHudRenderer
import xyz.vexo.features.impl.misc.screenshots.ui.components.ButtonPositionManager
import xyz.vexo.features.impl.misc.screenshots.util.ScreenshotChatNotifications
import xyz.vexo.features.impl.misc.screenshots.util.ScreenshotFileUtil
import xyz.vexo.features.impl.misc.screenshots.util.ScreenshotProcessor
import xyz.vexo.features.impl.misc.screenshots.util.SelectionBounds
import xyz.vexo.utils.ClipboardUtils
import xyz.vexo.utils.logError
import xyz.vexo.utils.modMessage

data class HandleDragState(
    val handle: CornerHandle,
    val originX: Double,
    val originY: Double,
    val selectionAtDragStart: SelectionBounds
)

enum class CornerHandle { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

enum class ScreenshotMode { SELECT, DRAW }

data class RectDrawAction(
    val bounds: SelectionBounds,
    val color: Color
)

object ScreenshotHud {

    internal var selection = SelectionBounds()
    internal var selectionStartPoint: Pair<Double, Double>? = null
    internal var activeDrag: HandleDragState? = null
    internal var selectionIsConfirmed = false

    internal var currentMode = ScreenshotMode.SELECT
    internal var selectedDrawColor = ScreenshotColors.drawColors[0]
    internal val drawnRectangles = mutableListOf<RectDrawAction>()
    internal var activeRectStart: Pair<Double, Double>? = null

    private var image: NativeImage? = null
    private var texture: DynamicTexture? = null

    internal val BACKGROUND_TEXTURE_ID = fromNamespaceAndPath("screenshots", "background")
    fun hasImage(): Boolean = image != null

    init {
        ScreenEvents.AFTER_INIT.register { _, screen: Screen, _, _ ->
            if (screen !is ScreenshotScreen) destroy()

            ScreenEvents.afterRender(screen).register { _, graphics: GuiGraphics, mouseX, mouseY, _ ->
                if (mc.screen !is ScreenshotScreen) return@register
                ScreenshotHudRenderer.renderOverlay(
                    graphics,
                    mouseX.toDouble(),
                    mouseY.toDouble(),
                    selection,
                    selectionIsConfirmed,
                    currentMode,
                    drawnRectangles
                )
            }

            ScreenMouseEvents.allowMouseClick(screen).register { _, event ->
                if (!ScreenshotActions.displayScreenshotHud) return@register true
                ScreenshotInputHandler.handleMouseClick(event.x, event.y, screen)
                false
            }

            ScreenMouseEvents.allowMouseDrag(screen).register { _, event, _, _ ->
                if (!ScreenshotActions.displayScreenshotHud) return@register false
                ScreenshotInputHandler.handleMouseDrag(event.x, event.y)
                true
            }

            ScreenMouseEvents.afterMouseRelease(screen).register { _, _, _ ->
                if (!ScreenshotActions.displayScreenshotHud) return@register false
                ScreenshotInputHandler.handleMouseRelease(screen)
                false
            }

            ScreenKeyboardEvents.allowKeyPress(screen).register { _, key ->
                if (!ScreenshotActions.displayScreenshotHud) return@register true
                ScreenshotInputHandler.handleKeyPress(key.key, screen)
            }
        }
    }

    /**
     * Undoes the last rectangle drawn.
     */
    internal fun undoLastRectangle() {
        if (drawnRectangles.isNotEmpty()) {
            drawnRectangles.removeAt(drawnRectangles.lastIndex)
        }
    }

    /**
     * Saves the current screenshot.
     *
     * @param screen The current screen.
     * @param cropToSelection Whether to crop the screenshot to the current selection.
     */
    internal fun saveScreenshot(screen: Screen, cropToSelection: Boolean) {
        val originalImage = image ?: return
        val guiScale = mc.window.guiScale
        val gameDirectory = mc.gameDirectory

        val imageCopy = NativeImage(originalImage.width, originalImage.height, false)
        imageCopy.copyFrom(originalImage)

        val rectsToDraw = ArrayList(drawnRectangles)
        val currentSelection = selection
        val outputFile = ScreenshotFileUtil.createScreenshotFile(gameDirectory)
        val shouldSaveUnedited = !ScreenshotActions.onlySaveEditedScreenshot && cropToSelection

        if (screen is ScreenshotScreen) screen.onClose()
        ScreenshotActions.displayScreenshotHud = false
        destroy()

        Vexo.scope.launch(Dispatchers.Default) {
            try {
                if (shouldSaveUnedited) {
                    withContext(Dispatchers.IO) {
                        imageCopy.writeToFile(ScreenshotFileUtil.createScreenshotFile(gameDirectory).toPath())
                    }
                }

                ScreenshotProcessor.drawRectangles(imageCopy, rectsToDraw, guiScale.toDouble())

                val finalImage = if (cropToSelection) {
                    ScreenshotProcessor.cropToSelection(imageCopy, currentSelection, guiScale.toDouble())
                } else {
                    imageCopy
                }

                withContext(Dispatchers.IO) {
                    finalImage.writeToFile(outputFile.toPath())
                }

                if (ScreenshotActions.autoCopyToClipboard) {
                    try {
                        val awtImage = withContext(Dispatchers.IO) { ImageIO.read(outputFile) }
                        ClipboardUtils.copyImageToClipboard(awtImage)
                        mc.execute { modMessage("Screenshot auto-copied to clipboard!") }
                    } catch (e: Exception) {
                        mc.execute { modMessage("§cFailed to auto-copy screenshots.") }
                        logError(e, this@ScreenshotHud)
                    }
                }

                finalImage.close()
                if (cropToSelection) imageCopy.close()

                ScreenshotChatNotifications.sendSavedMessage(outputFile)

            } catch (e: Exception) {
                logError(e, this@ScreenshotHud)
                modMessage("§cError processing screenshots!")
            }
        }
    }

    @JvmStatic
    fun updateBackgroundImage(target: RenderTarget, onReady: () -> Unit = {}) {
        destroy()
        Screenshot.takeScreenshot(target) { capturedImage: NativeImage ->
            mc.execute {
                image = capturedImage
                val newTexture = DynamicTexture({ "screenshots" }, capturedImage)
                texture = newTexture
                mc.textureManager.register(BACKGROUND_TEXTURE_ID, newTexture)
                onReady()
            }
        }
    }

    @JvmStatic
    fun reset() {
        selection = SelectionBounds()
        selectionStartPoint = null
        selectionIsConfirmed = false
        activeDrag = null
        drawnRectangles.clear()
        currentMode = ScreenshotMode.SELECT
        selectedDrawColor = ScreenshotColors.drawColors[0]
        ButtonPositionManager.reset()
    }

    internal fun destroy() {
        texture?.close()
        texture = null
        image?.close()
        image = null
        selectionStartPoint = null
        selectionIsConfirmed = false
        activeDrag = null
        drawnRectangles.clear()
        currentMode = ScreenshotMode.SELECT
        ButtonPositionManager.reset()
    }
}
