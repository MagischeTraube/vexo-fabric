package xyz.vexo

import com.google.gson.Gson
import com.google.gson.GsonBuilder
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
import xyz.vexo.events.EventDispatcher
import xyz.vexo.events.EventBus
import xyz.vexo.config.ConfigManager
import xyz.vexo.features.ModuleManager
import xyz.vexo.features.impl.misc.*
import xyz.vexo.features.impl.kuudra.*
import xyz.vexo.features.impl.dungeons.*
import xyz.vexo.utils.*


object Vexo : ClientModInitializer {
	const val MOD_ID = "vexo"

	private val metadata: ModMetadata by lazy {
		FabricLoader.getInstance().getModContainer(MOD_ID).orElseThrow().metadata
	}
	val version: Version by lazy { metadata.version }

	val configDir =
		FabricLoader.getInstance().configDir.resolve(MOD_ID).toFile().apply {
			if (!exists()) mkdirs()
		}

	val gson: Gson = GsonBuilder()
		.setPrettyPrinting()
		.create()

	@JvmStatic
	val mc: Minecraft = Minecraft.getInstance()

    val logger: Logger = LoggerFactory.getLogger("Vexo")

	val scope = CoroutineScope(SupervisorJob() + EmptyCoroutineContext)

	override fun onInitializeClient() {
		arrayOf(
			EventDispatcher, PriceUtils
		).forEach { it.init() }

		ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
			arrayOf(
				VexoCommand, TyfrCommand,

				entranceCommand, f1Command, f2Command, f3Command, f4Command, f5Command, f6Command, f7Command, m1Command, m2Command,
				m3Command, m4Command, m5Command, m6Command, m7Command, t1Command, t2Command, t3Command, t4Command, t5Command
			).forEach { commodore -> commodore.register(dispatcher) }
		}

		arrayOf(
			DungeonUtils, PartyUtils, TyfrTrigger
		).forEach { EventBus.subscribe(it) }

		arrayOf(
			// dungeons
			HealerP5LeapAlert, PadTimer, PartyFinder, PositionalMessages, RagAxeNow, ParticleHider,

			// kuudra
			AutoKuudraRequeue,

			// misc
			AutoRejoin, ChatCleaner, SlayerHelper, ParticleHiderDev
		).forEach { ModuleManager.register(it) }

		ConfigManager.load()
	}
}