package xyz.vexo.utils

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.item.ItemStack
import xyz.vexo.Vexo.mc
import net.minecraft.client.CameraType


object PlayerUtils {

    fun getHotbarSlot(i: Int): ItemStack? {
        if (! Inventory.isHotbarSlot(i)) return null
        val player = mc.player ?: return null
        return player.inventory.getItem(i)
    }

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

    fun isFirstPerson(): Boolean {
        if (getCameraPerspective().toString() == "FIRST_PERSON"){
            return true
        }
        return false
    }

    fun isThirdPerson(): Boolean {
        if (getCameraPerspective().toString() == "THIRD_PERSON_BACK"){
            return true
        }
        return false
    }
}