package xyz.vexo.clickgui.components.panels

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.universal.UScreen
import net.minecraft.util.Util
import xyz.vexo.Vexo.mc
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.gsib
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.Theme.withAlpha
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.features.Category
import xyz.vexo.hud.MoveActiveHudsGui

/**
 * Glass category sidebar. Adds two virtual entries on top of the real [Category] list — Favorites
 * and Active — for quick access. Selection is reported through [Nav].
 */
class CategoryPanel(
    private val onNavClick: (Nav) -> Unit
) : UIContainer() {

    /** A sidebar destination: a real category, or one of the virtual quick-access views. */
    sealed interface Nav {
        data class Cat(val category: Category) : Nav
        data object Favorites : Nav
        data object Active : Nav
    }

    private val rowBackgrounds = mutableMapOf<Nav, UIComponent>()
    private val rowIndicators = mutableMapOf<Nav, UIComponent>()
    private var selected: Nav? = null

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
        UIRoundedRectangle(5f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.sidebar()) childOf this

        UIBlock(Theme.divider()).constrain {
            x = 0.pixels(true)
            width = 1.gpx()
            height = 100.percent()
        } childOf this

        val scrollComponent = ScrollComponent("", innerPadding = 0f).constrain {
            x = 0.pixels()
            y = 6.gpx()
            width = 100.percent()
            height = 100.percent() - 51.gpx()
        } childOf this

        val rows = buildList {
            add(Nav.Favorites to ("Favorites" to Theme.star()))
            add(Nav.Active to ("Active" to Theme.success()))
            Category.entries.forEach { add(Nav.Cat(it) to (it.displayName to Theme.accent())) }
        }

        rows.forEachIndexed { index, (nav, label) ->
            createRow(nav, label.first, label.second).constrain {
                y = if (index == 0) 0.pixels() else gsib(2f)
            } childOf scrollComponent
        }

        createButton("Discord") {
            try {
                Util.getPlatform().openUri("https://discord.gg/wfW3aEEpVA")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.constrain {
            y = 6.gpx(true)
        } childOf this
        createButton("Move GUI") {
            mc.execute { UScreen.displayScreen(MoveActiveHudsGui()) }
        }.constrain {
            y = 44.gpx(true)
        } childOf this
    }

    private fun createRow(nav: Nav, label: String, dotColor: java.awt.Color): UIComponent {
        return UIContainer().constrain {
            width = 100.percent()
            height = 38.gpx()
        }.apply {
            val background = UIRoundedRectangle(8f).constrain {
                x = 5.gpx()
                y = 2.gpx()
                width = 100.percent() - 9.gpx()
                height = 100.percent() - 4.gpx()
            }.setColor(Theme.card()) childOf this
            rowBackgrounds[nav] = background

            // Left accent indicator, lit only when selected.
            val indicator = UIRoundedRectangle(2f).constrain {
                x = 2.gpx()
                y = CenterConstraint()
                width = 3.gpx()
                height = 18.gpx()
            }.setColor(Theme.accent().withAlpha(0)) childOf this
            rowIndicators[nav] = indicator

            UIRoundedRectangle(3f).constrain {
                x = 14.gpx()
                y = CenterConstraint()
                width = 6.gpx()
                height = 6.gpx()
            }.setColor(dotColor) childOf this

            UIText(label).constrain {
                x = 26.gpx()
                y = CenterConstraint()
                textScale = 1.2f.gpx()
            }.setColor(Theme.textPrimary()) childOf this

            onMouseEnter {
                if (selected != nav) background.colorTo(Theme.cardHover())
            }
            onMouseLeave {
                if (selected != nav) background.colorTo(Theme.card())
            }
            onMouseClick { onNavClick(nav) }
        }
    }

    private fun createButton(label: String, onClick: () -> Unit): UIComponent {
        return UIContainer().constrain {
            x = 0.pixels()
            width = 100.percent()
            height = 38.gpx()
        }.apply {
            val background = UIRoundedRectangle(8f).constrain {
                x = 5.gpx()
                y = 2.gpx()
                width = 100.percent() - 10.gpx()
                height = 100.percent() - 4.gpx()
            }.setColor(Theme.accent()) childOf this

            UIText(label).constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                textScale = 1.3f.gpx()
            }.setColor(Theme.textOnAccent()) childOf this

            onMouseEnter { background.colorTo(Theme.accentHover()) }
            onMouseLeave { background.colorTo(Theme.accent()) }
            onMouseClick { onClick() }
        }
    }

    fun select(nav: Nav) {
        selected?.let {
            rowBackgrounds[it]?.colorTo(Theme.card())
            rowIndicators[it]?.colorTo(Theme.accent().withAlpha(0))
        }
        selected = nav
        rowBackgrounds[nav]?.colorTo(Theme.cardActive())
        rowIndicators[nav]?.colorTo(Theme.accent())
    }
}
