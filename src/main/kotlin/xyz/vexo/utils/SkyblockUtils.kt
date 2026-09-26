package xyz.vexo.utils

/**
 * Returns the color associated with the given rarity.
 *
 * @param rarity The rarity to get the color for.
 * @return The color associated with the given rarity.
 */
fun rarityColor(rarity: String?): java.awt.Color {
    return when (rarity?.uppercase()) {
        "COMMON" -> java.awt.Color(0xFFFFFF)
        "UNCOMMON" -> java.awt.Color(0x55FF55)
        "RARE" -> java.awt.Color(0x5555FF)
        "EPIC" -> java.awt.Color(0xAA00AA)
        "LEGENDARY" -> java.awt.Color(0xFFAA00)
        "MYTHIC" -> java.awt.Color(0xFF55FF)
        "SPECIAL" -> java.awt.Color(0xFF5555)
        "VERY SPECIAL" -> java.awt.Color(0xFF5555)
        "DIVINE" -> java.awt.Color(0x55FFFF)
        else -> java.awt.Color(0xAAAAAA)
    }
}
