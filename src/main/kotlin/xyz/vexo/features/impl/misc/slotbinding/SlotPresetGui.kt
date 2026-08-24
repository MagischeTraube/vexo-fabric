package xyz.vexo.features.impl.misc.slotbinding

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.constraints.XConstraint
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.universal.UScreen
import net.minecraft.client.gui.screens.Screen
import xyz.vexo.clickgui.gpx
import xyz.vexo.features.impl.misc.SlotBinding
import xyz.vexo.clickgui.gsib
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.Theme.withAlpha
import xyz.vexo.clickgui.theme.colorTo
import java.awt.Color

class SlotPresetGui(private val previousScreen: Screen?) : WindowScreen(ElementaVersion.V10) {

    private val presetList: ScrollComponent

    init {
        val scrim = UIBlock(Theme.overlayScrim()).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf window
        scrim.onMouseClick { close() }

        val card = UIRoundedRectangle(12f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 384.gpx()
            height = 360.gpx()
        } childOf window
        card.setColor(Theme.panelDark())
        card.enableEffect(OutlineEffect(Theme.glow(140), 1f))
        card.onMouseClick { it.stopPropagation() }

        UIText("Slot Binding Presets").constrain {
            x = 22.gpx()
            y = 19.gpx()
            textScale = 1.68.gpx()
        }.setColor(Theme.accent()) childOf card

        UIBlock(Theme.divider()).constrain {
            y = 53.gpx()
            width = 100.percent()
            height = 1.gpx()
        } childOf card

        presetList = ScrollComponent("No presets saved yet", innerPadding = 0f).constrain {
            x = 0.pixels()
            y = 62.gpx()
            width = 100.percent()
            height = 202.gpx()
        } childOf card

        UIBlock(Theme.divider()).constrain {
            y = 274.gpx()
            width = 100.percent()
            height = 1.gpx()
        } childOf card

        buildSaveRow(card)
        buildCloseButton(card)
        refreshPresetList()
    }

    private fun close() = UScreen.displayScreen(previousScreen)

    private fun buildSaveRow(card: UIComponent) {
        val inputBackground = UIRoundedRectangle(5f).constrain {
            x = 22.gpx()
            y = 288.gpx()
            width = 216.gpx()
            height = 29.gpx()
        } childOf card
        inputBackground.setColor(Theme.inputBackground())

        val nameInput = UITextInput("Type here to save preset").constrain {
            x = 7.gpx()
            y = CenterConstraint()
            width = 202.gpx()
            height = 11.gpx()
        }
        nameInput.setColor(Theme.textPrimary())
        nameInput.setTextScale(1.2.gpx())
        nameInput childOf inputBackground

        inputBackground.onMouseClick { nameInput.grabWindowFocus() }

        val saveButton = UIContainer().constrain {
            x = 22.gpx(true)
            y = 288.gpx()
            width = 108.gpx()
            height = 29.gpx()
        } childOf card

        val saveBackground = UIRoundedRectangle(5f).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf saveButton
        saveBackground.setColor(Theme.accent())

        UIText("Save Current").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.08.gpx()
        }.setColor(Theme.textOnAccent()) childOf saveButton

        saveButton.onMouseEnter { saveBackground.colorTo(Theme.accentHover()) }
        saveButton.onMouseLeave { saveBackground.colorTo(Theme.accent()) }
        saveButton.onMouseClick {
            val name = nameInput.getText().trim()
            if (name.isEmpty()) return@onMouseClick
            SlotBinding.savePreset(name)
            nameInput.setText("")
            refreshPresetList()
        }
    }

    private fun buildCloseButton(card: UIComponent) {
        val closeButton = UIContainer().constrain {
            x = 22.gpx(true)
            y = 17.gpx()
            width = 24.gpx()
            height = 24.gpx()
        } childOf card

        val closeText = UIText("x").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.44.gpx()
        } childOf closeButton
        closeText.setColor(Theme.textMuted())

        closeButton.onMouseEnter { closeText.setColor(Theme.textPrimary()) }
        closeButton.onMouseLeave { closeText.setColor(Theme.textMuted()) }
        closeButton.onMouseClick { close() }
    }

    private fun refreshPresetList() {
        presetList.clearChildren()
        SlotBinding.presetNames().forEachIndexed { index, name ->
            buildPresetRow(name).constrain {
                y = if (index == 0) 5.gpx() else gsib(4.8f)
            } childOf presetList
        }
    }

    private fun buildPresetRow(name: String): UIContainer {
        return UIContainer().constrain {
            width = 100.percent()
            height = 36.gpx()
        }.apply {
            buildNameField(name) childOf this

            buildRowButton("Delete", Theme.toggleOff(), 22.gpx(true)) {
                SlotBinding.deletePreset(name)
                refreshPresetList()
            } childOf this

            buildRowButton("Load", Theme.accent(), 84.gpx(true)) {
                SlotBinding.loadPreset(name)
            } childOf this

            buildRowButton("Save", Theme.success(), 146.gpx(true)) {
                SlotBinding.savePreset(name)
            } childOf this
        }
    }

    private fun buildNameField(name: String): UIComponent {
        val container = UIContainer().constrain {
            x = 22.gpx()
            y = CenterConstraint()
            width = 154.gpx()
            height = 22.gpx()
        }

        val background = UIRoundedRectangle(4f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.inputBackground().withAlpha(0)) childOf container

        val nameInput = UITextInput(name).constrain {
            x = 6.gpx()
            y = CenterConstraint() - 0.5.gpx()
            width = 100.percent() - 12.gpx()
            height = 11.gpx()
        } childOf container
        nameInput.setColor(Theme.textPrimary())
        nameInput.setTextScale(1.2.gpx())
        nameInput.setText(name)

        container.onMouseClick {
            nameInput.grabWindowFocus()
            it.stopPropagation()
        }
        nameInput.onMouseClick {
            nameInput.grabWindowFocus()
            it.stopPropagation()
        }

        nameInput.onFocus { background.colorTo(Theme.inputBackground()) }

        fun commitRename() {
            val newName = nameInput.getText().trim()
            if (newName.isNotEmpty() && newName != name && SlotBinding.renamePreset(name, newName)) {
                refreshPresetList()
                return
            }
            nameInput.setText(name)
            background.colorTo(Theme.inputBackground().withAlpha(0))
        }

        nameInput.onActivate { commitRename() }
        nameInput.onFocusLost { commitRename() }

        return container
    }

    private fun buildRowButton(
        label: String,
        color: Color,
        xConstraint: XConstraint,
        onClick: () -> Unit
    ): UIContainer {
        return UIContainer().constrain {
            x = xConstraint
            y = CenterConstraint()
            width = 56.gpx()
            height = 24.gpx()
        }.apply {
            val background = UIRoundedRectangle(4f).constrain {
                width = 100.percent()
                height = 100.percent()
            } childOf this
            background.setColor(color)

            UIText(label).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 1.02.gpx()
            }.setColor(Theme.textOnAccent()) childOf this

            onMouseEnter { background.colorTo(Theme.accentHover()) }
            onMouseLeave { background.colorTo(color) }
            onMouseClick { onClick() }
        }
    }
}
