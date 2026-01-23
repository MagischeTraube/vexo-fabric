package xyz.vexo.features.impl.dungeons

import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.PartyUtils
import xyz.vexo.utils.runAfterServerTicks
import xyz.vexo.utils.sendCommand

object DungeonRequeue : Module(
    name = "Auto Dungeon requeue",
    description = "Automatically starts a new run of the current Dungeon Floor",
    toggled = false
){
    private val sleepTime by SliderSetting(
        name = "Requeue Time",
        description = "How many seconds the run is requeued after it has ended",
        default = 5.0,
        min = 0.1,
        max = 10.0,
        increment = 0.1
    )

    private val showTitle by BooleanSetting(
        name = "Show downtime reminder",
        default = false
    )

    private val title by HudSetting(
        name = "Move HUD",
        defaultText = "Downtime requested"
    ).dependsOn { showTitle }

    private val titleTime by SliderSetting(
        name = "Reminder Time",
        description = "How long the downtime reminder will be visible",
        default = 1.5,
        min = 0.1,
        max = 3.0,
        increment = 0.1
    ).dependsOn { showTitle }

    var downtime = false

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent){
        if (Regex("""^Party.*!dt""", RegexOption.IGNORE_CASE) matches event.unformattedMessage) {
            sendCommand("pchat downtime request -> canceled auto-requeue for this run")
            downtime = true
            return
        }

        if (Regex("score") matches event.unformattedMessage){

            if (downtime) {
                sendCommand("pchat downtime request -> canceled auto-requeue for this run")
                title.visible = true
                runAfterServerTicks((titleTime * 20.0).toInt()) {
                    title.visible = false
                }
            }

            runAfterServerTicks((sleepTime * 20.0).toInt()) {
                if (PartyUtils.getPartySize() == 5 && !downtime) {
                    downtime = false
                    sendCommand("instancerequeue")
                }
            }
        }
    }
}