package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
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
import xyz.vexo.config.ConfigManager
import xyz.vexo.config.Setting
import xyz.vexo.config.impl.*
import xyz.vexo.features.Module
import xyz.vexo.hud.MoveActiveHudsGui

/**
 * Shows a header for the selected module and a scrollable list of
 * its visible settings.
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

    /**
     * Shows the default settings hint screen.
     */
    fun showSettingsHint() {
        currentModule = null

        clearChildren()
        settingsScrollComponent = null

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

    /**
     * Shows settings for [module].
     */
    fun showModuleSettings(
        module: Module,
        activeKeybindSettings: MutableList<KeybindSetting>
    ) {
        currentModule = module
        activeKeybindSettings.clear()

        clearChildren()
        setupBackground()

        val header = createModuleHeader(module)
        header childOf this
        val headerHeight = calculateHeaderHeight(module.description)

        if (settingsScrollComponent == null) {
            settingsScrollComponent = ScrollComponent(
                "",
                innerPadding = 0f
            )
        }

        val scroll = settingsScrollComponent!!.constrain {
            x = 0.pixels()
            y = headerHeight.gpx()
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

    /**
     * Calculates a safe header height.
     *
     * @param description The description of the module
     * @return The height of the header
     */
    private fun calculateHeaderHeight(description: String): Float {
        if (description.isEmpty()) {
            return 55f
        }

        val panelWidth = this.getWidth()

        val textWidth = if (panelWidth > 28f) {
            panelWidth - 28f
        } else {
            300f
        }

        val averageCharWidth = 5.0f

        val charsPerLine = maxOf(
            1,
            (textWidth / averageCharWidth).toInt()
        )

        val estimatedLines =
            description.length.toFloat() / charsPerLine.toFloat()

        val lineCount = maxOf(
            1,
            kotlin.math.ceil(estimatedLines).toInt()
        )

        val lineHeight = 10f
        return 34f + (lineCount * lineHeight) + 15f
    }

    /**
     * Creates the module header.
     *
     * @param module The module to create the header for
     * @return The module header
     */
    private fun createModuleHeader(module: Module): UIComponent {
        return UIContainer().constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 55.gpx()
        }.apply {

            UIText(module.name).constrain {
                x = 14.gpx()
                y = 12.gpx()
                textScale = 1.6.gpx()
            }.setColor(Theme.accent()) childOf this

            if (module.description.isNotEmpty()) {

                UIWrappedText(module.description).constrain {
                    x = 14.gpx()
                    y = 34.gpx()
                    width = 100.percent() - 28.gpx()
                    textScale = 0.9.gpx()
                }.setColor(Theme.textMuted()) childOf this

                UIBlock(Theme.divider()).constrain {
                    x = 0.pixels()
                    y = SiblingConstraint(8f)
                    width = 100.percent()
                    height = 1.gpx()
                } childOf this
            }
        }
    }

    /**
     * Wraps a setting component so hovering anywhere on the row shows its description tooltip.
     *
     * @param setting The setting to wrap
     * @param component The component to wrap
     * @return The wrapped component
     */
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

    /**
     * Creates the appropriate UI component for a setting.
     *
     * @param setting The setting to create a component for
     * @param activeKeybindSettings The list of active keybind settings
     * @return The created UI component
     */
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
