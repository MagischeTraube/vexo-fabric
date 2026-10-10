package xyz.vexo.features.impl.dungeons

import net.minecraft.sounds.SoundEvents
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.StringSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.features.Module
import xyz.vexo.Vexo.mc

object CrystalPickupAlert : Module(
    name = "Crystal Pickup Alert",
    description = "Displays a HUD title when you pick up an Energy Crystal",
    toggled = false
) {
    private val crystalPickupTextSetting = StringSetting(
        name = "HUD Text",
        default = "§cCrystal Picked Up!"
    )

    private val crystalPickupText by crystalPickupTextSetting

    private val crystalPickupHud by HudSetting(
        name = "Move HUD",
        defaultText = crystalPickupText
    )

    init {
        crystalPickupTextSetting.onChange = {
            crystalPickupHud.text = it
        }
    }

    private val crystalPickupRegex = Regex("^(.+) picked up an Energy Crystal!$")

    @EventHandler
    fun onChat(event: ChatMessagePacketEvent) {
        val match = crystalPickupRegex.matchEntire(
            event.unformattedMessage
        ) ?: return

        val playerName = match.groupValues[1]
        val ownName = mc.player?.name?.string ?: return

        if (playerName != ownName) return

        crystalPickupHud.showForXServerTicks(20)
        mc.player?.playSound(
            SoundEvents.NOTE_BLOCK_PLING.value(),
            1.0f,
            2.0f
        )
    }
}