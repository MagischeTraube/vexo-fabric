package xyz.vexo.features.impl.misc

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerInput
import net.minecraft.world.item.ItemStack
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.KeybindSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.KeybindEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting

/**
 * Keybinds for Hypixel's new "Loadouts" GUI (the successor to the Wardrobe's armor-set page).
 *
 * Unlike the Wardrobe (a column of armor pieces + an equip row), each loadout here is a **single**
 * slot: left-click equips it, right-click edits it. Loadouts can be renamed, so we never match on
 * names — we find the loadout slots by their equip/edit lore (both configured and empty loadouts carry
 * "Right-click to edit") and map keybind N to the N-th loadout **in slot order**, i.e. its physical
 * position in the grid.
 */
object Loadouts : Module(
    name = "Loadouts",
    description = "Equip loadouts in the new Loadouts GUI with keybinds",
    toggled = false
) {
    private val slot1 = KeybindSetting("Loadout 1")
    private val slot2 = KeybindSetting("Loadout 2")
    private val slot3 = KeybindSetting("Loadout 3")
    private val slot4 = KeybindSetting("Loadout 4")
    private val slot5 = KeybindSetting("Loadout 5")
    private val slot6 = KeybindSetting("Loadout 6")
    private val slot7 = KeybindSetting("Loadout 7")
    private val slot8 = KeybindSetting("Loadout 8")
    private val slot9 = KeybindSetting("Loadout 9")
    private val prevPage = KeybindSetting("Prev Page")
    private val nextPage = KeybindSetting("Next Page")

    private var nextAllowedActionAtMs = 0L

    @EventHandler
    fun onKey(event: KeybindEvent) {
        val screen = mc.screen as? AbstractContainerScreen<*> ?: return
        if (!screen.title.string.removeFormatting().contains("Loadouts")) return

        val now = System.currentTimeMillis()
        if (now < nextAllowedActionAtMs) return

        val keyCode = event.key.value
        val menu = screen.menu
        val player = mc.player ?: return

        val nextKey = nextPage.getCurrentValue()
        val prevKey = prevPage.getCurrentValue()

        when {
            nextKey != -1 && keyCode == nextKey -> {
                val slot = findByName(menu, "Next Page") ?: return
                nextAllowedActionAtMs = now + 200L
                leftClick(menu.containerId, slot, player)
            }
            prevKey != -1 && keyCode == prevKey -> {
                val slot = findByName(menu, "Previous Page") ?: return
                nextAllowedActionAtMs = now + 200L
                leftClick(menu.containerId, slot, player)
            }
            else -> {
                val index = loadoutKeyIndex(keyCode)
                if (index == -1) return

                val target = collectLoadoutSlots(menu).getOrNull(index) ?: run {
                    modMessage("§cKein Loadout an Position ${index + 1}.")
                    return
                }
                if (isEmptyLoadout(target.item)) {
                    modMessage("§cLoadout ${index + 1} ist leer.")
                    return
                }

                nextAllowedActionAtMs = now + 200L
                leftClick(menu.containerId, target.index, player)
            }
        }
    }

    /** A normal left-click on [slotIndex] — Hypixel reads it as "equip loadout" / "next page". */
    private fun leftClick(containerId: Int, slotIndex: Int, player: Player) {
        mc.gameMode?.handleContainerInput(containerId, slotIndex, 0, ContainerInput.PICKUP, player)
    }

    /** Loadout slots of the top container (excludes the player inventory), in grid/slot order. */
    private fun collectLoadoutSlots(menu: AbstractContainerMenu) =
        menu.slots
            .filter { it.index < menu.slots.size - 36 && it.hasItem() && isLoadout(it.item) }
            .sortedBy { it.index }

    /** Both configured and empty loadouts carry a "Right-click to edit" line — decoration/borders don't. */
    private fun isLoadout(stack: ItemStack): Boolean = "click to edit" in loreOf(stack)

    /** Empty loadouts read "You must customize this loadout before you can equip it!" (no equip line). */
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
        val slots = listOf(slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9)
        return slots.indexOfFirst { it.getCurrentValue() == keyCode }
    }
}
