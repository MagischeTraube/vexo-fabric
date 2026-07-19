package xyz.vexo.features.impl.misc

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.world.inventory.ContainerInput
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.KeybindSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.KeybindEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.sendCommand

object Wardrobe : Module(
    name = "Wardrobe",
    description = "Quickly switch armor sets in the Wardrobe using keybinds",
    toggled = false
) {
    private val noUnequipEquipped = BooleanSetting("No Unequip Equipped", "Don't click a slot that is already equipped")

    private val slot1 = KeybindSetting("Slot 1")
    private val slot2 = KeybindSetting("Slot 2")
    private val slot3 = KeybindSetting("Slot 3")
    private val slot4 = KeybindSetting("Slot 4")
    private val slot5 = KeybindSetting("Slot 5")
    private val slot6 = KeybindSetting("Slot 6")
    private val slot7 = KeybindSetting("Slot 7")
    private val slot8 = KeybindSetting("Slot 8")
    private val slot9 = KeybindSetting("Slot 9")
    private val prevPage = KeybindSetting("Prev Page")
    private val nextPage = KeybindSetting("Next Page")
    private val unequip = KeybindSetting("Unequip")
    private val openWardrobe = KeybindSetting("Open Wardrobe")


    private val WARDROBE_PATTERN = Regex("""\((\d+)/(\d+)\)""")
    private val EQUIPPED_PATTERN = Regex("""Slot \d+: Equipped""")

    private var nextAllowedActionAtMs = 0L

    @EventHandler
    fun onKey(event: KeybindEvent) {
        val openKey = openWardrobe.getCurrentValue()
        if (openKey != -1 && event.key.value == openKey && mc.screen == null) {
            sendCommand("wardrobe")
            return
        }

        val screen = mc.screen as? AbstractContainerScreen<*> ?: return
        val title = screen.title.string.removeFormatting()
        if (!title.contains("Wardrobe") && !title.contains("Armor Sets")) return
        val match = WARDROBE_PATTERN.find(title) ?: return

        val current = match.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: return
        val total = match.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return

        val now = System.currentTimeMillis()
        if (now < nextAllowedActionAtMs) return

        val keyCode = event.key.value
        val equippedIndex = findEquippedIndex(screen)

        val nextPageKey = nextPage.getCurrentValue()
        val prevPageKey = prevPage.getCurrentValue()
        val unequipKey = unequip.getCurrentValue()

        val slotIndex: Int

        when {
            nextPageKey != -1 && keyCode == nextPageKey -> {
                if (current >= total) return
                slotIndex = 53
            }
            prevPageKey != -1 && keyCode == prevPageKey -> {
                if (current <= 1) return
                slotIndex = 45
            }
            unequipKey != -1 && keyCode == unequipKey -> {
                if (equippedIndex == -1) return
                slotIndex = equippedIndex
            }
            else -> {
                val keyIndex = wardrobeKeyIndex(keyCode)
                if (keyIndex == -1) return
                slotIndex = 36 + keyIndex
                if (noUnequipEquipped.getCurrentValue() && equippedIndex == slotIndex) {
                    modMessage("§cArmor already equipped.")
                    return
                }
            }
        }

        nextAllowedActionAtMs = now + 200L

        val player = mc.player ?: return
        mc.gameMode?.handleContainerInput(screen.menu.containerId, slotIndex, 0, ContainerInput.PICKUP, player)
    }

    private fun findEquippedIndex(screen: AbstractContainerScreen<*>): Int {
        for (slot in screen.menu.slots) {
            if (!slot.hasItem()) continue
            if (EQUIPPED_PATTERN.matches(slot.item.hoverName.string.removeFormatting())) return slot.index
        }
        return -1
    }

    private fun wardrobeKeyIndex(keyCode: Int): Int {
        if (keyCode == -1) return -1
        val slots = listOf(slot1, slot2, slot3, slot4, slot5, slot6, slot7, slot8, slot9)
        return slots.indexOfFirst { it.getCurrentValue() == keyCode }
    }
}
