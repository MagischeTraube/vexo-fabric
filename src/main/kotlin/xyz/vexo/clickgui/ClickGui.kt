package xyz.vexo.clickgui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.universal.UKeyboard
import xyz.vexo.clickgui.components.Tooltip
import xyz.vexo.clickgui.components.panels.CategoryPanel
import xyz.vexo.clickgui.components.panels.GuiSettingsOverlay
import xyz.vexo.clickgui.components.panels.HeaderPanel
import xyz.vexo.clickgui.components.panels.ModulePanel
import xyz.vexo.clickgui.components.panels.SettingsPanel
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.clickgui.theme.heightTo
import xyz.vexo.clickgui.theme.widthTo
import xyz.vexo.config.ConfigManager
import xyz.vexo.config.impl.KeybindSetting
import xyz.vexo.features.Category
import xyz.vexo.Vexo.mc
import xyz.vexo.utils.runAfterClientTicks

/**
 * Main configuration GUI — "Dark Glassmorphism" build. Three glass panels (categories / modules /
 * settings) under a glass header, with an animated open transition, a tooltip layer and an effects
 * overlay. Layout scale follows [GuiPrefs.scale].
 */
class ClickGui : WindowScreen(ElementaVersion.V10) {

    private var currentNav: CategoryPanel.Nav = CategoryPanel.Nav.Cat(Category.entries.first())
    private var currentSearch: String = ""
    val activeKeybindSettings = mutableListOf<KeybindSetting>()

    private lateinit var headerPanel: HeaderPanel
    private lateinit var categoryPanel: CategoryPanel
    private lateinit var modulePanel: ModulePanel
    private lateinit var settingsPanel: SettingsPanel

    init {
        setupUI()
        Tooltip.attach(window)

        window.onKeyType { _, keyCode ->
            if (activeKeybindSettings.any { it.listening }) return@onKeyType
            if (keyCode == UKeyboard.KEY_ESCAPE) {
                mc.execute { runAfterClientTicks(1) { displayScreen(null) } }
            }
        }
    }

    private fun setupUI() {
        val mult = GuiPrefs.sizeMultiplier()
        val w = (86f * mult).coerceIn(50f, 97f)
        val h = (86f * mult).coerceIn(50f, 95f)

        // Dim scrim behind the window, faded in.
        val scrim = UIBlock(Theme.overlayScrim()).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf window

        // Rounded backing behind the window, kept OUTSIDE mainContainer so it can't affect the panel
        // layout. This gives the few-pixel rounded outer corners (plus a faint accent edge); the
        // panels round their own corners (radius 5) and reveal this dark base at the corners/seams.
        UIRoundedRectangle(7f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = w.percent() + 2.gpx()
            height = h.percent() + 2.gpx()
        }.setColor(Theme.glow(150)) childOf window

        UIRoundedRectangle(6f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = w.percent()
            height = h.percent()
        }.setColor(Theme.panelDark()) childOf window

        val mainContainer = UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = w.percent()
            height = h.percent()
        } childOf window
        mainContainer.onMouseClick { releaseWindowFocus() }

        headerPanel = HeaderPanel(
            onSearchUpdate = { onSearchTextChanged(it) },
            onOpenSettings = { openGuiSettings() }
        ).also { it childOf mainContainer }

        val contentContainer = UIContainer().constrain {
            y = SiblingConstraint()
            width = 100.percent()
            height = FillConstraint()
        } childOf mainContainer

        categoryPanel = CategoryPanel { nav -> onNavSelected(nav) }.also { it childOf contentContainer }

        modulePanel = ModulePanel(
            onModuleSettings = { settingsPanel.showModuleSettings(it, activeKeybindSettings) }
        ).also { it childOf contentContainer }

        settingsPanel = SettingsPanel().also { it childOf contentContainer }

        onNavSelected(currentNav)

        // Open animation: grow from a touch smaller + fade the scrim in.
        if (GuiPrefs.animations) {
            scrim.setColor(Theme.overlayScrim().let { java.awt.Color(it.red, it.green, it.blue, 0) })
            scrim.colorTo(Theme.overlayScrim(), 0.25f)
            mainContainer.setWidth((w * 0.95f).percent())
            mainContainer.setHeight((h * 0.95f).percent())
            mainContainer.widthTo(w.percent(), 0.22f)
            mainContainer.heightTo(h.percent(), 0.22f)
        }
    }

    private fun onNavSelected(nav: CategoryPanel.Nav) {
        currentNav = nav
        currentSearch = ""
        categoryPanel.select(nav)
        showNavModules()
        settingsPanel.showSettingsHint()
    }

    private fun showNavModules() {
        when (val nav = currentNav) {
            is CategoryPanel.Nav.Cat -> modulePanel.showCategoryModules(nav.category, currentSearch)
            CategoryPanel.Nav.Favorites -> modulePanel.showFavorites()
            CategoryPanel.Nav.Active -> modulePanel.showActiveModules()
        }
    }

    private fun onSearchTextChanged(searchText: String) {
        currentSearch = searchText
        if (searchText.isEmpty()) {
            showNavModules()
        } else {
            modulePanel.searchAllModules(searchText)
        }
    }

    private fun openGuiSettings() {
        GuiSettingsOverlay(onApply = {
            // Reopen so structural theme changes (glass / accent / scale) take effect.
            mc.execute { runAfterClientTicks(1) { displayScreen(ClickGui()) } }
        }) childOf window
    }

    override fun shouldCloseOnEsc(): Boolean = false

    override fun isPauseScreen(): Boolean = false

    override fun onScreenClose() {
        Tooltip.detach()
        ConfigManager.save()
    }
}
