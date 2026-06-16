package xyz.vexo.utils

import xyz.vexo.Vexo

/**
 * Logs an info message with the given message.
 *
 * @param message The message to log.
 */
fun logInfo(message: String) {
    Vexo.logger.info(message)
}

/**
 * Logs a warning message with the given message.
 *
 * @param message The message to log.
 */
fun logWarn(message: String) {
    Vexo.logger.warn(message)
}

/**
 * Logs an error with the given throwable and context.
 *
 * @param throwable The throwable to log.
 * @param context The context in which the error occurred.
 */
fun logError(throwable: Throwable, context: Any) {
    val message =
        "[Vexo] ${throwable::class.simpleName ?: "error"} at ${context::class.simpleName}"
    Vexo.logger.error(message, throwable)
}

/**
 * Logs a debug message with the given message.
 *
 * @param message The message to log.
 */
fun logDebug(message: String) {
    Vexo.logger.debug(message)
}

/**
 * Precompiled pattern for [removeFormatting]. Compiling once avoids rebuilding the regex on
 * every call — this runs on every chat packet, tablist entry and scoreboard entry.
 */
private val FORMATTING_REGEX = Regex("§x(§[0-9a-fA-F]){6}|§[0-9a-fk-orA-FK-OR]")

/**
 * Removes Minecraft color and formatting codes from a string.
 *
 * @return The string with all formatting codes removed.
 */
fun String.removeFormatting(): String = this.replace(FORMATTING_REGEX, "")