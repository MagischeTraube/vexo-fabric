package xyz.vexo.features.impl.misc.screenshots.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.minecraft.ChatFormatting
import net.minecraft.util.Util
import xyz.vexo.Vexo
import xyz.vexo.utils.ClipboardUtils
import xyz.vexo.utils.chatbuttons.CallbackButton
import xyz.vexo.utils.logError
import xyz.vexo.utils.modButtonsMessage
import xyz.vexo.utils.modMessage
import java.io.File
import javax.imageio.ImageIO

object ScreenshotChatNotifications {
    /**
     * Sends a chat message with interactive buttons (Folder, Open, Copy, Delete) for the saved screenshot.
     *
     * @param imageFile The file of the saved screenshot.
     */
    fun sendSavedMessage(imageFile: File) {
        Vexo.mc.execute {
            modButtonsMessage(
                message = "Screenshot saved!",
                buttons = listOf(
                    CallbackButton(
                        text = "Folder",
                        hoverText = "Open screenshots folder",
                        buttonColor = ChatFormatting.GREEN
                    ) {
                        Util.getPlatform().openUri(imageFile.parentFile.toURI())
                    },
                    CallbackButton(text = "Open", hoverText = "Open image", buttonColor = ChatFormatting.YELLOW) {
                        try {
                            Util.getPlatform().openUri(imageFile.toURI())
                        } catch (e: Exception) {
                            logError(e, this@ScreenshotChatNotifications)
                            modMessage("§cFailed to open image.")
                        }
                    },
                    CallbackButton(
                        text = "Copy",
                        hoverText = "Copy image to clipboard",
                        buttonColor = ChatFormatting.AQUA
                    ) {
                        Vexo.scope.launch(Dispatchers.IO) {
                            try {
                                val awtImage = ImageIO.read(imageFile)
                                ClipboardUtils.copyImageToClipboard(awtImage)

                                Vexo.mc.execute {
                                    modMessage("Copied Screenshot to clipboard!")
                                }
                            } catch (e: Exception) {
                                logError(e, this@ScreenshotChatNotifications)
                                Vexo.mc.execute {
                                    modMessage("§cFailed to copy screenshot to clipboard.")
                                }
                            }
                        }
                    },
                    CallbackButton(
                        text = "Delete",
                        hoverText = "Delete this screenshots",
                        buttonColor = ChatFormatting.RED
                    ) {
                        imageFile.delete()
                        modMessage("Deleted ${imageFile.name}")
                    }
                )
            )
        }
    }
}