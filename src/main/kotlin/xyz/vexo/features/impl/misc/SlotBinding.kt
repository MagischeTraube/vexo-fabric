package xyz.vexo.features.impl.misc

import com.google.gson.JsonObject
import com.mojang.blaze3d.platform.InputConstants
import gg.essential.universal.UScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.inventory.Slot
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
import xyz.vexo.features.impl.misc.slotbinding.SlotPresetGui
import xyz.vexo.mixin.AbstractContainerScreenAccessor
import xyz.vexo.utils.drawLine2D
import xyz.vexo.utils.logError
import xyz.vexo.utils.modMessage
import java.awt.Color
import java.io.File

private const val SLOT_SIZE = 16

object SlotBinding : Module(
    name = "Slot Binding",
    description = "Pairs inventory slots with hotbar slots and swaps them on shift-click",
    toggled = false
) {
    private val bindKey = KeybindSetting("Bind", "Hold over a slot, release over a hotbar slot to pair them")
    private val alwaysShowLines by BooleanSetting("Always Show Lines", "Show pairing lines even when not hovering or holding shift", false)
    private val lineColor = ColorSetting("Line Color", "Color of the pairing lines", Color(99, 102, 241))

    private val managePresets = ButtonSetting("Presets", "Save and switch between named binding sets", "Manage") {
        UScreen.displayScreen(SlotPresetGui(mc.screen))
    }

    private val bindsFile = File(Vexo.configDir, "slotbinds.json")
    private val binds = linkedMapOf<Int, Int>()
    private val presets = linkedMapOf<String, Map<Int, Int>>()

    private var dragSourceSlot: Int? = null

    const val HOTBAR_MENU_SLOT_START = 36

    val bindings: Map<Int, Int> get() = binds

    fun presetNames(): List<String> = presets.keys.toList()

    fun savePreset(name: String) {
        presets[name] = binds.toMap()
        saveBinds()
    }

    fun loadPreset(name: String) {
        val preset = presets[name] ?: return
        binds.clear()
        binds.putAll(preset)
        saveBinds()
    }

    fun deletePreset(name: String) {
        if (presets.remove(name) != null) saveBinds()
    }

    fun renamePreset(oldName: String, newName: String): Boolean {
        if (newName.isBlank() || newName == oldName) return false
        if (!presets.containsKey(oldName) || presets.containsKey(newName)) return false

        val rebuilt = linkedMapOf<String, Map<Int, Int>>()
        presets.forEach { (name, binds) -> rebuilt[if (name == oldName) newName else name] = binds }
        presets.clear()
        presets.putAll(rebuilt)
        saveBinds()
        return true
    }

    @EventHandler
    fun onKey(event: KeybindEvent) {
        val bindKeyCode = bindKey.getCurrentValue()
        if (bindKeyCode == -1 || event.key.value != bindKeyCode) return
        dragSourceSlot = hoveredSlot()?.index
    }

    @EventHandler
    fun onKeyRelease(event: KeybindReleaseEvent) {
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
            saveBinds()
            modMessage("§aUnbound the hovered slot from hotbar slot ${hotbarIndex + 1}.")
            return
        }

        binds[sourceSlot] = hotbarIndex
        saveBinds()
        modMessage("§aPaired the hovered slot with hotbar slot ${hotbarIndex + 1}.")
    }

    @EventHandler
    fun onSlotClick(event: ContainerSlotClickEvent) {
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

    private fun unbindSlot(slotIndex: Int) {
        if (binds.remove(slotIndex) != null) {
            saveBinds()
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
        saveBinds()
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

    fun loadBinds() {
        try {
            if (!bindsFile.exists()) return
            val root = Vexo.gson.fromJson(bindsFile.readText(), JsonObject::class.java) ?: return

            binds.clear()
            root.getAsJsonObject("binds")?.let { binds.putAll(readBindMap(it)) }

            presets.clear()
            root.getAsJsonObject("presets")?.entrySet()?.forEach { (name, value) ->
                if (value.isJsonObject) presets[name] = readBindMap(value.asJsonObject)
            }
        } catch (e: Exception) {
            logError(e, this)
        }
    }

    private fun readBindMap(json: JsonObject): Map<Int, Int> {
        val result = linkedMapOf<Int, Int>()
        for ((key, value) in json.entrySet()) {
            val partnerMenuSlot = key.toIntOrNull() ?: continue
            val hotbarIndex = value.asInt
            if (hotbarIndex !in 0..8) continue
            result[partnerMenuSlot] = hotbarIndex
        }
        return result
    }

    private fun writeBindMap(map: Map<Int, Int>): JsonObject {
        val json = JsonObject()
        map.forEach { (partnerMenuSlot, hotbarIndex) -> json.addProperty(partnerMenuSlot.toString(), hotbarIndex) }
        return json
    }

    private fun saveBinds() {
        try {
            val root = JsonObject()
            root.add("binds", writeBindMap(binds))
            val presetsJson = JsonObject()
            presets.forEach { (name, map) -> presetsJson.add(name, writeBindMap(map)) }
            root.add("presets", presetsJson)
            bindsFile.writeText(Vexo.gson.toJson(root))
        } catch (e: Exception) {
            logError(e, this)
        }
    }
}
