package xyz.vexo.features.impl.dungeons

import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.getDungeonFloor
import xyz.vexo.utils.inArea
import xyz.vexo.utils.inRadius
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.sendCommand

object PositionalMessages : Module (
    name = "Positional Messages",
    description = "Sends a Message when standing on specific blocks in M7"
){
    private val radius by SliderSetting(
        name = "Positional Message radius",
        description = "How far you can be away from the Specific coords",
        default = 1.5,
        min = 0.5,
        max = 5.0,
        increment = 0.1)

    private val simonSays by BooleanSetting( name = "Simon Says")

    private val hee2 by BooleanSetting( name = "High EE2" )

    private val ee3 by BooleanSetting( name = "EE3" )

    private val hee3 by BooleanSetting( name = "High EE3" )

    private val insideCore by BooleanSetting( name = "Inside Core" )

    private val outsideCore by BooleanSetting( name = "Outside Core" )

    private val mid by BooleanSetting("atMid")


    private val atSimonSays get() = inRadius(108.0, 120.0, 93.0, radius)
    private val atHee2 get() = inRadius(60.0, 132.0, 140.0, radius)
    private val atEe3 get() = inRadius(2.0, 109.0, 104.0, radius)
    private val atHee3 get() = inRadius(18.0, 121.0, 91.0, radius)
    private val atInsideCore get() = inArea(50.0, 116.0, 58.0, 58.0, 114.0, 55.0)
    private val atOutsideCore get() = inRadius(54.0, 115.0, 51.0, radius)
    private val atMid get() = inArea(61.0, 64.0, 83.0, 47.0, 68.0, 69.0)

    private var atSimonSaysSent = false
    private var atHee2Sent = false
    private var atEe3Sent = false
    private var atHee3Sent = false
    private var atInsideCoreSent = false
    private var atOutsideCoreSent = false
    private var atMidSent = false

    @EventHandler
    fun onTick(event: ClientTickEvent){
        val floor = getDungeonFloor()
        if (floor != "M7" && floor != "F7") return

        if (atSimonSays && !atSimonSaysSent && simonSays) {
            sendCommand("pc at SimonSays")
            atSimonSaysSent = true
        }
        if (!atSimonSays) atSimonSaysSent = false

        if (atHee2 && !atHee2Sent && hee2) {
            sendCommand("pc at High EE2")
            atHee2Sent = true
        }
        if (!atHee2) atHee2Sent = false

        if (atEe3 && !atEe3Sent && ee3) {
            sendCommand("pc at EE3")
            atEe3Sent = true
        }
        if (!atEe3) atEe3Sent = false

        if (atHee3 && !atHee3Sent && hee3) {
            sendCommand("pc at High EE3")
            atHee3Sent = true
        }
        if (!atHee3) atHee3Sent = false

        if (atInsideCore && !atInsideCoreSent && insideCore) {
            sendCommand("pc at Inside Core")
            atInsideCoreSent = true
        }
        if (!atInsideCore) atInsideCoreSent = false

        if (atOutsideCore && !atOutsideCoreSent && outsideCore) {
            sendCommand("pc at Outside Core")
            atOutsideCoreSent = true
        }
        if (!atOutsideCore) atOutsideCoreSent = false

        if (atMid && !atMidSent && mid) {
            sendCommand("pc at Mid")
            atMidSent = true
        }
        if (!atMid) atMidSent = false
    }
}

