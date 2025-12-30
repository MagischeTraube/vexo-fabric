package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.universal.UScreen
import xyz.vexo.clickgui.components.ClickGuiColor
import xyz.vexo.clickgui.components.settings.*
import xyz.vexo.config.ConfigManager
import xyz.vexo.config.Setting
import xyz.vexo.config.impl.*
import xyz.vexo.features.Module
import xyz.vexo.hud.MoveActiveHudsGui

/**
 * Settings panel component
 */
class SettingsPanel : UIContainer() {

    private var settingsScrollComponent: ScrollComponent? = null
    private var currentModule: Module? = null

    init {
        constrain {
            x = SiblingConstraint()
            y = 0.pixels()
            width = FillConstraint()
            height = 100.percent()
        }

        setupBackground()
        showSettingsHint()
    }

    private fun setupBackground() {
        UIBlock(ClickGuiColor.BACKGROUND_COLOR).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this
    }

    fun showSettingsHint() {
        clearChildren()
        setupBackground()

        UIText("Settings").constrain {
            x = 10.pixels()
            y = 10.pixels()
            textScale = 1.5.pixels()
        }.setColor(ClickGuiColor.ACCENT_COLOR) childOf this

        UIText("Right Click Feature to view settings").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.1.pixels()
        }.setColor(ClickGuiColor.GRAY_TEXT_COLOR) childOf this
    }

    fun showModuleSettings(module: Module, activeKeybindSettings: MutableList<KeybindSetting>) {
        currentModule = module
        activeKeybindSettings.clear()

        clearChildren()
        setupBackground()

        UIText("Settings: ${module.name}").constrain {
            x = 10.pixels()
            y = 10.pixels()
            textScale = 1.5.pixels()
        }.setColor(ClickGuiColor.ACCENT_COLOR) childOf this

        if (settingsScrollComponent == null) {
            settingsScrollComponent = ScrollComponent("", innerPadding = 0f)
        }

        val scroll = settingsScrollComponent!!.constrain {
            x = 0.pixels()
            y = 40.pixels()
            width = 100.percent()
            height = FillConstraint(false) - 2.pixels()
        } childOf this

        scroll.clearChildren()

        val settings = ConfigManager.getSettingsFromModule(module)
        val visibleSettings = settings.filter { it.shouldShowInGui() }

        if (visibleSettings.isEmpty()) {
            UIText("No settings available").constrain {
                x = 10.pixels()
                y = 10.pixels()
            }.setColor(ClickGuiColor.GRAY_TEXT_COLOR) childOf scroll
        } else {
            visibleSettings.forEachIndexed { index, setting ->
                createSettingComponent(setting, activeKeybindSettings).constrain {
                    y = if (index == 0) 10.pixels() else SiblingConstraint(5f)
                } childOf scroll

                if (index < visibleSettings.size - 1) {
                    UIBlock(ClickGuiColor.SEPARATOR_COLOR).constrain {
                        y = SiblingConstraint(5f)
                        width = 100.percent() - 20.pixels()
                        x = 10.pixels()
                        height = 1.pixels()
                    } childOf scroll
                }
            }
        }
    }

    private fun createSettingComponent(
        setting: Setting<*>,
        activeKeybindSettings: MutableList<KeybindSetting>
    ): UIComponent {
        return when (setting) {
            is BooleanSetting -> BooleanSettingComponent(setting) {
                currentModule?.let { showModuleSettings(it, activeKeybindSettings) }
            }
            is SliderSetting -> SliderSettingComponent(setting)
            is StringSetting -> StringSettingComponent(setting)
            is SelectorSetting -> SelectorSettingComponent(setting)
            is ColorSetting -> ColorSettingComponent(setting)
            is KeybindSetting -> {
                activeKeybindSettings.add(setting)
                KeybindSettingComponent(setting)
            }
            is HudSetting -> HudSettingComponent(setting) {
                UScreen.displayScreen(MoveActiveHudsGui())
            }
            else -> UIContainer()
        }
    }
}
