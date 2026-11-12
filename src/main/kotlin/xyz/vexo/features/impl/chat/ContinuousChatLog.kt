package xyz.vexo.features.impl.chat

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.mojang.serialization.JsonOps
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents
import net.minecraft.client.multiplayer.chat.GuiMessage
import net.minecraft.client.multiplayer.chat.GuiMessageSource
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import xyz.vexo.Vexo
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.features.Module
import xyz.vexo.mixin.ChatComponentAccessor
import xyz.vexo.utils.logError
import xyz.vexo.utils.logInfo
import java.io.File

/** Keeps the chat history across restarts. */
object ContinuousChatLog : Module(
    name = "Continuous Chat Log",
    description = "Keeps your chat history across restarts instead of starting with an empty chat",
    toggled = false
) {
    private val keepOnDisconnect by BooleanSetting(
        name = "Keep On Disconnect",
        description = "Don't clear the chat when leaving a server or world",
        default = true
    )

    private val showSeparator by BooleanSetting(
        name = "Show Separator",
        description = "Adds a line marking where the restored chat log ends",
        default = true
    )

    private val file = File(Vexo.configDir, "chatlog.json")

    private const val RESTORED_ADDED_TIME = -100_000

    init {
        ClientLifecycleEvents.CLIENT_STARTED.register { if (enabled) restore() }
        ClientLifecycleEvents.CLIENT_STOPPING.register { if (enabled) save(skipIfEmpty = true) }
    }

    @JvmStatic
    fun shouldKeepOnDisconnect(): Boolean = enabled && keepOnDisconnect

    @JvmStatic
    fun onDisconnect() {
        if (enabled) save(skipIfEmpty = true)
    }

    private val chat get() = mc.gui.hud.chat as ChatComponentAccessor

    private fun save(skipIfEmpty: Boolean) {
        try {
            val messages = chat.vexoAllMessages().toList()
            if (skipIfEmpty && messages.isEmpty()) return

            val ops = mc.level?.registryAccess()?.createSerializationContext(JsonOps.INSTANCE) ?: JsonOps.INSTANCE
            val array = JsonArray()
            for (message in messages.asReversed()) {
                val entry = JsonObject()
                ComponentSerialization.CODEC.encodeStart(ops, message.content()).result().ifPresent {
                    entry.add("component", it)
                }
                entry.addProperty("text", message.content().string)
                entry.addProperty("source", message.source().name)
                array.add(entry)
            }

            file.writeText(Vexo.gson.toJson(JsonObject().apply { add("messages", array) }))
        } catch (e: Exception) {
            logError(e, this)
        }
    }

    private fun restore() {
        try {
            if (!file.exists()) return
            val root = JsonParser.parseString(file.readText()).asJsonObject
            val entries = root.getAsJsonArray("messages") ?: return

            val accessor = chat
            val all = accessor.vexoAllMessages()
            val restored = entries.mapNotNull(::decode)
            val combined = buildList {
                addAll(all)
                if (showSeparator && restored.isNotEmpty()) {
                    add(message(Component.literal("§8§m          §r §7Restored chat log §8§m          "), GuiMessageSource.SYSTEM_CLIENT))
                }
                addAll(restored.asReversed())
            }.take(100)

            all.clear()
            all.addAll(combined)
            accessor.vexoRefreshTrimmedMessages()
            logInfo("Restored ${restored.size} chat messages")
        } catch (e: Exception) {
            logError(e, this)
        }
    }

    private fun decode(element: JsonElement): GuiMessage? {
        val entry = element.takeIf { it.isJsonObject }?.asJsonObject ?: return null
        val component = entry.get("component")
            ?.let { ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, it).result().orElse(null) }
            ?: entry.get("text")?.asString?.let { Component.literal(it) }
            ?: return null
        val source = runCatching { GuiMessageSource.valueOf(entry.get("source").asString) }
            .getOrDefault(GuiMessageSource.SYSTEM_SERVER)
        return message(component, source)
    }

    private fun message(component: Component, source: GuiMessageSource) =
        GuiMessage(RESTORED_ADDED_TIME, component, null, source, null)
}
