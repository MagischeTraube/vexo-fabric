package xyz.vexo.features.impl.kuudra

import java.util.concurrent.ConcurrentHashMap
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.core.component.DataComponents
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextColor
import xyz.vexo.Vexo
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ChatMessageEvent
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.TooltipEvent
import xyz.vexo.events.impl.WorldJoinEvent
import xyz.vexo.features.Module
import xyz.vexo.utils.KuudraPartyStats
import xyz.vexo.utils.modMessage
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.showKuudraStats

object KuudraPartyFinderInfo : Module(
        name = "Kuudra Party Finder Info",
        description = "Shows Kuudra stats behind each member in the party finder",
        toggled = false
){
    private val statsOnJoin by BooleanSetting(
        "Stats On Join",
        "Prints full Kuudra stats in chat when a player joins your party",
        default = true
    )

    private val selectedTier by SelectorSetting(
        "Kuudra Tier",
        "Which Kuudra tier should be displayed",
        default = "Infernal",
        options = listOf(
            "Basic",
            "Hot",
            "Burning",
            "Fiery",
            "Infernal"
        )
    )

    private val debugPrefetch by BooleanSetting(
        "Debug Prefetch",
        "Prints how many players were queued when the party finder opens (diagnostic)",
        default = false
    )

    private val partyFinderJoinRegex = Regex("^Party Finder > (\\w{1,16}) joined the group! \\(Combat Level \\d+\\)$")

    private const val MAX_CACHE_SIZE = 300

    private const val PREFETCH_INTERVAL_TICKS = 10

    private val originalLines = ConcurrentHashMap<String, Component>()

    private var tickCounter = 0

    private var lastScreen: Screen? = null

    @EventHandler
    fun onWorldJoin(event: WorldJoinEvent) {
        originalLines.clear()
        KuudraPartyStats.clear()
        lastScreen = null
    }


    @EventHandler
    fun onTick(event: ClientTickEvent) {
        val screen = Vexo.mc.screen
        if (screen !is AbstractContainerScreen<*> || screen.title.string.removeFormatting() != "Party Finder") {
            lastScreen = null
            return
        }

        val justOpened = screen !== lastScreen
        if (!justOpened && ++tickCounter % PREFETCH_INTERVAL_TICKS != 0) return
        lastScreen = screen

        val queued = HashSet<String>()
        for (slot in screen.menu.slots) {
            if (!slot.hasItem()) continue
            val lore = slot.item.get(DataComponents.LORE)?.styledLines()
                ?.map { it.string.removeFormatting().trim() }
                ?: continue
            for (name in kuudraPartyMembers(lore)) if (queued.add(name)) KuudraPartyStats.get(name)
        }

        if (debugPrefetch && justOpened) {
            modMessage("§7[Prefetch] party finder open — queued §f${queued.size}§7 players")
        }
    }


    private fun kuudraPartyMembers(lore: List<String>): List<String> {
        if (lore.none { it.startsWith("Tier:") }) return emptyList()
        val membersIndex = lore.indexOfFirst { it == "Members:" }
        if (membersIndex == -1) return emptyList()

        val names = ArrayList<String>()
        for (i in (membersIndex + 1) until lore.size) {
            val text = lore[i]
            if (text.isEmpty() || text.startsWith("Click to join") || text.startsWith("Empty")) break
            val name = text.substringBefore('(').substringBefore(':').trim()
            if (name.isNotEmpty() && name.all { it.isLetterOrDigit() || it == '_' }) names.add(name)
        }
        return names
    }

    @EventHandler
    fun triggerOnPartyFinderJoin(event: ChatMessageEvent) {
        if (!statsOnJoin) return
        val match = partyFinderJoinRegex.find(event.unformattedMessage.trim()) ?: return
        showKuudraStats(match.groupValues[1])
    }

    @EventHandler
    fun onTooltip(event: TooltipEvent) {
        val screen = event.screen
        if (screen !is AbstractContainerScreen<*>) return
        if (screen.title.string.removeFormatting() != "Party Finder") return

        val lines = event.lines
        if (lines.isEmpty()) return
        if (!lines[0].string.removeFormatting().endsWith("Party")) return

        if (lines.none { it.string.removeFormatting().trim().startsWith("Tier:") }) return

        val membersIndex = lines.indexOfFirst { it.string.removeFormatting().trim() == "Members:" }
        if (membersIndex == -1) return

        for (i in (membersIndex + 1) until lines.size) {
            val text = lines[i].string.removeFormatting().trim()
            if (text.isEmpty() || text.startsWith("Click to join") || text.startsWith("Empty")) break

            val name = text.substringBefore('(').substringBefore(':').trim()
            if (name.isEmpty() || !name.all { it.isLetterOrDigit() || it == '_' }) continue

            val original = originalLines.getOrPut(name) {
                if (originalLines.size >= MAX_CACHE_SIZE) originalLines.clear()
                lines[i]
            }
            lines[i] = buildLine(original, name)
        }
    }

    private fun buildLine(original: Component, name: String): Component {
        val suffix = when (val stats = KuudraPartyStats.get(name)) {
            null -> " §8[...]"

            else -> if (stats.isError) {
                " §c[?]"
            } else {
                val runs = stats.runs[selectedTier.lowercase()] ?: 0

                " §8| §dRuns §f${"%,d".format(runs)}" +
                        " §8· §aC: §f${stats.cataLevel}" +
                        " §8· §bMP §f${"%,d".format(stats.magicalPower)}" +
                        " §8· §7Rend ${if (stats.rendBone) "§a✔" else "§c✘"}"
            }
        }
        val color = firstColor(original) ?: TextColor.fromRgb(0x55FFFF)
        val nameComp = Component.literal(name).setStyle(Style.EMPTY.withColor(color))
        return Component.empty().append(nameComp).append(Component.literal(suffix))
    }

    /** The first explicit text color in the component tree (member lines carry it on name or root). */
    private fun firstColor(component: Component): TextColor? {
        component.style.color?.let { return it }
        for (sibling in component.siblings) firstColor(sibling)?.let { return it }
        return null
    }
}
