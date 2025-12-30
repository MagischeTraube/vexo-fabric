package xyz.vexo.events

import xyz.vexo.utils.logError

/**
 * Base class for events.
 * Events are posted to the event bus and can be handled by event handlers.
 */
abstract class Event {
    fun postAndCatch(): Boolean {
        return runCatching {
            EventBus.post(this)
        }.onFailure {
            logError(it, this)
        }.isSuccess
    }
}