package xyz.vexo.utils

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessagePacketEvent
import xyz.vexo.events.impl.ServerConnectEvent

object PartyUtils {

    /**
     * Returns a boolean value whether you are in a party or not.
     */
    var inParty = false
        private set

    /**
     * Returns a list of strings containing all party Members
     */
    val partyMembers: MutableList<String> = mutableListOf()


    @EventHandler
    fun onChat(event: ChatMessagePacketEvent){
        val rawMessage = event.message.removeFormatting()
        val cleanedMessage = rawMessage.replace(Regex("\\[[^]]*]"), "").replace("●", "")
        val nameRegex = Regex("\\b[A-Za-z0-9_]{1,16}\\b")
        val relevant = prefixes.any { cleanedMessage.startsWith(it) } || cleanedMessage.endsWith("joined the party.")
        val names = nameRegex.findAll(cleanedMessage).map { it.value }

        if (leftParty.any { it.containsMatchIn(rawMessage) }) {
            inParty = false
            partyMembers.clear()
        }
        if (createdParty.any { it.containsMatchIn(rawMessage) }) {
            inParty = true
        }

        if (cleanedMessage.endsWith("has left the party.")) {
            nameRegex.find(cleanedMessage)?.value?.let { partyMembers.remove(it) }
        }

        if (relevant) {

            names.forEach { if (!partyMembers.contains(it)) partyMembers.add(it) }
        }
    }

    /**
     *Returns an integer of the current party size
     */
    fun getPartySize():Int = partyMembers.size


    val leftParty = listOf(
        Regex("""You left the party\."""),
        Regex("""The party was disbanded .*"""),
        Regex("""You have been kicked from the party by .*"""),
        Regex("""You are not currently in a party\.""")
    )

    val createdParty = listOf(
        Regex("""You have joined .* party!"""),
        Regex(""".* joined the party\.""")
    )

    val prefixes = listOf(
        "Party Members:",
        "Party Moderators:",
        "Party Leader:",
        "You have joined ",
        "You'll be partying with:"
    )



    @EventHandler
    fun onJoin(event: ServerConnectEvent){
        runAfterServerTicks(5){
            sendCommand("p list")
        }
    }
}
