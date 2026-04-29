package xyz.vexo.utils


object DevMode {
    private val flags = mutableSetOf<String>()

    fun toggle(flag: String) {
        val active = if (flags.add(flag)) "ON" else { flags.remove(flag); "OFF" }
        modMessage("$flag: $active")
    }

    fun has(flag: String) = flags.contains(flag)

    inline fun ifFlag(flag: String, block: () -> Unit) {
        if (has(flag)) block()
    }
}
