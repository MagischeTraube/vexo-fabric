package xyz.vexo.utils

/**
 * A utility object for managing development mode flags.
 */
object DevMode {
    private val flags = mutableSetOf<String>()

    /**
     * Toggles a development mode flag on or off.
     *
     * @param flag The flag to toggle.
     */
    fun toggle(flag: String) {
        val active = if (flags.add(flag)) "ON" else { flags.remove(flag); "OFF" }
        modMessage("$flag: $active")
    }

    /**
     * Checks if a development mode flag is active.
     *
     * @param flag The flag to check.
     * @return `true` if the flag is active, `false` otherwise.
     */
    fun has(flag: String) = flags.contains(flag)

    /**
     * Executes a block of code if a development mode flag is active.
     *
     * @param flag The flag to check.
     * @param block The block of code to execute.
     */
    inline fun ifFlag(flag: String, block: () -> Unit) {
        if (has(flag)) block()
    }
}
