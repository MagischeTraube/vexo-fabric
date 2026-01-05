package xyz.vexo.utils

import net.fabricmc.loader.api.FabricLoader
import xyz.vexo.Vexo
import java.io.File
import java.io.IOException

private const val MOD_ID = "vexo"

fun writeInFile(content: String, filename: String) {
    try {
        // Config-Ordner für die Mod
        val configDir: File = FabricLoader.getInstance().configDir.resolve(MOD_ID).toFile().apply {
            if (!exists()) mkdirs()
        }

        // Datei im Ordner
        val file = File(configDir, filename)

        // Datei erstellen, falls sie nicht existiert
        if (!file.exists()) {
            file.createNewFile()
        }

        // Inhalt anhängen, jede Nachricht in eine neue Zeile
        file.appendText(content + System.lineSeparator())

    } catch (e: IOException) {
        Vexo.logger.error("Failed to write to file $filename", e)
    }
}
