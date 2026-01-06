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

fun inRadius(x: Double, y: Double, z: Double, radius: Double): Boolean {
    val playerPos = getOwnPlayerCoords() ?: return false
    val dx = playerPos.x - x
    val dy = playerPos.y - y
    val dz = playerPos.z - z

    return dx * dx + dy * dy + dz * dz <= radius * radius
}

/**
 * Checks whether the own player is inside a 3D area (axis-aligned bounding box).
 *
 * @param x1 X coordinate of the first corner
 * @param y1 Y coordinate of the first corner
 * @param z1 Z coordinate of the first corner
 * @param x2 X coordinate of the opposite corner
 * @param y2 Y coordinate of the opposite corner
 * @param z2 Z coordinate of the opposite corner
 *
 * @return boolean value whether the player is inside the box
 */
fun inArea(
    x1: Double, y1: Double, z1: Double,
    x2: Double, y2: Double, z2: Double
): Boolean {
    val pos = getOwnPlayerCoords() ?: return false

    val minX = minOf(x1, x2)
    val maxX = maxOf(x1, x2)
    val minY = minOf(y1, y2)
    val maxY = maxOf(y1, y2)
    val minZ = minOf(z1, z2)
    val maxZ = maxOf(z1, z2)

    return pos.x in minX..maxX &&
            pos.y in minY..maxY &&
            pos.z in minZ..maxZ
}
