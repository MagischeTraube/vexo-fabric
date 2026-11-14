package xyz.vexo.utils

import kotlin.jvm.optionals.getOrNull
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.PacketReceiveEvent
import xyz.vexo.events.impl.ServerLeaveEvent
import xyz.vexo.events.impl.WorldJoinEvent

enum class Location(val displayName: String) {
    BackwaterBayou("Backwater Bayou"),
    CrimsonIsle("Crimson Isle"),
    CrystalHollows("Crystal Hollows"),
    DarkAuction("Dark Auction"),
    DeepCaverns("Deep Caverns"),
    Dungeon("Catacombs"),
    DungeonHub("Dungeon Hub"),
    DwarvenMines("Dwarven Mines"),
    End("The End"),
    Garden("Garden"),
    GoldMine("Gold Mine"),
    Hub("Hub"),
    JerryWorkshop("Jerry's Workshop"),
    Kuudra("Kuudra"),
    LotusAtoll("Lotus Atoll"),
    Mineshaft("Mineshaft"),
    MoongladeMarsh("Moonglade Marsh"),
    PrivateIsland("Private Island"),
    Rift("The Rift"),
    Safari("Safari"),
    SpiderDen("Spider's Den"),
    ThePark("The Park"),
    TorrhusCanyon("Torrhus Canyon"),
    Unknown("Unknown");

    companion object {
        private val byLength = entries
            .filter { it != Unknown }
            .sortedByDescending { it.displayName.length }

        fun fromTabLine(line: String): Location =
            byLength.firstOrNull { line.contains(it.displayName, ignoreCase = true) } ?: Unknown
    }
}

/** Dungeon floors as shown on the scoreboard: Entrance, Floors 1-7 and Master Mode 1-7. */
enum class DungeonFloor {
    E,
    F1, F2, F3, F4, F5, F6, F7,
    M1, M2, M3, M4, M5, M6, M7;

    /** Floor number (0 for the Entrance). */
    val number: Int get() = name.drop(1).toIntOrNull() ?: 0

    val isMaster: Boolean get() = name.startsWith("M")

    companion object {
        fun fromId(id: String): DungeonFloor? = entries.firstOrNull { it.name == id }
    }
}

/** The dungeon floor or Kuudra tier a run is (or was last seen) taking place in. */
sealed interface RunLocation {
    data class Dungeon(val floor: DungeonFloor) : RunLocation
    data class Kuudra(val tier: Int) : RunLocation
}

object LocationUtils {

    private val AREA_PREFIXES = listOf("Area: ", "Dungeon: ")

    private val catacombsFloorRegex = Regex("""The Catacombs \((E|[FM][1-7])\)""")
    private val kuudraTierRegex = Regex("""Kuudra's Hollow \(T([1-5])\)""")

    var location: Location = Location.Unknown
        private set

    val isLocationKnown: Boolean get() = location != Location.Unknown

    /**
     * Last floor / tier seen in a sidebar team update. Survives world changes on purpose: packets are
     * handled on the network thread and can arrive before the join event fires on the main thread.
     */
    @Volatile private var lastSeenRun: RunLocation? = null

    /** Current dungeon floor, or null outside of dungeons. */
    val dungeonFloor: DungeonFloor?
        get() = if (isIn(Location.Dungeon)) (lastSeenRun as? RunLocation.Dungeon)?.floor else null

    val isMasterMode: Boolean get() = dungeonFloor?.isMaster == true

    fun isIn(vararg locations: Location): Boolean = location in locations

    /** Parses a floor from a sidebar line, ignoring queue lines. */
    fun parseFloor(text: String): DungeonFloor? =
        if ("Queue" in text) null else catacombsFloorRegex.find(text)?.groupValues?.get(1)?.let(DungeonFloor::fromId)

    /** Parses a Kuudra tier (1-5) from a sidebar line. */
    fun parseKuudraTier(text: String): Int? =
        kuudraTierRegex.find(text)?.groupValues?.get(1)?.toInt()

    private fun detectRun(text: String): RunLocation? {
        if ("Queue" in text) return null
        parseFloor(text)?.let { return RunLocation.Dungeon(it) }
        parseKuudraTier(text)?.let { return RunLocation.Kuudra(it) }
        return null
    }

    /**
     * Floor / tier from the cached team packets; the sidebar is only read as a fallback. Callers may run on the
     * network thread, where reading the scoreboard can race with the main thread, so that read may fail.
     */
    fun currentRunLocation(): RunLocation? = lastSeenRun
        ?: runCatching { ScoreboardUtils.getEntries().firstNotNullOfOrNull { detectRun(it) } }.getOrNull()

    /** Current dungeon floor from the cache, with the scoreboard as fallback, or null if not in a dungeon. */
    fun currentFloor(): DungeonFloor? = (currentRunLocation() as? RunLocation.Dungeon)?.floor

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        location = Location.Unknown
    }

    @EventHandler
    fun onLeave(event: ServerLeaveEvent) {
        location = Location.Unknown
    }

    @EventHandler
    fun onPacket(event: PacketReceiveEvent) {
        when (val packet = event.packet) {
            is ClientboundPlayerInfoUpdatePacket -> handleTablist(packet)
            is ClientboundSetPlayerTeamPacket -> handleTeam(packet)
        }
    }

    private fun handleTablist(packet: ClientboundPlayerInfoUpdatePacket) {
        if (isLocationKnown) return
        if (ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME !in packet.actions()) return

        val line = packet.entries()
            .mapNotNull { it.displayName?.string }
            .firstOrNull { text -> AREA_PREFIXES.any { text.startsWith(it) } }
            ?: return

        val found = Location.fromTabLine(line)
        if (found != Location.Unknown) {
            location = found
        }
    }

    private fun handleTeam(packet: ClientboundSetPlayerTeamPacket) {
        val params = packet.parameters.getOrNull() ?: return
        val text = (params.playerPrefix.string + params.playerSuffix.string).removeFormatting()
        detectRun(text)?.let { lastSeenRun = it }
    }
}
