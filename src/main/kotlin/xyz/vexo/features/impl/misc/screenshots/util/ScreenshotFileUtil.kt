package xyz.vexo.features.impl.misc.screenshots.util

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Utilities for handling screenshot file operations.
 */
object ScreenshotFileUtil {
    /**
     * Creates a new screenshot file with a unique name.
     *
     * @param gameDirectory The directory where the screenshot will be saved.
     * @return The created screenshot file.
     */
    fun createScreenshotFile(gameDirectory: File): File {
        val screenshotDir = File(gameDirectory, "screenshots").also { it.mkdirs() }

        val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss")
        val dateTimeString = LocalDateTime.now().format(dateTimeFormat)

        var outputFile = File(screenshotDir, "$dateTimeString.png")
        var duplicateCount = 1

        while (outputFile.exists()) {
            outputFile = File(screenshotDir, "${dateTimeString}_$duplicateCount.png")
            duplicateCount++
        }

        return outputFile
    }
}