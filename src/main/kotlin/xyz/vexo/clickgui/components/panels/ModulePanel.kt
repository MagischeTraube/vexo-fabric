package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.GuiPrefs
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.gsib
import xyz.vexo.clickgui.components.Tooltip
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.features.Category
import xyz.vexo.features.Module
import xyz.vexo.features.ModuleManager

/**
 * Glass module list. Cards carry a faux drop-shadow, an accent glow when the module is enabled, a
 * favorite star and an in-name search highlight. Hover/toggle transitions are animated.
 */
class ModulePanel(
    private val onModuleSettings: (Module) -> Unit
) : UIContainer() {

    private var scroll: ScrollComponent? = null
    private var refreshCurrent: () -> Unit = {}

    init {
        constrain {
            x = SiblingConstraint()
            y = 0.pixels()
            width = 38.percent()
            height = 100.percent()
        }

        setupBackground()
    }

    private fun setupBackground() {
        UIRoundedRectangle(5f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.panel()) childOf this
    }

    /** Shows modules from [category], optionally filtered by [searchText]. */
    fun showCategoryModules(category: Category, searchText: String) {
        refreshCurrent = { showCategoryModules(category, searchText) }
        val modules = ModuleManager.getModulesByCategory(category).filterMatches(searchText)
        val empty = if (searchText.isEmpty()) "No modules in this category"
        else "No modules found for \"$searchText\""
        displayModules(modules, empty, searchText)
    }

    /** Searches all modules across every category for [searchText]. */
    fun searchAllModules(searchText: String) {
        refreshCurrent = { searchAllModules(searchText) }
        val modules = ModuleManager.getAllModules().filterMatches(searchText)
        displayModules(modules, "No modules found for \"$searchText\"", searchText)
    }

    /** Shows favorited modules. */
    fun showFavorites() {
        refreshCurrent = { showFavorites() }
        val modules = ModuleManager.getAllModules().filter { GuiPrefs.isFavorite(it.name) }
        displayModules(modules, "No favorites yet — click the star on a module", "")
    }

    /** Shows currently enabled modules. */
    fun showActiveModules() {
        refreshCurrent = { showActiveModules() }
        val modules = ModuleManager.getEnabledModules()
        displayModules(modules, "No modules are currently enabled", "")
    }

    private fun displayModules(modules: List<Module>, emptyMessage: String, searchText: String) {
        clearChildren()
        setupBackground()

        scroll = ScrollComponent("", innerPadding = 0f).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 100.percent()
        } childOf this

        if (modules.isEmpty()) {
            UIText(emptyMessage).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 1.gpx()
            }.setColor(Theme.textMuted()) childOf scroll!!
            return
        }

        modules.forEachIndexed { index, module ->
            createModuleButton(module, searchText).constrain {
                y = if (index == 0) 6.gpx() else gsib(5f)
            } childOf scroll!!
        }
    }

    private fun List<Module>.filterMatches(searchText: String): List<Module> {
        if (searchText.isEmpty()) return this
        return filter {
            it.name.contains(searchText, ignoreCase = true) ||
                it.description.contains(searchText, ignoreCase = true)
        }
    }

    private fun createModuleButton(module: Module, searchText: String): UIComponent {
        return UIContainer().constrain {
            width = 100.percent()
            height = 46.gpx()
        }.apply {
            UIRoundedRectangle(9f).constrain {
                x = 6.gpx()
                y = 5.gpx()
                width = 100.percent() - 8.gpx()
                height = 100.percent() - 4.gpx()
            }.setColor(Theme.shadow()) childOf this

            val glow = UIRoundedRectangle(10f).constrain {
                x = 3.gpx()
                y = 1.gpx()
                width = 100.percent() - 6.gpx()
                height = 100.percent()
            }.setColor(if (module.enabled) Theme.glow(55) else Theme.glow(0)) childOf this

            val background = UIRoundedRectangle(8f).constrain {
                x = 5.gpx()
                y = 2.gpx()
                width = 100.percent() - 10.gpx()
                height = 100.percent() - 4.gpx()
            }.setColor(if (module.enabled) Theme.cardActive() else Theme.card()) childOf this

            val statusDot = UIRoundedRectangle(3f).constrain {
                x = 16.gpx()
                y = CenterConstraint()
                width = 6.gpx()
                height = 6.gpx()
            }.setColor(if (module.enabled) Theme.success() else Theme.toggleOff()) childOf this

            addNameWithHighlight(module.name, searchText, this)

            if (module.description.isNotEmpty()) {
                UIText(module.description).constrain {
                    x = 28.gpx()
                    y = 27.gpx()
                    textScale = 0.8.gpx()
                }.setColor(Theme.textMuted().brighter()) childOf this
            }

            createStar(module, this)

            onMouseEnter {
                if (!module.enabled) background.colorTo(Theme.cardHover())
                Tooltip.show(module.description.ifEmpty { "Left-click: toggle · Right-click: settings" }, this)
            }
            onMouseLeave {
                if (!module.enabled) background.colorTo(Theme.card())
                Tooltip.hide()
            }

            onMouseClick { event ->
                when (event.mouseButton) {
                    0 -> {
                        module.toggle()
                        background.colorTo(if (module.enabled) Theme.cardActive() else Theme.card())
                        statusDot.colorTo(if (module.enabled) Theme.success() else Theme.toggleOff())
                        glow.colorTo(if (module.enabled) Theme.glow(55) else Theme.glow(0))
                    }
                    1 -> onModuleSettings(module)
                }
            }
        }
    }

    /** Renders the name, accent-coloring the matched search substring. */
    private fun addNameWithHighlight(name: String, search: String, parent: UIComponent) {
        val row = UIContainer().constrain {
            x = 28.gpx()
            y = 9.gpx()
            width = 100.percent() - 50.gpx()
            height = 12.gpx()
        } childOf parent

        val idx = if (search.isNotEmpty()) name.indexOf(search, ignoreCase = true) else -1
        if (idx < 0) {
            UIText(name).constrain {
                textScale = 1.3f.gpx()
            }.setColor(Theme.textPrimary()) childOf row
            return
        }

        val before = name.substring(0, idx)
        val match = name.substring(idx, idx + search.length)
        val after = name.substring(idx + search.length)
        var anchor: UIComponent? = null
        listOf(before to Theme.textPrimary(), match to Theme.accent(), after to Theme.textPrimary())
            .filter { it.first.isNotEmpty() }
            .forEach { (part, color) ->
                val t = UIText(part).constrain {
                    x = anchor?.let { SiblingConstraint() } ?: 0.pixels()
                    textScale = 1.3f.gpx()
                }.setColor(color) childOf row
                anchor = t
            }
    }

    private fun createStar(module: Module, parent: UIComponent) {
        val star = UIText(if (GuiPrefs.isFavorite(module.name)) "★" else "☆").constrain {
            x = 12.gpx(true)
            y = CenterConstraint()
            textScale = 1.4f.gpx()
        }.apply { setColor(if (GuiPrefs.isFavorite(module.name)) Theme.star() else Theme.textMuted()) } childOf parent

        star.onMouseClick { event ->
            event.stopPropagation()
            GuiPrefs.toggleFavorite(module.name)
            val fav = GuiPrefs.isFavorite(module.name)
            star.setText(if (fav) "★" else "☆")
            star.setColor(if (fav) Theme.star() else Theme.textMuted())
            refreshCurrent()
        }
    }
}
