package xyz.vexo.features.impl.dungeons

import net.minecraft.world.phys.Vec3

import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.DungeonUtils
import xyz.vexo.utils.inArea
import xyz.vexo.utils.inRadius
import xyz.vexo.utils.sendCommand
import xyz.vexo.utils.getOwnPlayerCoords

object PositionalMessages : Module(
    name = "Positional Messages",
    description = "Sends a Message when standing on specific blocks in M7"
) {

    private val radius by SliderSetting(
        name = "Trigger radius (blocks)",
        description = "How far you can be away from the specific coords",
        default = 1.5,
        min = 0.5,
        max = 5.0,
        increment = 0.1
    )

    private val simonSays by BooleanSetting( name = "Simon Says")

    private val hee2 by BooleanSetting( name = "High EE2" )

    private val ee3 by BooleanSetting( name = "EE3" )

    private val hee3 by BooleanSetting( name = "High EE3" )

    private val insideCore by BooleanSetting( name = "Inside Core" )

    private val outsideCore by BooleanSetting( name = "Outside Core" )

    private val mid by BooleanSetting("atMid")

    private data class SpotDef(
        val message: String,
        val settingEnabled: () -> Boolean,
        val check: (Vec3) -> Boolean
    )


    private val spotDefs = listOf(
        SpotDef(
            message = "Simon Says",
            settingEnabled = { simonSays },
            check = { pos -> inRadius(pos, 108.0, 120.0, 93.0, radius) }
        ),

        SpotDef(
            message = "High EE2",
            settingEnabled = { hee2 },
            check = { pos -> inRadius(pos, 60.0, 132.0, 140.0, radius) }
        ),

        SpotDef(
            message = "EE3",
            settingEnabled = { ee3 },
            check = { pos -> inRadius(pos,2.0, 109.0, 104.0, radius) }
        ),

        SpotDef(
            message = "High EE3",
            settingEnabled = { hee3 },
            check = { pos -> inRadius(pos, 18.0, 121.0, 91.0, radius) }
        ),

        SpotDef(
            message = "Inside Core",
            settingEnabled = { insideCore },
            check = { pos -> inArea(pos, 50.0, 116.0, 58.0, 58.0, 114.0, 55.0) }
        ),

        SpotDef(
            message = "Outside Core",
            settingEnabled = { outsideCore },
            check = { pos -> inRadius(pos, 54.0, 115.0, 51.0, radius) }
        ),

        SpotDef(
            message = "Mid",
            settingEnabled = { mid },
            check = { pos -> inArea(pos, 61.0, 64.0, 83.0, 47.0, 68.0, 69.0) }
        )
    )

    private val sentSpot = BooleanArray(spotDefs.size)

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        val floor = DungeonUtils.getDungeonFloor()
        if (floor != "M7" && floor != "F7") return

        val playerPos = getOwnPlayerCoords() ?: return
        var foundSpot = false

        spotDefs.forEachIndexed { index, spot ->
            if (foundSpot) {
                sentSpot[index] = false
                return@forEachIndexed
            }

            val atSpot = spot.check(playerPos)

            if (atSpot && spot.settingEnabled()) {
                if (!sentSpot[index]) {
                    sendCommand("pc at ${spot.message}")
                    sentSpot[index] = true
                }
                foundSpot = true
            } else {
                sentSpot[index] = false
            }
        }
    }


    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        sentSpot.fill(false)
    }
}

