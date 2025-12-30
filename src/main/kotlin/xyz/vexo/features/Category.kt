package xyz.vexo.features

/**
 * Enum representing different categories for modules.
 */
enum class Category(val displayName: String) {
    DUNGEONS("Dungeons"),
    RENDER("Render"),
    MISC("Misc");

    companion object {
        /**
         * Determines the category of a module based on its package name.
         * @param clazz The module class to analyze
         * @return The corresponding Category, or null if not found
         */
        fun fromPackage(clazz: Class<out Module>): Category? =
            entries.find { clazz.`package`.name.contains(it.name, true) }
    }
}
