package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.universal.UScreen
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.gsib
import xyz.vexo.clickgui.components.Tooltip
import xyz.vexo.clickgui.components.settings.*
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.Theme.withAlpha
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.clickgui.theme.xTo
import xyz.vexo.config.ConfigManager
import xyz.vexo.config.Setting
import xyz.vexo.config.impl.*
import xyz.vexo.features.Module
import xyz.vexo.hud.MoveActiveHudsGui

/**
 * Glass settings panel. Shows a header card for the selected module (name, description, master
 * toggle) and the scrollable list of its visible settings, each wrapped with a hover tooltip.
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
        UIRoundedRectangle(5f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.panelDark()) childOf this
    }

    /** Shows the default settings hint screen. */
    fun showSettingsHint() {
        clearChildren()
        setupBackground()

        UIText("Settings").constrain {
            x = 14.gpx()
            y = 14.gpx()
            textScale = 1.5.gpx()
        }.setColor(Theme.accent()) childOf this

        UIText("Right-click a feature to view its settings").constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            textScale = 1.1.gpx()
        }.setColor(Theme.textMuted()) childOf this
    }

    /** Shows settings for [module], tracking active keybind listeners in [activeKeybindSettings]. */
    fun showModuleSettings(module: Module, activeKeybindSettings: MutableList<KeybindSetting>) {
        currentModule = module
        activeKeybindSettings.clear()

        clearChildren()
        setupBackground()

        createModuleHeader(module) childOf this

        if (settingsScrollComponent == null) {
            settingsScrollComponent = ScrollComponent("", innerPadding = 0f)
        }

        val scroll = settingsScrollComponent!!.constrain {
            x = 0.pixels()
            y = 62.gpx()
            width = 100.percent()
            height = FillConstraint(false) - 2.gpx()
        } childOf this

        scroll.clearChildren()

        val settings = ConfigManager.getSettingsFromModule(module)
        val visibleSettings = settings.filter { it.shouldShowInGui() }

        if (visibleSettings.isEmpty()) {
            UIText("No settings available").constrain {
                x = 14.gpx()
                y = 12.gpx()
                textScale = 1.gpx()
            }.setColor(Theme.textMuted()) childOf scroll
        } else {
            visibleSettings.forEachIndexed { index, setting ->
                wrapSetting(setting, createSettingComponent(setting, activeKeybindSettings)).constrain {
                    y = if (index == 0) 4.gpx() else gsib(2f)
                } childOf scroll
            }
        }
    }

    /** Header card: module name, description and a master enable toggle. */
    private fun createModuleHeader(module: Module): UIComponent {
        return UIContainer().constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 58.gpx()
        }.apply {
            UIBlock(Theme.divider()).constrain {
                y = 0.pixels(true)
                width = 100.percent()
                height = 1.gpx()
            } childOf this

            UIText(module.name).constrain {
                x = 14.gpx()
                y = 12.gpx()
                textScale = 1.6.gpx()
            }.setColor(Theme.accent()) childOf this

            if (module.description.isNotEmpty()) {
                UIText(module.description).constrain {
                    x = 14.gpx()
                    y = 34.gpx()
                    textScale = 0.9.gpx()
                }.setColor(Theme.textMuted()) childOf this
            }

            val toggle = UIContainer().constrain {
                x = 14.gpx(true)
                y = CenterConstraint()
                width = 40.gpx()
                height = 20.gpx()
            } childOf this

            val toggleBg = UIRoundedRectangle(10f).constrain {
                width = 100.percent()
                height = 100.percent()
            }.setColor(if (module.enabled) Theme.accent() else Theme.toggleOff()) childOf toggle

            val knob = UIRoundedRectangle(8f).constrain {
                x = if (module.enabled) 22.gpx() else 2.gpx()
                y = 2.gpx()
                width = 16.gpx()
                height = 16.gpx()
            }.setColor(Theme.handle()) childOf toggle

            toggle.onMouseClick {
                module.toggle()
                toggleBg.colorTo(if (module.enabled) Theme.accent() else Theme.toggleOff())
                knob.xTo(if (module.enabled) 22.gpx() else 2.gpx())
            }
        }
    }

    /** Wraps a setting component so hovering anywhere on the row shows its description tooltip. */
    private fun wrapSetting(setting: Setting<*>, component: UIComponent): UIComponent {
        val row = UIContainer().constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = ChildBasedMaxSizeConstraint()
        }

        val indicator = UIRoundedRectangle(2f).constrain {
            x = 2.gpx()
            y = CenterConstraint()
            width = 3.gpx()
            height = 20.gpx()
        }.setColor(Theme.accent().withAlpha(0)) childOf row

        component childOf row

        row.onMouseEnter {
            indicator.colorTo(Theme.accent())
            Tooltip.show(setting.description, row)
        }
        row.onMouseLeave {
            indicator.colorTo(Theme.accent().withAlpha(0))
            Tooltip.hide()
        }
        return row
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
            is SelectorSetting -> SelectorSettingComponent(setting) {
                currentModule?.let { showModuleSettings(it, activeKeybindSettings) }
            }
            is ColorSetting -> ColorSettingComponent(setting)
            is ButtonSetting -> ButtonSettingComponent(setting)
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
