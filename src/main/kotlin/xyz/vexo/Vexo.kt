package xyz.vexo

import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.metadata.ModMetadata
import net.fabricmc.loader.api.Version
import net.minecraft.client.Minecraft
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.vexo.commands.*
import xyz.vexo.features.ModuleManager
import xyz.vexo.events.EventDispatcher
import xyz.vexo.config.ConfigManager
import xyz.vexo.features.impl.ExampleModule



object Vexo : ClientModInitializer {
	const val MOD_ID = "vexo"

	private val metadata: ModMetadata by lazy {
		FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().metadata
	}
	val version: Version by lazy { metadata.version }

	@JvmStatic
	val mc: Minecraft = Minecraft.getInstance()

    val logger: Logger = LoggerFactory.getLogger("Vexo")

	override fun onInitializeClient() {

		EventDispatcher.init()

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			arrayOf(
				VexoCommand
			).forEach { commodore -> commodore.register(dispatcher) }
		}

		arrayOf(
			ExampleModule
		).forEach { ModuleManager.register(it) }

		ConfigManager.load()
	}
}