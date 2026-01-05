package xyz.vexo.features.impl.dungeons

import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.StringSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.runAfterServerTicks


object RagAxeNow : Module (
    name = "RagAxe Alert",
    description = "Triggers when RagAxe is mentioned in chat.",
    toggled = false
) {
    private val ragAxeNowText by StringSetting(
        name = "HUD Text",
        default = "§cRag Axe Now!"
    )

    private val ragAxeNowTitle by HudSetting(
        name = "Move HUD",
        defaultText = ragAxeNowText
    )

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (RagAxeTriggers.any { it.containsMatchIn(event.message.removeFormatting()) }) {
            modMessage("RagAxe Now!")
            ragAxeNowTitle.visible = true
            runAfterServerTicks(40){ ragAxeNowTitle.visible =false }
        }
    }


    private val RagAxeTriggers = listOf(
        Regex("\\[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you."),
        Regex("\\[BOSS] Livid: I can now turn those Spirits into shadows of myself, identical to their creator."),
        Regex("\\[BOSS] Sadan: I am the bridge between this realm and the world below! You shall not pass!")
    )
}