package xyz.vexo.features.impl.kuudra

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessageEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.showKuudraStats

object KuudraPartyFinderInfo : Module(
        name = "Kuudra Party Finder Info",
        description = "Displays a countdown when you get eaten by Kuudra",
        toggled = false
){
    private val partyFinderJoinRegex = Regex("^Party Finder > (\\w{1,16}) joined the group! \\(Combat Level \\d+\\)$")

    @EventHandler
    fun triggerOnPartyFinderJoin(event: ChatMessageEvent) {
        val match = partyFinderJoinRegex.find(event.unformattedMessage.trim()) ?: return
        showKuudraStats(match.groupValues[1])
    }
}