package xyz.vexo.features.impl.dungeons

import net.minecraft.sounds.SoundEvents
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.StringSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.modMessage
import xyz.vexo.Vexo.mc


object RagAxeNow : Module (
    name = "Rag Axe Alert",
    description = "Displays a title when to Rag Axe",
    toggled = false
) {
    private val ragAxeNowTextSetting = StringSetting(
        name = "HUD Text",
        default = "§cRag Axe Now!"
    )

    private val ragAxeNowText by ragAxeNowTextSetting

    private val ragAxeNowTitle by HudSetting(
        name = "Move HUD",
        defaultText = ragAxeNowText
    )

    init {
        ragAxeNowTextSetting.onChange = {
            ragAxeNowTitle.text = it
        }
    }

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        if (RagAxeTriggers.any { it.containsMatchIn(event.unformattedMessage) }) {
            modMessage("Rag Axe Now!")
            ragAxeNowTitle.showForXServerTicks(40)
            mc.player?.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0f, 2.0f)
        }
    }

    private val RagAxeTriggers = listOf(
        Regex("\\[BOSS] Wither King: I no longer wish to fight, but I know that will not stop you."),
        Regex("\\[BOSS] Livid: I can now turn those Spirits into shadows of myself, identical to their creator."),
        Regex("\\[BOSS] Sadan: I am the bridge between this realm and the world below! You shall not pass!")
    )
}