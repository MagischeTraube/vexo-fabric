package xyz.vexo.features.impl.chat

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.network.chat.Component
import xyz.vexo.Vexo
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.features.Module
import xyz.vexo.mixin.ChatComponentAccessor
import xyz.vexo.utils.logError
import xyz.vexo.utils.removeFormatting
import java.io.File
import java.util.function.Consumer

/** Stacks repeated chat messages into one line with a counter. */
object ChatStacking : Module(
    name = "Chat Stacking",
    description = "Stacks repeated chat messages into a single line with a counter",
    toggled = false
) {
    private const val MODE_INDEFINITELY = "Indefinitely"
    private const val MODE_SESSION = "This Session"
    private const val MODE_TIME = "Based on Time"
    private const val MODE_MESSAGES = "Based on Messages"

    private val mode by SelectorSetting(
        name = "Stack Mode",
        description = "When a repeated message should still be stacked onto the previous one",
        default = MODE_SESSION,
        options = listOf(MODE_INDEFINITELY, MODE_SESSION, MODE_TIME, MODE_MESSAGES)
    )

    private val timeWindow by SliderSetting(
        name = "Time Window (min)",
        description = "Only stack if the same message came within this many minutes",
        default = 5.0,
        min = 1.0,
        max = 60.0,
        increment = 1.0
    ).apply { dependsOn { mode == MODE_TIME } }

    private val messageWindow by SliderSetting(
        name = "Message Window",
        description = "Only stack if the same message came within this many messages",
        default = 100.0,
        min = 10.0,
        max = 1000.0,
        increment = 10.0
    ).apply { dependsOn { mode == MODE_MESSAGES } }

    private class Stack(val count: Int, val time: Long, val index: Long, val message: GuiMessage?)

    private const val MAX_TRACKED = 2000

    private val stacks = object : LinkedHashMap<String, Stack>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Stack>) = size > MAX_TRACKED
    }

    private var messageCounter = 0L

    private val file = File(Vexo.configDir, "chatstacks.json")
    private val separatorRegex = Regex("""^[-=_▬━─▃▀▄\s]+$""")
    private val stackSuffixRegex = Regex(""" \(x\d+\)$""")

    init {
        ClientLifecycleEvents.CLIENT_STARTED.register { load() }
        ClientLifecycleEvents.CLIENT_STOPPING.register { save() }
    }

    @JvmStatic
    fun handle(chat: ChatComponent, message: Component, add: Consumer<Component>): Boolean {
        if (!enabled) return false
        messageCounter++

        val key = message.string.removeFormatting().trim()
        if (key.isEmpty() || separatorRegex.matches(key)) return false

        val accessor = chat as ChatComponentAccessor
        val now = System.currentTimeMillis()
        val previous = stacks[key]

        val count = if (previous != null && isWithinWindow(previous, now)) {
            removePrevious(accessor, previous, key)
            previous.count + 1
        } else 1

        val display = if (count > 1) {
            Component.empty().append(message).append(Component.literal(" (x$count)").withStyle(ChatFormatting.GRAY))
        } else message

        add.accept(display)

        val added = accessor.vexoAllMessages().firstOrNull()?.takeIf { it.content() === display }
        stacks[key] = Stack(count, now, messageCounter, added)
        return true
    }

    private fun isWithinWindow(stack: Stack, now: Long): Boolean = when (mode) {
        MODE_TIME -> now - stack.time <= timeWindow.toLong() * 60_000L
        MODE_MESSAGES -> messageCounter - stack.index <= messageWindow.toLong()
        else -> true
    }

    private fun removePrevious(accessor: ChatComponentAccessor, stack: Stack, key: String) {
        val all = accessor.vexoAllMessages()
        val index = stack.message?.let { ref -> all.indexOfFirst { it === ref } }?.takeIf { it >= 0 }
            ?: all.indexOfFirst { it.content().string.removeFormatting().trim().replace(stackSuffixRegex, "") == key }
        if (index < 0) return

        all.removeAt(index)
        accessor.vexoRefreshTrimmedMessages()
    }

    @JvmStatic
    fun onDisconnect() {
        if (mode == MODE_SESSION) stacks.clear()
        save()
    }

    private fun save() {
        try {
            val array = JsonArray()
            stacks.forEach { (key, stack) ->
                array.add(JsonObject().apply {
                    addProperty("key", key)
                    addProperty("count", stack.count)
                    addProperty("time", stack.time)
                    addProperty("index", stack.index)
                })
            }
            val root = JsonObject().apply {
                addProperty("counter", messageCounter)
                add("stacks", array)
            }
            file.writeText(Vexo.gson.toJson(root))
        } catch (e: Exception) {
            logError(e, this)
        }
    }

    private fun load() {
        try {
            if (!file.exists()) return
            val root = JsonParser.parseString(file.readText()).asJsonObject
            messageCounter = root.get("counter")?.asLong ?: 0L
            if (mode == MODE_SESSION) return

            root.getAsJsonArray("stacks")?.forEach { element ->
                val obj = element.asJsonObject
                stacks[obj.get("key").asString] = Stack(
                    count = obj.get("count").asInt,
                    time = obj.get("time").asLong,
                    index = obj.get("index").asLong,
                    message = null
                )
            }
        } catch (e: Exception) {
            logError(e, this)
        }
    }
}
