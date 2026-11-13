package xyz.vexo.utils

import java.awt.Color

/**
 * Returns the color associated with the given rarity.
 *
 * @param rarity The rarity to get the color for.
 * @return The color associated with the given rarity.
 */
fun rarityColor(rarity: String?): Color {
    return when (rarity?.uppercase()) {
        "COMMON" -> Color(0xFFFFFF)
        "UNCOMMON" -> Color(0x55FF55)
        "RARE" -> Color(0x5555FF)
        "EPIC" -> Color(0xAA00AA)
        "LEGENDARY" -> Color(0xFFAA00)
        "MYTHIC" -> Color(0xFF55FF)
        "SPECIAL", "VERY SPECIAL" -> Color(0xFF5555)
        "DIVINE" -> Color(0x55FFFF)
        "ULTIMATE" -> Color(0xAA0000)
        else -> Color(0xAAAAAA)
    }
}