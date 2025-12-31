package xyz.vexo.events

import xyz.vexo.utils.logError
import java.lang.reflect.Method
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaMethod

object EventBus {
    private val subscribers = mutableMapOf<Any, MutableList<EventSubscriber>>()

    /**
     * Subscribes an object to receive events.
     * @param obj The object to subscribe
     */
    fun subscribe(obj: Any) {
        if (subscribers.containsKey(obj)) return

        val eventSubscribers = mutableListOf<EventSubscriber>()

        obj::class.declaredMemberFunctions.forEach { function ->
            if (function.hasAnnotation<EventHandler>()) {
                val method = function.javaMethod ?: return@forEach

                if (method.parameterCount == 1) {
                    val eventType = method.parameterTypes[0]

                    if (Event::class.java.isAssignableFrom(eventType)) {
                        method.isAccessible = true
                        eventSubscribers.add(EventSubscriber(obj, method, eventType))
                    }
                }
            }
        }

        subscribers[obj] = eventSubscribers
    }

    /**
     * Unsubscribes an object from receiving events.
     * @param obj The object to unsubscribe
     */
    fun unsubscribe(obj: Any) {
        subscribers.remove(obj)
    }

    /**
     * Posts an event to all registered subscribers.
     * @param event The event to post
     * @return true if all subscribers handled the event successfully, false otherwise
     */
    fun post(event: Event): Boolean {
        var success = true
        val eventClass = event::class.java

        subscribers.values.flatten()
            .filter { it.eventType.isAssignableFrom(eventClass) }
            .forEach { subscriber ->
                try {
                    subscriber.method.invoke(subscriber.instance, event)
                } catch (e: Exception) {
                    logError(e, this)
                    success = false
                }
            }

        return success
    }

    private data class EventSubscriber(
        val instance: Any,
        val method: Method,
        val eventType: Class<*>
    )
}