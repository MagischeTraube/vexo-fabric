package xyz.vexo.features.impl

import xyz.vexo.features.Module
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.HudSetting


object ExampleModule : Module(
    name = "Example",
    description = "Demonstrates module functionality",
    toggled = false
) {
    private val enableFeatureA by BooleanSetting(
        name = "Enable Feature A",
        description = "Enables feature A",
        default = false
    )

    private val featureBColor by HudSetting(
        name = "Hud A",
        description = "A customizable HUD element",
        defaultText = "test",
        defaultVisibility = true
    ).dependsOn { enableFeatureA }
}