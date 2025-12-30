package xyz.vexo.events

/**
 * Base class for cancellable events.
 * Cancellable events can be canceled by calling the cancel() method.
 */
abstract class CancellableEvent : Event() {
    var cancelled = false
        private set

    fun cancel() {
        cancelled = true
    }

    fun isCancelled(): Boolean = cancelled
}