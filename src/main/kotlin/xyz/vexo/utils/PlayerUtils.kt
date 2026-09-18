package xyz.vexo.utils

import net.minecraft.client.CameraType
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import xyz.vexo.Vexo.mc

object PlayerUtils {
    /**
     * Returns the item stack in the specified hotbar slot.
     *
     * @param i the index of the hotbar slot (0-8)
     * @return the item stack in the specified hotbar slot, or null if the slot is invalid
     */
    fun getHotbarSlot(i: Int): ItemStack? {
        if (! Inventory.isHotbarSlot(i)) return null
        val player = mc.player ?: return null
        return player.inventory.getItem(i)
    }

    /**
     * Returns the first hotbar slot that matches the given predicate.
     *
     * @param predicate the predicate to test each hotbar slot
     * @return the index of the first matching hotbar slot, or null if no match is found
     */
    fun findHotbarSlot(predicate: (ItemStack) -> Boolean): Int? {
        return (0 .. 8).firstOrNull { idx ->
            val stack = getHotbarSlot(idx) ?: return@firstOrNull false
            if (stack.isEmpty) return@firstOrNull false
            predicate(stack)
        }
    }

    /**
     * Returns the current camera perspective of the user.
     *
     * @return FIRST_PERSON, THIRD_PERSON_BACK, THIRD_PERSON_FRONT
     */
    fun getCameraPerspective(): CameraType = mc.options.cameraType

    /**
     * Returns whether the current camera perspective is first person.
     *
     * @return true if the camera perspective is first person, false otherwise
     */
    fun isFirstPerson(): Boolean {
        return getCameraPerspective().toString() == "FIRST_PERSON"
    }

    /**
     * Returns whether the current camera perspective is third person.
     *
     * @return true if the camera perspective is third person, false otherwise
     */
    fun isThirdPerson(): Boolean {
        return getCameraPerspective().toString() == "THIRD_PERSON_BACK"
    }
}