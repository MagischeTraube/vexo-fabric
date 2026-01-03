package xyz.vexo

import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlin.coroutines.EmptyCoroutineContext
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
import xyz.vexo.features.impl.misc.*
import xyz.vexo.features.impl.misc.AutoRejoin
import xyz.vexo.features.impl.misc.ChatCleaner
import xyz.vexo.features.impl.misc.TyfrTrigger
import xyz.vexo.events.EventBus
import xyz.vexo.features.impl.dungeons.PadTimer
import xyz.vexo.utils.PartyUtils
import xyz.vexo.utils.PriceUtils


object Vexo : ClientModInitializer {
	const val MOD_ID = "vexo"

	private val metadata: ModMetadata by lazy {
		FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().metadata
	}
	val version: Version by lazy { metadata.version }

	val configDir: File by lazy {
		FabricLoader.getInstance().configDir.resolve(MOD_ID).toFile().apply {
			if (!exists()) mkdirs()
		}
	}

	@JvmStatic
	val mc: Minecraft = Minecraft.getInstance()

    val logger: Logger = LoggerFactory.getLogger("Vexo")

	val scope = CoroutineScope(SupervisorJob() + EmptyCoroutineContext)

	override fun onInitializeClient() {
		arrayOf(
			EventDispatcher,  PriceUtils
		).forEach { it.init() }

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			arrayOf(
				VexoCommand, TyfrCommand
			).forEach { commodore -> commodore.register(dispatcher) }
		}

		arrayOf(
			TyfrTrigger, PartyUtils,
		).forEach { EventBus.subscribe(it) }

		arrayOf(
			ChatCleaner, AutoRejoin, PadTimer
		).forEach { ModuleManager.register(it) }

		ConfigManager.load()
	}
}