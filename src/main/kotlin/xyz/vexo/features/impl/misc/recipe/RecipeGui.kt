package xyz.vexo.features.impl.misc.recipe

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.*
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.minecraft.world.item.ItemStack
import java.io.File
import xyz.vexo.Vexo
import xyz.vexo.Vexo.mc
import xyz.vexo.clickgui.components.UIItem
import xyz.vexo.clickgui.components.UIItemRenderer
import xyz.vexo.clickgui.gpx
import xyz.vexo.clickgui.gsib
import xyz.vexo.clickgui.theme.Theme
import xyz.vexo.clickgui.theme.colorTo
import xyz.vexo.utils.sendCommand

class RecipeGUI(
    initialSearch: String = ""
) : WindowScreen(ElementaVersion.V10) {
    data class RecipeItem(
        val name: String,
        val itemStack: ItemStack,
        val itemId: String
    )

    private companion object {
        val searchHistory = mutableListOf<String>()
        private val HISTORY_FILE = File(Vexo.configDir, "recipe_history.json")

        @JvmStatic
        fun addSearchToHistory(query: String) {
            val clean = query.trim()
            if (clean.isBlank()) return
            searchHistory.remove(clean)
            searchHistory.add(0, clean)
            if (searchHistory.size > 5) {
                searchHistory.removeAt(searchHistory.size - 1)
            }
            saveHistory()
        }

        private fun saveHistory() {
            try {
                HISTORY_FILE.writeText(Vexo.gson.toJson(searchHistory.toList()))
            } catch (e: Exception) { }
        }

        fun loadHistory() {
            if (HISTORY_FILE.exists()) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
                    val loaded: List<String> = Vexo.gson.fromJson(HISTORY_FILE.readText(), type)
                    searchHistory.clear()
                    searchHistory.addAll(loaded.filter { it.isNotBlank() }.take(5))
                } catch (e: Exception) { }
            }
        }
    }

    private var searchQuery: String = initialSearch
    private val rows = mutableListOf<RecipeItemRow>()
    private var scroll: ScrollComponent? = null
    private var searchInput: UITextInput? = null
    private var historyContainer: UIContainer? = null

    private var allRecipes: List<RecipeItem> = emptyList()
    private var isLoading = true
    private var loadError = false
    private var loadingText: UIText? = null

    init {
        loadHistory()
        if (initialSearch.isNotBlank()) {
            addSearchToHistory(initialSearch)
        }
        setupUI()
        loadRecipes()
    }

    override fun afterInitialization() {
        super.afterInitialization()
        searchInput?.grabWindowFocus()
    }

    override fun initScreen(width: Int, height: Int) {
        super.initScreen(width, height)

        ScreenEvents.afterExtract(this).register { _, graphics, _, _, _ ->
            UIItemRenderer.renderQueued(graphics)
        }
    }

    private fun loadRecipes() {
        isLoading = false

        val remote = RecipeRepository.getRecipes()

        allRecipes = remote.map { r ->
            RecipeItem(
                name = r.name,
                itemStack = RecipeRepository.buildItemStack(r),
                itemId = r.id
            )
        }

        populateRows()
    }

    private fun updateLoadingText() {
        val s = scroll ?: return
        loadingText?.let { s.removeChild(it); loadingText = null }

        if (isLoading) {
            loadingText = UIText("Loading Recipes...").constrain {
                x = CenterConstraint()
                y = 8.gpx()
                textScale = 1.1f.gpx()
            }.setColor(Theme.textPrimary()) as UIText
            loadingText?.let { it childOf s }
        } else if (loadError && allRecipes.isEmpty()) {
            loadingText = UIText("Error loading recipes").constrain {
                x = CenterConstraint()
                y = 8.gpx()
                textScale = 1.1f.gpx()
            }.setColor(Theme.textPrimary()) as UIText
            loadingText?.let { it childOf s }
        }
    }

    private fun setupUI() {
        val w = 360.gpx()
        val h = 430.gpx()

        UIRoundedRectangle(13f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = w + 2.gpx()
            height = h + 2.gpx()
        }.setColor(Theme.glow(140)) childOf window

        UIRoundedRectangle(12f).constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = w
            height = h
        }.setColor(Theme.panelDark()) childOf window

        val content = UIContainer().constrain {
            x = CenterConstraint()
            y = CenterConstraint()
            width = w
            height = h
        } childOf window

        UIText("Recipes").constrain {
            x = CenterConstraint()
            y = 12.gpx()
            textScale = 1.8.gpx()
        }.setColor(Theme.accent()) childOf content

        val searchBar = createSearchBar() childOf content
        searchBar.constrain {
            x = 16.gpx()
            y = 40.gpx()
            width = 100.percent() - 32.gpx()
            height = 28.gpx()
        }

        scroll = ScrollComponent("", 4f).constrain {
            x = 16.gpx()
            y = 74.gpx()
            width = 100.percent() - 32.gpx()
            height = 214.gpx()
        } childOf content

        val hCont = UIContainer().constrain {
            x = 16.gpx()
            y = 298.gpx()
            width = 100.percent() - 32.gpx()
            height = 112.gpx()
        } childOf content
        historyContainer = hCont

        rebuildHistoryList()
        updateLoadingText()
    }

    private fun createSearchBar(): UIComponent {
        val container = UIContainer()

        val bg = UIRoundedRectangle(8f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.inputBackground()) childOf container

        val field = UITextInput("Search recipes...").constrain {
            x = 10.gpx()
            y = CenterConstraint()
            width = 100.percent() - 20.gpx()
        } childOf container

        field.setTextScale(1.0f.gpx())
        if (searchQuery.isNotBlank()) {
            field.setText(searchQuery)
        }

        field.onMouseClick {
            field.grabWindowFocus()
            it.stopPropagation()
        }
        field.onFocus { bg.colorTo(Theme.accent()) }
        field.onFocusLost { bg.colorTo(Theme.inputBackground()) }
        field.onUpdate { text ->
            searchQuery = text
            filterAndRebuild()
        }

        searchInput = field

        return container
    }

    private fun rebuildHistoryList() {
        val container = historyContainer ?: return
        container.children.toList().forEach { container.removeChild(it) }

        if (searchHistory.isEmpty()) return

        UIText("Recent Searches:").constrain {
            x = 0.gpx()
            y = 0.gpx()
            textScale = 1.0f.gpx()
        }.setColor(Theme.textPrimary()) childOf container

        val historyListContainer = UIContainer().constrain {
            x = 0.gpx()
            y = 16.gpx()
            width = 100.percent()
            height = 110.gpx()
        } childOf container

        var isFirst = true
        searchHistory.take(5).forEach { query ->
            createHistoryChip(query).constrain {
                width = 100.percent()
                height = 18.gpx()
                y = if (isFirst) 0.pixels() else gsib(3f)
            } childOf historyListContainer
            isFirst = false
        }
    }

    private fun createHistoryChip(query: String): UIComponent {
        val chip = UIContainer()

        val bg = UIRoundedRectangle(4f).constrain {
            width = 100.percent()
            height = 100.percent()
        }.setColor(Theme.card()) childOf chip

        UIText("• $query").constrain {
            x = 6.gpx()
            y = CenterConstraint()
            textScale = 0.95f.gpx()
        }.setColor(Theme.textPrimary()) childOf chip

        chip.onMouseEnter { bg.colorTo(Theme.cardHover()) }
        chip.onMouseLeave { bg.colorTo(Theme.card()) }
        chip.onMouseClick {
            if (searchQuery.isNotBlank()) {
                addSearchToHistory(searchQuery)
            }
            searchQuery = query
            searchInput?.setText(query)
            rebuildHistoryList()
            filterAndRebuild()
        }

        return chip
    }

    private fun populateRows() {
        rows.clear()

        val s = scroll ?: return

        s.clearChildren()
        updateLoadingText()

        if (isLoading || allRecipes.isEmpty()) return

        val filtered = filterRecipes(searchQuery)

        var first = true
        filtered.forEach { recipe ->
            val row = RecipeItemRow(recipe).constrain {
                width = 100.percent()
                height = 36.gpx()
                y = if (first) 0.pixels() else gsib(4f)
            } childOf s
            rows.add(row)
            first = false
        }

        s.scrollToTop()
    }

    private fun filterAndRebuild() {
        populateRows()
    }

    private fun filterRecipes(query: String): List<RecipeItem> {
        return if (query.isBlank()) {
            allRecipes
        } else {
            val lower = query.lowercase()
            allRecipes.filter {
                it.name.lowercase().contains(lower) || it.itemId.lowercase().contains(lower)
            }
        }
    }

    private inner class RecipeItemRow(
        val recipe: RecipeItem
    ) : UIContainer() {

        init {
            val bg = UIRoundedRectangle(6f).constrain {
                x = 4.gpx()
                y = 2.gpx()
                width = 100.percent() - 8.gpx()
                height = 100.percent() - 4.gpx()
            }.setColor(Theme.card()) childOf this

            UIText(recipe.name).constrain {
                x = 32.gpx()
                y = CenterConstraint()
                textScale = 1.25f.gpx()
            }.setColor(Theme.textPrimary()) childOf this

            UIItem(recipe.itemStack).constrain {
                x = 8.gpx()
                y = CenterConstraint()
            } childOf this

            onMouseEnter {
                bg.colorTo(Theme.cardHover())
            }
            onMouseLeave {
                bg.colorTo(Theme.card())
            }
            onMouseClick {
                if (this@RecipeGUI.searchQuery.isNotBlank()) {
                    addSearchToHistory(this@RecipeGUI.searchQuery)
                }
                sendCommand("viewrecipe ${recipe.itemId}")
                mc.execute { mc.setScreen(null) }
            }
        }
    }

    override fun shouldCloseOnEsc(): Boolean = true
    override fun isPauseScreen(): Boolean = false

    override fun onScreenClose() {
        rows.clear()
        scroll = null
        searchInput = null
        historyContainer = null
        loadingText = null
    }
}