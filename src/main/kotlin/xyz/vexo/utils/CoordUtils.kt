package xyz.vexo.utils

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.phys.Vec3
import xyz.vexo.Vexo.mc

/**
 * Get all player coordinates (real players only)
 *
 * @return coords as map
 */
fun getAllPlayerCoords(): Map<String, Vec3> {
    val playerCoords = mutableMapOf<String, Vec3>()
    val level: ClientLevel = mc.level ?: return playerCoords

    for (player in level.players()) {
        if (player.uuid.version() != 4) continue

        val playerName = player.name.string
        val position = player.position()
        playerCoords[playerName] = position
    }

    return playerCoords
}

/**
 * Get all player coordinates as formatted strings
 *
 * @return formattedCoords (X: %.1f, Y: %.1f, Z: %.1f)
 */
fun getAllPlayerCoordsFormat(): Map<String, String> {
    val formattedCoords = mutableMapOf<String, String>()
    val allCoords = getAllPlayerCoords()

    for ((name, pos) in allCoords) {
        val formatted = String.format(
            "X: %.1f, Y: %.1f, Z: %.1f",
            pos.x, pos.y, pos.z
        )
        formattedCoords[name] = formatted
    }

    return formattedCoords
}

/**
 * Get own player coordinates
 *
 * @return Vec3 or null if not available
 */
fun getOwnPlayerCoords(): Vec3? {
    val player = mc.player ?: return null
    return getAllPlayerCoords()[player.name.string]
}

/**
 * Get own player coordinates as formatted string
 *
 * @return formatted string or null
 */
fun getOwnPlayerCoordsFormat(): String? {
    val player = mc.player ?: return null
    return getAllPlayerCoordsFormat()[player.name.string]
}