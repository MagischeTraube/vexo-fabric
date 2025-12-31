package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.components.ClickGuiColor
import xyz.vexo.features.Category
import xyz.vexo.features.Module
import xyz.vexo.features.ModuleManager

/**
 * Module panel component
 */
class ModulePanel(
    private val onModuleSettings: (Module) -> Unit
) : UIContainer() {

    private var moduleScrollComponent: ScrollComponent? = null

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
        UIBlock(ClickGuiColor.DARK_BACKGROUND_COLOR).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this
    }

    fun showCategory(category: Category, searchText: String) {
        clearChildren()
        setupBackground()

        moduleScrollComponent = ScrollComponent("", innerPadding = 0f).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 100.percent()
        } childOf this

        val modules = ModuleManager.getModulesByCategory(category)

        val filteredModules = if (searchText.isEmpty()) {
            modules
        } else {
            modules.filter {
                it.name.contains(searchText, ignoreCase = true) ||
                        it.description.contains(searchText, ignoreCase = true)
            }
        }

        if (filteredModules.isEmpty()) {
            showEmptyMessage(if (searchText.isEmpty()) {
                "No modules in this category"
            } else {
                "No modules found for \"$searchText\""
            })
            return
        }

        filteredModules.forEachIndexed { index, module ->
            createModuleButton(module).constrain {
                y = if (index == 0) 0.pixels() else SiblingConstraint()
            } childOf moduleScrollComponent!!
        }
    }

    fun searchAllModules(searchText: String) {
        clearChildren()
        setupBackground()

        moduleScrollComponent = ScrollComponent("", innerPadding = 0f).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 100.percent()
        } childOf this

        val allModules = ModuleManager.getAllModules()
            .filter {
                it.name.contains(searchText, ignoreCase = true) ||
                        it.description.contains(searchText, ignoreCase = true)
            }

        if (allModules.isEmpty()) {
            showEmptyMessage("No modules found for \"$searchText\"")
            return
        }

        allModules.forEachIndexed { index, module ->
            createModuleButton(module).constrain {
                y = if (index == 0) 0.pixels() else SiblingConstraint()
            } childOf moduleScrollComponent!!
        }
    }

    private fun showEmptyMessage(message: String) {
        UIText(message).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        }.setColor(ClickGuiColor.GRAY_TEXT_COLOR) childOf moduleScrollComponent!!
    }

    private fun createModuleButton(module: Module): UIComponent {
        return UIContainer().constrain {
            width = 100.percent()
            height = 40.pixels()
        }.apply {
            val backgroundColor = if (module.enabled) {
                ClickGuiColor.ACCENT_COLOR
            } else {
                ClickGuiColor.DARK_BACKGROUND_COLOR
            }

            val background = UIBlock(backgroundColor).constrain {
                x = 2.pixels()
                y = 2.pixels()
                width = 100.percent() - 4.pixels()
                height = 100.percent() - 4.pixels()
            } childOf this

            UIText(module.name).constrain {
                x = 12.pixels()
                y = 10.pixels()
                textScale = 1.3f.pixels()
            } childOf this

            if (module.description.isNotEmpty()) {
                UIText(module.description).constrain {
                    x = 12.pixels()
                    y = 24.pixels()
                    textScale = 0.8.pixels()
                }.setColor(ClickGuiColor.GRAY_TEXT_COLOR.brighter()) childOf this
            }

            onMouseEnter {
                background.setColor(ClickGuiColor.HOVER_COLOR)
            }

            onMouseLeave {
                val color = if (module.enabled) {
                    ClickGuiColor.ACCENT_COLOR
                } else {
                    ClickGuiColor.DARK_BACKGROUND_COLOR
                }
                background.setColor(color)
            }

            onMouseClick { event ->
                when (event.mouseButton) {
                    0 -> {
                        module.toggle()
                        val newColor = if (module.enabled) {
                            ClickGuiColor.ACCENT_COLOR
                        } else {
                            ClickGuiColor.BACKGROUND_COLOR
                        }
                        background.setColor(newColor)
                    }
                    1 -> {
                        onModuleSettings(module)
                    }
                }
            }
        }
    }
}
