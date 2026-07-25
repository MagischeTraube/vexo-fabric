package xyz.vexo.clickgui

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi
import net.minecraft.client.gui.screens.Screen

/**
 * ModMenu integration that exposes the ClickGui as the mod configuration screen.
 */
class ModMenu : ModMenuApi {
    override fun getModConfigScreenFactory(): ConfigScreenFactory<Screen> = ConfigScreenFactory { _ ->
        ClickGui()
    }
}