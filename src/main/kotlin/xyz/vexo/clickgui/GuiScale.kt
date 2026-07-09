package xyz.vexo.clickgui

import gg.essential.elementa.constraints.PixelConstraint
import gg.essential.elementa.constraints.SiblingConstraint
import gg.essential.elementa.dsl.pixels

/**
 * Content-scale helpers for the ClickGui. Everything fixed-size in the GUI (text, icons, padding,
 * card heights …) is expressed through [gpx] / [gsib] so it all resizes proportionally with the
 * UI-scale slider in [GuiPrefs], instead of only the outer window scaling while the contents stay a
 * fixed pixel size.
 */

/** Current ClickGui content-scale factor, driven by the UI-scale slider in [GuiPrefs]. */
val guiScale: Float get() = GuiPrefs.sizeMultiplier()

/**
 * A pixel constraint scaled by the current UI-scale slider, mirroring Elementa's [pixels]. Use this
 * for every fixed design dimension (sizes, offsets, text scales). Do NOT wrap runtime values coming
 * from `getLeft()` / `getWidth()` / `mouseX` — those are already in real (scaled) pixels.
 */
fun Number.gpx(alignOpposite: Boolean = false, alignOutside: Boolean = false): PixelConstraint =
    (toFloat() * guiScale).pixels(alignOpposite, alignOutside)

/** A sibling-spacing constraint whose padding scales with the UI-scale slider. */
fun gsib(padding: Float = 0f, alignOpposite: Boolean = false): SiblingConstraint =
    SiblingConstraint(padding * guiScale, alignOpposite)
