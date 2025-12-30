package xyz.vexo.features

/**
 * Annotation for modules that should always be active.
 *
 * These modules are automatically subscribed to the event bus and cannot be deactivated.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AlwaysActive