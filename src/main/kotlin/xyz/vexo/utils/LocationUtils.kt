package xyz.vexo.utils

import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
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

object LocationUtils {

    private val AREA_PREFIXES = listOf("Area: ", "Dungeon: ")

    var location: Location = Location.Unknown
        private set

    val isLocationKnown: Boolean get() = location != Location.Unknown

    fun isIn(vararg locations: Location): Boolean = location in locations

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
        val packet = event.packet as? ClientboundPlayerInfoUpdatePacket ?: return
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
}