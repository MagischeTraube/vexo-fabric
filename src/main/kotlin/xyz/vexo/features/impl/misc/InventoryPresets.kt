package xyz.vexo.features.impl.misc

import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.InputConstants
import gg.essential.universal.UScreen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import org.lwjgl.glfw.GLFW
import xyz.vexo.Vexo
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ButtonSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.KeybindSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ContainerSlotClickEvent
import xyz.vexo.events.impl.GuiRenderEvent
import xyz.vexo.events.impl.KeybindEvent
import xyz.vexo.events.impl.KeybindReleaseEvent
import xyz.vexo.features.Module
import xyz.vexo.features.impl.misc.inventorypresets.InventoryPresetGui
import xyz.vexo.mixin.AbstractContainerScreenAccessor
import xyz.vexo.utils.drawLine2D
import xyz.vexo.utils.logError
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import java.awt.Color
import java.io.File

private const val SLOT_SIZE = 16

object InventoryPresets : Module(
    name = "Inventory Presets",
    description = "Slot Binding and Loadouts in one module, each with switchable named presets",
    toggled = false
) {
    enum class Section { SLOT_BINDING, LOADOUTS }

    private val slotBindingEnabled = BooleanSetting("Slot Binding", "Enable the slot-to-hotbar binding sub-feature", true)
    private val loadoutsEnabled = BooleanSetting("Loadouts", "Enable the Loadouts keybind sub-feature", true)

    private val slotPresetsButton = ButtonSetting("Slot Binding Presets", "Save and switch between named binding sets", "Manage") {
        UScreen.displayScreen(InventoryPresetGui(mc.screen, Section.SLOT_BINDING))
    }
    private val loadoutPresetsButton = ButtonSetting("Loadout Presets", "Save and switch between named Loadout keybind sets", "Manage") {
        UScreen.displayScreen(InventoryPresetGui(mc.screen, Section.LOADOUTS))
    }

    private val bindKey = KeybindSetting("Bind", "Hold over a slot, release over a hotbar slot to pair them")
    private val alwaysShowLines by BooleanSetting("Always Show Lines", "Show pairing lines even when not hovering or holding shift", false)
    private val lineColor = ColorSetting("Line Color", "Color of the pairing lines", Color(99, 102, 241))


    private val lSlot1 = KeybindSetting("Loadout 1")
    private val lSlot2 = KeybindSetting("Loadout 2")
    private val lSlot3 = KeybindSetting("Loadout 3")
    private val lSlot4 = KeybindSetting("Loadout 4")
    private val lSlot5 = KeybindSetting("Loadout 5")
    private val lSlot6 = KeybindSetting("Loadout 6")
    private val lSlot7 = KeybindSetting("Loadout 7")
    private val lSlot8 = KeybindSetting("Loadout 8")
    private val lSlot9 = KeybindSetting("Loadout 9")
    private val lSlot10 = KeybindSetting("Loadout 10")
    private val lSlot11 = KeybindSetting("Loadout 11")
    private val lSlot12 = KeybindSetting("Loadout 12")
    private val lPrevPage = KeybindSetting("Loadout Prev Page")
    private val lNextPage = KeybindSetting("Loadout Next Page")

    init {
        listOf(lSlot1, lSlot2, lSlot3, lSlot4, lSlot5, lSlot6, lSlot7, lSlot8, lSlot9, lSlot10, lSlot11, lSlot12, lPrevPage, lNextPage)
            .forEach { it.dependsOn { loadoutsEnabled.getCurrentValue() } }
    }

    private val configFile = File(Vexo.configDir, "inventorypresets.json")

    private val binds = linkedMapOf<Int, Int>()
    private val slotPresets = linkedMapOf<String, Map<Int, Int>>()
    private var slotActive = ""

    private val loadoutPresets = linkedMapOf<String, Map<String, Int>>()
    private var loadoutActive = ""

    private var dragSourceSlot: Int? = null
    private var loadoutCooldownMs = 0L

    const val HOTBAR_MENU_SLOT_START = 36

    val bindings: Map<Int, Int> get() = binds

    private fun loadoutKeybinds(): Map<String, KeybindSetting> = linkedMapOf(
        "slot1" to lSlot1, "slot2" to lSlot2, "slot3" to lSlot3, "slot4" to lSlot4, "slot5" to lSlot5,
        "slot6" to lSlot6, "slot7" to lSlot7, "slot8" to lSlot8, "slot9" to lSlot9,
        "slot10" to lSlot10, "slot11" to lSlot11, "slot12" to lSlot12,
        "prevPage" to lPrevPage, "nextPage" to lNextPage
    )

    fun presetNames(section: Section): List<String> = when (section) {
        Section.SLOT_BINDING -> slotPresets.keys.toList()
        Section.LOADOUTS -> loadoutPresets.keys.toList()
    }

    fun activePreset(section: Section): String = when (section) {
        Section.SLOT_BINDING -> slotActive
        Section.LOADOUTS -> loadoutActive
    }

    fun savePreset(section: Section, name: String) {
        if (name.isBlank()) return
        when (section) {
            Section.SLOT_BINDING -> { slotPresets[name] = binds.toMap(); slotActive = name }
            Section.LOADOUTS -> { loadoutPresets[name] = snapshot(loadoutKeybinds()); loadoutActive = name }
        }
        persist()
    }

    fun loadPreset(section: Section, name: String) {
        when (section) {
            Section.SLOT_BINDING -> {
                val preset = slotPresets[name] ?: return
                binds.clear()
                binds.putAll(preset)
                slotActive = name
            }
            Section.LOADOUTS -> {
                val preset = loadoutPresets[name] ?: return
                apply(preset, loadoutKeybinds())
                loadoutActive = name
            }
        }
        persist()
    }

    fun deletePreset(section: Section, name: String) {
        val removed = when (section) {
            Section.SLOT_BINDING -> (slotPresets.remove(name) != null).also { if (it && slotActive == name) slotActive = "" }
            Section.LOADOUTS -> (loadoutPresets.remove(name) != null).also { if (it && loadoutActive == name) loadoutActive = "" }
        }
        if (removed) persist()
    }

    fun renamePreset(section: Section, oldName: String, newName: String): Boolean {
        val ok = when (section) {
            Section.SLOT_BINDING -> slotPresets.renameKey(oldName, newName).also { if (it && slotActive == oldName) slotActive = newName }
            Section.LOADOUTS -> loadoutPresets.renameKey(oldName, newName).also { if (it && loadoutActive == oldName) loadoutActive = newName }
        }
        if (ok) persist()
        return ok
    }

    private fun snapshot(keybinds: Map<String, KeybindSetting>): Map<String, Int> =
        keybinds.mapValues { it.value.getCurrentValue() }

    private fun apply(preset: Map<String, Int>, keybinds: Map<String, KeybindSetting>) {
        keybinds.forEach { (key, setting) -> setting.updateValue(preset[key] ?: -1) }
    }

    private fun <V> LinkedHashMap<String, V>.renameKey(oldName: String, newName: String): Boolean {
        if (newName.isBlank() || newName == oldName || oldName !in this || newName in this) return false
        val rebuilt = LinkedHashMap<String, V>()
        forEach { (name, value) -> rebuilt[if (name == oldName) newName else name] = value }
        clear()
        putAll(rebuilt)
        return true
    }

    @EventHandler
    fun onKeybind(event: KeybindEvent) {
        if (slotBindingEnabled.getCurrentValue()) handleBindKey(event)
        if (loadoutsEnabled.getCurrentValue()) handleLoadoutKey(event)
    }

    @EventHandler
    fun onKeybindRelease(event: KeybindReleaseEvent) {
        if (!slotBindingEnabled.getCurrentValue()) return

        val bindKeyCode = bindKey.getCurrentValue()
        if (bindKeyCode == -1 || event.key.value != bindKeyCode) return

        val sourceSlot = dragSourceSlot ?: return
        dragSourceSlot = null

        val targetSlot = hoveredSlot()?.index ?: return
        if (sourceSlot == targetSlot) {
            unbindSlot(sourceSlot)
            return
        }

        val hotbarIndex = targetSlot - HOTBAR_MENU_SLOT_START
        if (hotbarIndex !in 0..8) {
            modMessage("§cRelease the bind key while hovering a hotbar slot.")
            return
        }

        if (binds[sourceSlot] == hotbarIndex) {
            binds.remove(sourceSlot)
            persist()
            modMessage("§aUnbound the hovered slot from hotbar slot ${hotbarIndex + 1}.")
            return
        }

        binds[sourceSlot] = hotbarIndex
        persist()
        modMessage("§aPaired the hovered slot with hotbar slot ${hotbarIndex + 1}.")
    }

    @EventHandler
    fun onSlotClick(event: ContainerSlotClickEvent) {
        if (!slotBindingEnabled.getCurrentValue()) return
        if (event.input != ContainerInput.QUICK_MOVE) return

        val menu = (mc.screen as? InventoryScreen)?.menu ?: return

        binds[event.slotId]?.let { hotbarIndex ->
            event.cancel()
            swapWithHotbarSlot(menu, event.slotId, hotbarIndex)
            return
        }

        val hotbarIndex = event.slotId - HOTBAR_MENU_SLOT_START
        if (hotbarIndex !in 0..8) return
        val partnerSlot = binds.entries.firstOrNull { it.value == hotbarIndex }?.key ?: return

        event.cancel()
        swapWithHotbarSlot(menu, partnerSlot, hotbarIndex)
    }

    @EventHandler
    fun onRender(event: GuiRenderEvent) {
        if (!slotBindingEnabled.getCurrentValue()) return
        val screen = event.screen as? InventoryScreen ?: return
        val accessor = screen as AbstractContainerScreenAccessor
        val color = lineColor.getRGBA()

        for ((partnerSlot, hotbarIndex) in visiblePairs()) {
            val hotbar = screen.menu.slots.getOrNull(HOTBAR_MENU_SLOT_START + hotbarIndex) ?: continue
            val partner = screen.menu.slots.getOrNull(partnerSlot) ?: continue
            drawLine2D(
                event.context,
                centerX(accessor, partner), centerY(accessor, partner),
                centerX(accessor, hotbar), centerY(accessor, hotbar),
                color, 2f
            )
        }

        val source = dragSourceSlot?.let { screen.menu.slots.getOrNull(it) } ?: return
        drawLine2D(
            event.context,
            centerX(accessor, source), centerY(accessor, source),
            event.mouseX.toFloat(), event.mouseY.toFloat(),
            color, 2f
        )
    }

    private fun handleBindKey(event: KeybindEvent) {
        val bindKeyCode = bindKey.getCurrentValue()
        if (bindKeyCode == -1 || event.key.value != bindKeyCode) return
        dragSourceSlot = hoveredSlot()?.index
    }

    private fun handleLoadoutKey(event: KeybindEvent) {
        val screen = mc.screen as? AbstractContainerScreen<*> ?: return
        if (!screen.title.string.removeFormatting().contains("Loadouts")) return

        val now = System.currentTimeMillis()
        if (now < loadoutCooldownMs) return

        val keyCode = event.key.value
        val menu = screen.menu
        val player = mc.player ?: return

        val nextKey = lNextPage.getCurrentValue()
        val prevKey = lPrevPage.getCurrentValue()

        when {
            nextKey != -1 && keyCode == nextKey -> {
                val slot = findByName(menu, "Next Page") ?: return
                loadoutCooldownMs = now + 200L
                leftClick(menu.containerId, slot, player)
            }
            prevKey != -1 && keyCode == prevKey -> {
                val slot = findByName(menu, "Previous Page") ?: return
                loadoutCooldownMs = now + 200L
                leftClick(menu.containerId, slot, player)
            }
            else -> {
                val index = loadoutKeyIndex(keyCode)
                if (index == -1) return

                val target = collectLoadoutSlots(menu).getOrNull(index) ?: run {
                    modMessage("§cNo loadout at position ${index + 1}.")
                    return
                }
                if (isEmptyLoadout(target.item)) {
                    modMessage("§cLoadout ${index + 1} is empty.")
                    return
                }

                loadoutCooldownMs = now + 200L
                leftClick(menu.containerId, target.index, player)
            }
        }
    }

    private fun unbindSlot(slotIndex: Int) {
        if (binds.remove(slotIndex) != null) {
            persist()
            modMessage("§aUnbound the hovered slot.")
            return
        }

        val hotbarIndex = slotIndex - HOTBAR_MENU_SLOT_START
        val partners = binds.filterValues { it == hotbarIndex }.keys
        if (hotbarIndex !in 0..8 || partners.isEmpty()) {
            modMessage("§cThe hovered slot isn't bound.")
            return
        }

        partners.forEach { binds.remove(it) }
        persist()
        modMessage("§aUnbound ${partners.size} slot(s) from hotbar slot ${hotbarIndex + 1}.")
    }

    private fun visiblePairs(): List<Pair<Int, Int>> {
        if (alwaysShowLines || shiftDown() || bindKeyDown()) return binds.toList()

        val hoveredSlotIndex = hoveredSlot()?.index ?: return emptyList()
        val pairsFromHoveredSlot = binds[hoveredSlotIndex]?.let { listOf(hoveredSlotIndex to it) }.orEmpty()

        val hoveredHotbarIndex = hoveredSlotIndex - HOTBAR_MENU_SLOT_START
        if (hoveredHotbarIndex !in 0..8) return pairsFromHoveredSlot
        return pairsFromHoveredSlot + binds.filterValues { it == hoveredHotbarIndex }.map { it.key to it.value }
    }

    private fun centerX(accessor: AbstractContainerScreenAccessor, slot: Slot): Float =
        (accessor.vexoLeftPos() + slot.x + SLOT_SIZE / 2).toFloat()

    private fun centerY(accessor: AbstractContainerScreenAccessor, slot: Slot): Float =
        (accessor.vexoTopPos() + slot.y + SLOT_SIZE / 2).toFloat()

    private fun hoveredSlot(): Slot? =
        (mc.screen as? InventoryScreen as? AbstractContainerScreenAccessor)?.vexoHoveredSlot()

    private fun shiftDown(): Boolean =
        InputConstants.isKeyDown(mc.window, GLFW.GLFW_KEY_LEFT_SHIFT) ||
            InputConstants.isKeyDown(mc.window, GLFW.GLFW_KEY_RIGHT_SHIFT)

    private fun bindKeyDown(): Boolean {
        val bindKeyCode = bindKey.getCurrentValue()
        return bindKeyCode != -1 && InputConstants.isKeyDown(mc.window, bindKeyCode)
    }

    private fun swapWithHotbarSlot(menu: AbstractContainerMenu, menuSlot: Int, hotbarIndex: Int) {
        val player = mc.player ?: return
        mc.execute {
            ContainerSlotClickEvent.bypass = true
            try {
                mc.gameMode?.handleContainerInput(menu.containerId, menuSlot, hotbarIndex, ContainerInput.SWAP, player)
            } finally {
                ContainerSlotClickEvent.bypass = false
            }
        }
    }

    private fun leftClick(containerId: Int, slotIndex: Int, player: Player) {
        mc.gameMode?.handleContainerInput(containerId, slotIndex, 0, ContainerInput.PICKUP, player)
    }

    private fun collectLoadoutSlots(menu: AbstractContainerMenu) =
        menu.slots
            .filter { it.index < menu.slots.size - 36 && it.hasItem() && isLoadout(it.item) }
            .sortedBy { it.index }

    private fun isLoadout(stack: ItemStack): Boolean = "click to edit" in loreOf(stack)

    private fun isEmptyLoadout(stack: ItemStack): Boolean {
        val lore = loreOf(stack)
        return "must customize" in lore || "click to equip" !in lore
    }

    private fun findByName(menu: AbstractContainerMenu, name: String): Int? =
        menu.slots.firstOrNull {
            it.hasItem() && it.item.hoverName.string.removeFormatting().contains(name, ignoreCase = true)
        }?.index

    private fun loreOf(stack: ItemStack): String =
        stack.get(DataComponents.LORE)?.styledLines()
            ?.joinToString("\n") { it.string.removeFormatting() }
            ?.lowercase()
            .orEmpty()

    private fun loadoutKeyIndex(keyCode: Int): Int {
        if (keyCode == -1) return -1
        val slots = listOf(lSlot1, lSlot2, lSlot3, lSlot4, lSlot5, lSlot6, lSlot7, lSlot8, lSlot9, lSlot10, lSlot11, lSlot12)
        return slots.indexOfFirst { it.getCurrentValue() == keyCode }
    }


    fun load() {
        try {
            if (!configFile.exists()) return
            val root = Vexo.gson.fromJson(configFile.readText(), JsonObject::class.java) ?: return

            root.getAsJsonObject("slotBinding")?.let { sb ->
                slotActive = sb.get("active")?.asString ?: ""
                binds.clear()
                sb.getAsJsonObject("binds")?.let { binds.putAll(readIntMap(it)) }
                slotPresets.clear()
                sb.getAsJsonObject("presets")?.entrySet()?.forEach { (name, value) ->
                    if (value.isJsonObject) slotPresets[name] = readIntMap(value.asJsonObject)
                }
            }

            readKeybindSection(root.getAsJsonObject("loadouts"), loadoutPresets) { loadoutActive = it }
        } catch (e: Exception) {
            logError(e, this)
        }
    }

    private fun persist() {
        try {
            val root = JsonObject()

            val slotBinding = JsonObject()
            slotBinding.addProperty("active", slotActive)
            slotBinding.add("binds", writeIntMap(binds))
            val slotPresetsJson = JsonObject()
            slotPresets.forEach { (name, map) -> slotPresetsJson.add(name, writeIntMap(map)) }
            slotBinding.add("presets", slotPresetsJson)
            root.add("slotBinding", slotBinding)

            root.add("loadouts", writeKeybindSection(loadoutActive, loadoutPresets))

            configFile.writeText(Vexo.gson.toJson(root))
        } catch (e: Exception) {
            logError(e, this)
        }
    }

    private fun writeKeybindSection(active: String, presets: Map<String, Map<String, Int>>): JsonObject {
        val section = JsonObject()
        section.addProperty("active", active)
        val presetsJson = JsonObject()
        presets.forEach { (name, map) ->
            val mapJson = JsonObject()
            map.forEach { (key, value) -> mapJson.addProperty(key, value) }
            presetsJson.add(name, mapJson)
        }
        section.add("presets", presetsJson)
        return section
    }

    private inline fun readKeybindSection(
        section: JsonObject?,
        target: LinkedHashMap<String, Map<String, Int>>,
        setActive: (String) -> Unit
    ) {
        section ?: return
        setActive(section.get("active")?.asString ?: "")
        target.clear()
        section.getAsJsonObject("presets")?.entrySet()?.forEach { (name, value) ->
            if (!value.isJsonObject) return@forEach
            val map = linkedMapOf<String, Int>()
            value.asJsonObject.entrySet().forEach { (key, element) ->
                if (element.isJsonPrimitive && element.asJsonPrimitive.isNumber) map[key] = element.asInt
            }
            target[name] = map
        }
    }

    private fun readIntMap(json: JsonObject): Map<Int, Int> {
        val result = linkedMapOf<Int, Int>()
        for ((key, value) in json.entrySet()) {
            val partnerMenuSlot = key.toIntOrNull() ?: continue
            val hotbarIndex = value.asInt
            if (hotbarIndex !in 0..8) continue
            result[partnerMenuSlot] = hotbarIndex
        }
        return result
    }

    private fun writeIntMap(map: Map<Int, Int>): JsonObject {
        val json = JsonObject()
        map.forEach { (partnerMenuSlot, hotbarIndex) -> json.addProperty(partnerMenuSlot.toString(), hotbarIndex) }
        return json
    }
}
