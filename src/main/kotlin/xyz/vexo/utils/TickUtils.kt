package xyz.vexo.utils

import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.ServerTickEvent
import xyz.vexo.events.EventBus
import xyz.vexo.events.EventHandler

/**
 * This class is used to schedule actions to be executed after a specified number of game ticks.
 * It uses the Event Bus to listen for ClientTickEvents and execute the scheduled actions when the time is right.
 */
object TickScheduler {

    private val clientTasks = mutableListOf<ScheduledTask>()
    private val serverTasks = mutableListOf<ScheduledTask>()

    init {
        EventBus.subscribe(this)
    }

    @EventHandler
    fun onClientTick(event: ClientTickEvent) {
        processTasks(clientTasks)
    }

    @EventHandler
    fun onServerTick(event: ServerTickEvent) {
        processTasks(serverTasks)
    }

    private fun processTasks(tasks: MutableList<ScheduledTask>) {
        val iterator = tasks.iterator()

        while (iterator.hasNext()) {
            val task = iterator.next()
            task.remainingTicks--

            if (task.remainingTicks <= 0) {
                try {
                    task.action()
                } catch (e: Exception) {
                    logError(e, this)
                } finally {
                    iterator.remove()
                }
            }
        }
    }

    fun scheduleClient(ticks: Int, action: () -> Unit) {
        val safeTicks = ticks.coerceAtLeast(1)
        clientTasks += ScheduledTask(safeTicks, action)
    }

    fun scheduleServer(ticks: Int, action: () -> Unit) {
        val safeTicks = ticks.coerceAtLeast(1)
        serverTasks += ScheduledTask(safeTicks, action)
    }

    private data class ScheduledTask(
        var remainingTicks: Int,
        val action: () -> Unit
    )
}

/**
 * Schedules an action to be executed after a specified number of client ticks.
 *
 * @param ticks The number of ticks to wait before executing the action. Values <= 0 will be treated as 1.
 * @param action The action to be executed.
 */
fun runAfterClientTicks(ticks: Int, action: () -> Unit) {
    TickScheduler.scheduleClient(ticks, action)
}

/**
 * Schedules an action to be executed after a specified number of server ticks.
 *
 * @param ticks The number of ticks to wait before executing the action. Values <= 0 will be treated as 1.
 * @param action The action to be executed.
 */
fun runAfterServerTicks(ticks: Int, action: () -> Unit) {
    TickScheduler.scheduleServer(ticks, action)
}
