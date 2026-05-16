package xyz.vexo.utils.chatbuttons

import java.util.UUID

/**
 * Registry for chat button actions.
 *
 * This registry allows chat buttons to execute callbacks when clicked.
 * Actions are grouped by groupId to prevent memory leaks from old actions.
 */
object ActionRegistry {

    private data class Entry(val action: () -> Unit, val groupId: String)

    private val entries = LinkedHashMap<String, Entry>()
    private val groupOrder = ArrayDeque<String>()

    private const val MAX_GROUPS = 10

    /**
     * Registers an action with a group ID.
     *
     * @param action The action to register.
     * @param groupId The group ID to associate with the action.
     * @return The ID of the registered action.
     */
    @Synchronized
    fun register(action: () -> Unit, groupId: String): String {
        if (groupId !in groupOrder) {
            groupOrder.addLast(groupId)

            while (groupOrder.size > MAX_GROUPS) {
                val oldestGroup = groupOrder.removeFirst()
                entries.entries.removeIf { it.value.groupId == oldestGroup }
            }
        }

        val id = UUID.randomUUID().toString()
        entries[id] = Entry(action, groupId)
        return id
    }

    /**
     * Runs an action by its ID.
     *
     * @param id The ID of the action to run.
     * @return True if the action was found and run, false otherwise.
     */
    @JvmStatic
    @Synchronized
    fun runAction(id: String): Boolean {
        val entry = entries.get(id) ?: return false
        entry.action()
        return true
    }

    /**
     * Clears all actions from the registry.
     */
    @Synchronized
    fun clear() {
        entries.clear()
        groupOrder.clear()
    }
}
