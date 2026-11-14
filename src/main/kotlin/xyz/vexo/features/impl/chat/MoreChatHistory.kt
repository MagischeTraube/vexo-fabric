package xyz.vexo.features.impl.chat

import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.features.Module

object MoreChatHistory : Module(
    name = "More Chat History",
    description = "Raises the chat history limit from 100 up to 100000 messages",
    toggled = false
) {
    private const val VANILLA_LIMIT = 100

    private val maxMessages by SliderSetting(
        name = "Max Messages",
        description = "How many chat messages are kept",
        default = 10_000.0,
        min = 100.0,
        max = 100_000.0,
        increment = 100.0
    )

    @JvmStatic
    fun limit(): Int = if (enabled) maxMessages.toInt() else VANILLA_LIMIT
}
