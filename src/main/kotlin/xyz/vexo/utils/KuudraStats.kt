package xyz.vexo.utils

import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style
import xyz.vexo.Vexo

private const val KUUDRA_API = "https://api.infm7.xyz/kuudra"

internal fun showKuudraStats(name: String) {
    modMessage("§7Loading Kuudra stats for §f$name§7 ...")
    Vexo.scope.launch {
        val uuid = PlayerData.getUuidFromUsername(name) ?: run {
            modMessage("§cPlayer §f$name §cnot found.")
            return@launch
        }

        val json = try {
            ApiUtils.fetchJsonWithRetry("$KUUDRA_API?uuid=$uuid")
        } catch (e: Exception) {
            logError(e, ApiUtils)
            modMessage("§cFailed to fetch Kuudra stats.")
            return@launch
        }

        val data = json?.getAsJsonObject("data")
        if (json?.get("success")?.asBoolean != true || data == null) {
            modMessage("§cNo Kuudra data found for §f$name§c.")
            return@launch
        }

        renderKuudraStats(name, data)
    }
}

private val RELEVANT_TERMINATOR_ENCHANTMENTS = mapOf(
    "ultimate_rend" to "Rend",
    "ultimate_reiterate" to "Duplex",
)

private val TALISMAN_NAMES = mapOf(
    "KUUDRAS_KIDNEY" to "Kuudra's Kidney",
    "KUUDRAS_LUNG" to "Kuudra's Lung",
    "KUUDRAS_HEART" to "Kuudra's Heart",
)

private const val DIVIDER = "§6§m                                            §r"


private fun statLine(component: Component) {
    Vexo.mc.execute { Vexo.mc.gui.chat.addClientSystemMessage(component) }
}


private fun statLine(text: String) = statLine(Component.literal(text))

private fun renderKuudraStats(name: String, data: JsonObject) {
    fun int(key: String): Int = data.get(key)?.takeUnless { it.isJsonNull }?.asInt ?: 0
    fun flag(key: String): Boolean {
        val el = data.get(key) ?: return false
        return el.isJsonPrimitive && el.asJsonPrimitive.isBoolean && el.asBoolean
    }

    statLine(DIVIDER)
    statLine("  §6§lKUUDRA STATS §r§8» §f$name")
    statLine("")


    statLine("  §e§lProfile")
    statLine("  §7Magical Power: §b${grouped(int("magical_power"))}")
    statLine("  §7Catacombs: §a${int("cata_level")}")
    statLine("  §7Infernal Runs: §d${grouped(int("infernal_runs"))}")

    val talisman = data.getAsJsonObject("kuudra_talisman")
        ?.get("type")?.takeUnless { it.isJsonNull }?.asString ?: "NONE"
    statLine("  §7Kuudra Talisman: ${talismanDisplay(talisman)}")
    statLine("")


    statLine("  §e§lGear")
    statLine("  §7Wither Blade: ${check(flag("wither_blade"))}")


    val termEl = data.get("terminators")
    val terminators = if (termEl != null && termEl.isJsonArray) {
        termEl.asJsonArray.filterIsInstance<JsonObject>()
    } else emptyList()
    val relevant = terminators.filter {
        it.get("ultimate_name")?.takeUnless { e -> e.isJsonNull }?.asString in RELEVANT_TERMINATOR_ENCHANTMENTS
    }
    if (relevant.isEmpty()) {
        statLine("  §7Terminator: ${check(false)}")
    } else {
        statLine("  §7Terminator: ${check(true)} §8(${relevant.size}) §7— hover for enchants")
        for (t in relevant) statLine(terminatorComponent(t))
    }

    statLine("  §7SOS Flare: ${check(flag("sos_flare"))}")
    statLine("  §7Rend Bone: ${check(flag("rend_bone"))}")

    val axe = data.getAsJsonObject("ragnarock_axe")
    if (axe?.get("owned")?.asBoolean == true) {
        val gemstone = axe.get("gemstone")?.takeUnless { it.isJsonNull }?.asString
            ?.takeIf { it.isNotBlank() }?.let { titleCase(it) }
        val chimera = axe.get("chimera_level")?.takeUnless { it.isJsonNull }?.asInt ?: 0
        val extra = listOfNotNull(gemstone, "Chimera $chimera".takeIf { chimera > 0 }).joinToString(" §8· §7")
        statLine("  §7Ragnarock Axe: ${check(true)}${if (extra.isNotEmpty()) " §8(§7$extra§8)" else ""}")
    } else {
        statLine("  §7Ragnarock Axe: ${check(false)}")
    }


    val armorEl = data.get("armor")
    val armorSets = if (armorEl != null && armorEl.isJsonArray) {
        armorEl.asJsonArray.filterIsInstance<JsonObject>().filter { (it.getAsJsonArray("pieces")?.size() ?: 0) == 4 }
    } else emptyList()
    if (armorSets.isNotEmpty()) {
        statLine("")
        statLine("  §e§lArmor Sets §8» §7hover for enchants")
        // Worn set first, then wardrobe pages in their slot order.
        val sorted = armorSets.sortedBy { if (it.get("slot")?.asString == "armor") 0 else 1 }
        for (a in sorted) statLine(armorSetComponent(a))
    }

    statLine(DIVIDER)
}

