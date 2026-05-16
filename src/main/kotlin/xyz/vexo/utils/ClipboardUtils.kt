package xyz.vexo.utils

import xyz.vexo.Vexo.mc
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

/**
 * Utility class for clipboard operations.
 */
object ClipboardUtils {
    /**
     * A transferable object for images.
     */
    class ImageTransferable(private val image: Image) : Transferable {

        override fun getTransferDataFlavors(): Array<DataFlavor> {
            return arrayOf(DataFlavor.imageFlavor)
        }

        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
            return DataFlavor.imageFlavor.equals(flavor)
        }

        override fun getTransferData(flavor: DataFlavor): Any {
            if (!isDataFlavorSupported(flavor)) {
                throw UnsupportedFlavorException(flavor)
            }
            return image
        }
    }

    /**
     * Copies the given image to the system clipboard.
     *
     * @param image The image to copy.
     */
    fun copyImageToClipboard(image: Image) {
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        val selection = ImageTransferable(image)
        clipboard.setContents(selection, null)
    }

    /**
     * Copies the given text to the system clipboard.
     *
     * @param text The text to copy.
     */
    fun copyTextToClipboard(text: String) {
        mc.keyboardHandler.clipboard = text
    }
}
