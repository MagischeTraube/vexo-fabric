package xyz.vexo.clickgui

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import xyz.vexo.clickgui.components.ClickGuiColor
import xyz.vexo.clickgui.components.panels.HeaderPanel
import xyz.vexo.clickgui.components.panels.CategoryPanel
import xyz.vexo.clickgui.components.panels.ModulePanel
import xyz.vexo.clickgui.components.panels.SettingsPanel
import xyz.vexo.config.ConfigManager
import xyz.vexo.config.impl.KeybindSetting
import xyz.vexo.features.Category

/**
 * Main GUI to configure settings
 */
class ClickGui : WindowScreen(ElementaVersion.V10) {

    private var selectedCategory: Category? = null
    val activeKeybindSettings = mutableListOf<KeybindSetting>()

    private lateinit var headerPanel: HeaderPanel
    private lateinit var categoryPanel: CategoryPanel
    private lateinit var modulePanel: ModulePanel
    private lateinit var settingsPanel: SettingsPanel

    init {
        setupUI()
    }

    private fun setupUI() {
        val mainContainer = UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = 90.percent()
            height = 85.percent()
        } childOf window

        mainContainer.enableEffect(OutlineEffect(ClickGuiColor.ACCENT_COLOR, 2f))
        mainContainer.onMouseClick {
            releaseWindowFocus()
        }

        headerPanel = HeaderPanel { searchText ->
            onSearchTextChanged(searchText)
        }.also { it childOf mainContainer }

        val contentContainer = UIContainer().constrain {
            y = SiblingConstraint()
            width = 100.percent()
            height = FillConstraint()
        } childOf mainContainer

        categoryPanel = CategoryPanel { category ->
            onCategorySelected(category)
        }.also { it childOf contentContainer }

        modulePanel = ModulePanel(
            onModuleSettings = { module ->
                settingsPanel.showModuleSettings(module, activeKeybindSettings)
            }
        ).also { it childOf contentContainer }

        settingsPanel = SettingsPanel().also {
            it childOf contentContainer
        }

        Category.entries.firstOrNull()?.let { onCategorySelected(it) }
    }

    private fun onCategorySelected(category: Category) {
        selectedCategory = category
        categoryPanel.selectCategory(category)
        modulePanel.showCategory(category, "")
        settingsPanel.showSettingsHint()
    }

    private fun onSearchTextChanged(searchText: String) {
        if (searchText.isEmpty()) {
            selectedCategory?.let { modulePanel.showCategory(it, "") }
        } else {
            modulePanel.searchAllModules(searchText)
        }
    }

    override fun shouldCloseOnEsc(): Boolean {
        return !activeKeybindSettings.any { it.listening }
    }

    override fun isPauseScreen(): Boolean = false

    override fun onScreenClose() {
        ConfigManager.save()
    }
}