package xyz.vexo.clickgui.components.panels

import net.minecraft.Util
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import xyz.vexo.clickgui.components.ClickGuiColor
import xyz.vexo.features.Category

/**
 * Category panel component
 */
class CategoryPanel(
    private val onCategoryClick: (Category) -> Unit
) : UIContainer() {

    private val categoryBackgrounds = mutableMapOf<Category, UIBlock>()
    private var selectedCategory: Category? = null

    init {
        constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 19.percent()
            height = 100.percent()
        }

        setupPanel()
    }

    private fun setupPanel() {
        UIBlock(ClickGuiColor.BACKGROUND_COLOR).constrain {
            width = 100.percent()
            height = 100.percent()
        } childOf this

        val scrollComponent = ScrollComponent("", innerPadding = 0f).constrain {
            x = 0.pixels()
            y = 0.pixels()
            width = 100.percent()
            height = 100.percent() - 45.pixels()
        } childOf this

        Category.entries.forEachIndexed { index, category ->
            createCategoryButton(category).constrain {
                y = if (index == 0) 0.pixels() else SiblingConstraint()
            } childOf scrollComponent
        }

        createDiscordButton() childOf this
    }

    private fun createCategoryButton(category: Category): UIComponent {
        return UIContainer().constrain {
            width = 100.percent()
            height = 40.pixels()
        }.apply {
            val background = UIBlock(ClickGuiColor.BACKGROUND_COLOR).constrain {
                x = 2.pixels()
                y = 2.pixels()
                width = 100.percent() - 4.pixels()
                height = 100.percent() - 4.pixels()
            } childOf this

            categoryBackgrounds[category] = background

            UIText(category.displayName).constrain {
                x = 12.pixels()
                y = CenterConstraint()
                textScale = 1.3f.pixels()
            } childOf this

            onMouseEnter {
                if (selectedCategory != category) {
                    background.setColor(ClickGuiColor.HOVER_COLOR)
                }
            }

            onMouseLeave {
                if (selectedCategory != category) {
                    background.setColor(ClickGuiColor.BACKGROUND_COLOR)
                }
            }

            onMouseClick {
                onCategoryClick(category)
            }
        }
    }

    private fun createDiscordButton(): UIComponent {
        return UIContainer().constrain {
            x = 0.pixels()
            y = 0.pixels(true)
            width = 100.percent()
            height = 40.pixels()
        }.apply {
            val background = UIBlock(ClickGuiColor.ACCENT_COLOR).constrain {
                x = 2.pixels()
                y = 2.pixels()
                width = 100.percent() - 4.pixels()
                height = 100.percent() - 4.pixels()
            } childOf this

            UIText("Discord").constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 1.3f.pixels()
            }.setColor(ClickGuiColor.WHITE_TEXT_COLOR) childOf this

            onMouseEnter {
                background.setColor(ClickGuiColor.HOVER_COLOR)
            }

            onMouseLeave {
                background.setColor(ClickGuiColor.ACCENT_COLOR)
            }

            onMouseClick {
                try {
                    Util.getPlatform().openUri("https://discord.gg/wfW3aEEpVA")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun selectCategory(category: Category) {
        selectedCategory?.let { prevCategory ->
            categoryBackgrounds[prevCategory]?.setColor(ClickGuiColor.BACKGROUND_COLOR)
        }

        selectedCategory = category
        categoryBackgrounds[category]?.setColor(ClickGuiColor.ACCENT_COLOR)
    }
}
