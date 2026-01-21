package xyz.vexo.hud

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.universal.UKeyboard
import xyz.vexo.Vexo.mc
import xyz.vexo.clickgui.components.ClickGuiColor
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.ConfigManager
import xyz.vexo.utils.runAfterClientTicks

/**
 * GUI for moving, scaling, and positioning active HUD elements.
 * Shows all HUDs that are currently active (module enabled + dependsOn conditions met).
 */
class MoveActiveHudsGui : WindowScreen(ElementaVersion.V10) {
    private val hudComponents = mutableMapOf<HudSetting, DraggableHudComponent>()

    init {
        setupUI()
        window.onKeyType { charTyped, keyCode ->
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                mc.execute {
                    runAfterClientTicks(1) {
                        displayScreen(null)
                    }
                }
            }
        }
    }

    private fun setupUI() {
        UIContainer().constrain {
            x = CenterConstraint()
            y = 10.pixels()
            width = ChildBasedSizeConstraint()
            height = ChildBasedSizeConstraint()
        }.apply {
            UIText("Move HUD Elements - Drag to reposition").constrain {
                textScale = 1.5.pixels()
            }.setColor(ClickGuiColor.ACCENT_COLOR) childOf this
        } childOf window

        UIText("Press ESC to save and exit").constrain {
            x = CenterConstraint()
            y = 35.pixels()
            textScale = 1.pixels()
        }.setColor(ClickGuiColor.GRAY_TEXT_COLOR) childOf window

        val activeHuds = HudManager.getActiveHuds()

        if (activeHuds.isEmpty()) {
            UIText("No active HUDs found").constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 1.2.pixels()
            }.setColor(ClickGuiColor.GRAY_TEXT_COLOR) childOf window
        } else {
            activeHuds.forEach { hudSetting ->
                val hudComponent = DraggableHudComponent(hudSetting)
                hudComponents[hudSetting] = hudComponent
                hudComponent childOf window
            }
        }
    }

    /**
     * Draggable component that represents a HUD element.
     */
    private inner class DraggableHudComponent(val hudSetting: HudSetting) : UIContainer() {
        private val hudElement = hudSetting.getCurrentValue()
        private var isDragging = false
        private var dragOffsetX = 0f
        private var dragOffsetY = 0f


        init {
            constrain {
                x = hudElement.x.pixels()
                y = hudElement.y.pixels()
                width = ChildBasedSizeConstraint() + 10.pixels()
                height = ChildBasedSizeConstraint() + 6.pixels()
            }

            val text = UIText(hudElement.text).constrain {
                x = 5.pixels()
                y = 3.pixels()
                textScale = hudElement.scale.pixels()
            }.setColor(ClickGuiColor.WHITE_TEXT_COLOR) childOf this

            enableEffect(OutlineEffect(ClickGuiColor.ACCENT_COLOR, 1f))

            onMouseClick { event ->
                isDragging = true
                dragOffsetX = event.relativeX
                dragOffsetY = event.relativeY
            }


            onMouseRelease {
                isDragging = false
            }

            onMouseDrag { mouseX, mouseY, _ ->
                if (!isDragging) return@onMouseDrag

                val newX = mouseX - dragOffsetX + getLeft()
                val newY = mouseY - dragOffsetY + getTop()

                val newXClamped = newX.coerceIn(0f, window.getWidth() - getWidth())
                val newYClamped = newY.coerceIn(0f, window.getHeight() - getHeight())

                setX(newXClamped.pixels())
                setY(newYClamped.pixels())
            }


            onMouseScroll { event ->
                val delta = if (event.delta > 0) 0.1f else -0.1f

                hudElement.scale = (hudElement.scale + delta).coerceAtLeast(0.1f)

                text.setTextScale(hudElement.scale.pixels())
            }

        }

        /**
         * Saves the current position back to the HUD setting.
         */
        fun savePosition() {
            // Offset because of border/outline effect (5px left, 3px top)
            val xOffset = 5
            val yOffset = 3
            val xPos = (getLeft() + xOffset).toInt()
            val yPos = (getTop() + yOffset).toInt()
            hudSetting.setPos(xPos, yPos)

            hudSetting.setPos(xPos, yPos)
        }
    }

    override fun onScreenClose() {
        hudComponents.forEach { (hudSetting, component) ->
            component.savePosition()
        }
        ConfigManager.save()
    }

    override fun isPauseScreen(): Boolean = false
}