private val ARMOR_SLOT_NOUNS = setOf("HELMET", "CHESTPLATE", "LEGGINGS", "BOOTS")

private fun armorBaseName(id: String): String {
    val parts = id.split('_')
    val kept = if (parts.size > 1 && parts.last().uppercase() in ARMOR_SLOT_NOUNS) parts.dropLast(1) else parts
    return titleCase(kept.joinToString("_"))
}


private fun armorSetComponent(entry: JsonObject): Component {
    val slot = entry.get("slot")?.takeUnless { it.isJsonNull }?.asString ?: ""
    val worn = slot == "armor"
    val pieces = entry.getAsJsonArray("pieces")?.filterIsInstance<JsonObject>() ?: emptyList()

    val names = pieces.mapNotNull { it.get("id")?.takeUnless { e -> e.isJsonNull }?.asString?.let(::armorBaseName) }
    val primary = names.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: "Unknown"
    val label = primary + if (names.distinct().size > 1) " §8(mixed)" else ""
    val tag = if (worn) " §a(worn)" else ""

    val hover = Component.literal(
        if (worn) "§aWorn Set" else "§7Wardrobe Slot ${slot.removePrefix("wardrobe_slot_")}"
    )
    for (p in pieces) {
        val id = p.get("id")?.takeUnless { it.isJsonNull }?.asString ?: continue
        hover.append(Component.literal("\n§f${armorBaseName(id)}"))
        for ((key, el) in p.entrySet()) {
            if (key == "id" || el.isJsonNull || !el.isJsonPrimitive || !el.asJsonPrimitive.isNumber) continue
            hover.append(Component.literal("  ${enchantName(key)} §f${el.asInt}"))
        }
    }

    val style = Style.EMPTY
        .withColor(ChatFormatting.AQUA)
        .withHoverEvent(HoverEvent.ShowText(hover))
    return Component.literal("  §7$label$tag").setStyle(style)
}

private fun enchantName(key: String): String {
    val ultimate = key.startsWith("ultimate_", ignoreCase = true)
    val name = titleCase(if (ultimate) key.substring("ultimate_".length) else key)
    return if (ultimate) "§d$name" else "§7$name"
}

private fun check(b: Boolean): String = if (b) "§a✔" else "§c✘"

private fun grouped(value: Int): String = "%,d".format(value)

private fun titleCase(raw: String): String = raw.split('_', ' ')
    .filter { it.isNotBlank() }
    .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

private fun talismanDisplay(type: String): String {
    if (type.equals("NONE", ignoreCase = true)) return "§8None"
    val name = TALISMAN_NAMES[type]
        ?: titleCase(type).replace(Regex("^Kuudras\\b"), "Kuudra's")
    return "§6$name"
}

private fun terminatorComponent(t: JsonObject): Component {
    val ultName = t.get("ultimate_name")?.takeUnless { it.isJsonNull }?.asString
    val display = RELEVANT_TERMINATOR_ENCHANTMENTS[ultName] ?: "Terminator"
    val ultLvl = t.get("ultimate_level")?.takeUnless { it.isJsonNull }?.asInt ?: 0

    val hover = Component.literal("§6$display §e$ultLvl")
    for ((key, el) in t.entrySet()) {
        if (key == "ultimate_name" || key == "ultimate_level") continue
        if (el.isJsonNull || !el.isJsonPrimitive || !el.asJsonPrimitive.isNumber) continue
        val label = key.removeSuffix("_level").split('_')
            .joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
        hover.append(Component.literal("\n§7$label: §f${el.asInt}"))
    }

    val style = Style.EMPTY
        .withColor(ChatFormatting.AQUA)
        .withHoverEvent(HoverEvent.ShowText(hover))
    return Component.literal("    §b→ $display $ultLvl").setStyle(style)
}
