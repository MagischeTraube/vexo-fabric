package xyz.vexo.events

import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

object EventBus {

    private val subscribersByEvent =
        ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<EventSubscriber>>()

    private val instanceIndex =
        ConcurrentHashMap<Any, MutableList<Class<*>>>()

    /**
     * Subscribe an object to receive events.
     *
     * @param obj The object to subscribe
     */
    fun subscribe(obj: Any) {
        if (instanceIndex.containsKey(obj)) return

        val eventTypes = mutableListOf<Class<*>>()
        val lookup = MethodHandles.lookup()

        for (method in obj.javaClass.declaredMethods) {
            if (!method.isAnnotationPresent(EventHandler::class.java)) continue
            if (method.parameterCount != 1) continue

            val eventType = method.parameterTypes[0]
            if (!Event::class.java.isAssignableFrom(eventType)) continue

            method.isAccessible = true

            val handle = lookup
                .unreflect(method)
                .bindTo(obj)

            val subscriber = EventSubscriber(obj, handle)

            subscribersByEvent
                .computeIfAbsent(eventType) { CopyOnWriteArrayList() }
                .add(subscriber)

            eventTypes.add(eventType)
        }

        if (eventTypes.isNotEmpty()) {
            instanceIndex[obj] = eventTypes
        }
    }

    /**
     * Unsubscribe an object from receiving events.
     */
    fun unsubscribe(obj: Any) {
        val eventTypes = instanceIndex.remove(obj) ?: return

        for (eventType in eventTypes) {
            subscribersByEvent[eventType]?.removeIf {
                it.instance === obj
            }
        }
    }

    /**
     * Post an event to all subscribers.
     * @return true if no subscriber threw an exception
     */
    fun post(event: Event) {
        subscribersByEvent[event.javaClass]?.forEach { subscriber ->
            subscriber.handle.invoke(event)
        }
    }

    private data class EventSubscriber(
        val instance: Any,
        val handle: MethodHandle
    )
}
