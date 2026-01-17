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
            "X: %.1f, Y: %.1f, Z: %.1f", pos.x, pos.y, pos.z
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
    return Vec3(player.x, player.y, player.z)
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

/**
 * Checks whether a position is within a certain radius of a given point.
 *
 * @param pos The position to check
 * @param x The X coordinate of the center point
 * @param y The Y coordinate of the center point
 * @param z The Z coordinate of the center point
 * @param radius The radius to check
 * @return boolean value whether the position is within the radius
 */
fun inRadius(
    pos: Vec3,
    x: Double, y: Double, z: Double,
    radius: Double
): Boolean {
    val dx = pos.x - x
    val dy = pos.y - y
    val dz = pos.z - z
    return dx * dx + dy * dy + dz * dz <= radius * radius
}


/**
 * Checks whether the own player is inside a 3D area (axis-aligned bounding box).
 *
 * @param pos The position to check
 * @param x1 X coordinate of the first corner
 * @param y1 Y coordinate of the first corner
 * @param z1 Z coordinate of the first corner
 * @param x2 X coordinate of the opposite corner
 * @param y2 Y coordinate of the opposite corner
 * @param z2 Z coordinate of the opposite corner
 * @return boolean value whether the player is inside the box
 */
fun inArea(
    pos: Vec3,
    x1: Double, y1: Double, z1: Double,
    x2: Double, y2: Double, z2: Double
): Boolean {
    val minX = minOf(x1, x2)
    val maxX = maxOf(x1, x2)
    val minY = minOf(y1, y2)
    val maxY = maxOf(y1, y2)
    val minZ = minOf(z1, z2)
    val maxZ = maxOf(z1, z2)

    return pos.x >= minX && pos.x <= maxX &&
            pos.y >= minY && pos.y <= maxY &&
            pos.z >= minZ && pos.z <= maxZ
}
