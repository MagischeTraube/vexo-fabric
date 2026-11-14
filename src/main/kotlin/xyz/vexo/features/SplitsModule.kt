package xyz.vexo.features

import kotlin.math.abs
import xyz.vexo.Vexo.mc
import java.awt.Color
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.ColorSetting
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.hud.MoveActiveHudsGui
import xyz.vexo.hud.PreviewSegment
import xyz.vexo.utils.PersonalBest
import xyz.vexo.utils.SplitArea
import xyz.vexo.utils.SplitRow
import xyz.vexo.utils.SplitsManager
import xyz.vexo.utils.formatTime
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.renderString

/**
 * Shared HUD of the split timers with configurable columns and color schemes.
 * Subclasses must forward their [HudRenderEvent] handler to [render], since the event bus only sees declared methods.
 *
 * @param area Which runs this module shows.
 * @param hasTargets Whether the "Dynamic (Target)" color scheme is offered.
 */
abstract class SplitsModule(
    name: String,
    description: String,
    private val area: SplitArea,
    hasTargets: Boolean,
) : Module(name, description, toggled = false, legacyNames = listOf("Splits")) {

    /** What a HUD column shows. [sample] is the widest typical text, used as minimum width so columns don't jitter. */
    protected enum class Column(val label: String, val sample: String) {
        NONE("None", ""),
        TIME("Time", "59m 59.99s"),
        TICK_TIME("Tick Time", "99m 59.99s"),
        LAG_LOST("Lag Lost", "+99.9s"),
        PB_DIFF("PB Diff", "+9.99s"),
        PB("PB", "59m 59.99s");

        companion object {
            fun of(label: String) = entries.firstOrNull { it.label == label } ?: NONE
        }
    }

    private enum class ColorScheme(val label: String) {
        ODIN("Odin"),
        DYNAMIC_PB("Dynamic (PB)"),
        DYNAMIC_TARGET("Dynamic (Target)"),
        CUSTOM("Custom"),
        VEXO("Vexo");

        companion object {
            fun of(label: String) = entries.firstOrNull { it.label == label } ?: VEXO
        }
    }

    /**
     * Colors as 0xRRGGBB. [label] / [summary] null keeps the built-in § colors of the names.
     * [muted] is used for empty cells ("-") and 0s lag.
     */
    protected data class Palette(
        val label: Int?,
        val time: Int,
        val current: Int,
        val tick: Int,
        val ahead: Int,
        val behind: Int,
        val pb: Int,
        val summary: Int?,
        val muted: Int,
    )

    /** One piece of text drawn in a single color (0xRRGGBB); § codes inside [text] still apply. */
    protected class Cell(val text: String, val color: Int)

    /** A rendered HUD line: label on the left, one cell per active column. */
    protected class Line(val label: Cell, val cells: List<Cell>, val separatorBefore: Boolean = false)

    /** A split segment with its PB and target time in ms (null if not set). */
    protected data class Segment(val row: SplitRow, val pb: Long?, val target: Long?)

    /** A segment as shown on the HUD; a separator is drawn wherever [phase] changes between visible rows. */
    protected class DisplayRow(val segment: Segment, val phase: Int = 0)

    private companion object {
        const val LINE_HEIGHT = 9
        /** Extra space for a separator line; the line sits in the middle of the gap. */
        const val SEPARATOR_GAP = 6
        const val COLUMN_GAP = 6
        const val SEPARATOR_COLOR = 0x40FFFFFF

        val ODIN = Palette(
            label = null, time = 0xFFFFFF, current = 0xFFFF55, tick = 0xAAAAAA,
            ahead = 0x55FF55, behind = 0xFF5555, pb = 0xFFAA00, summary = null, muted = 0x555555
        )

        /** Built around the Vexo indigo accent with soft pastel status colors. */
        val VEXO = Palette(
            label = 0xC4B5FD, time = 0xF1F5F9, current = 0x7DD3FC, tick = 0x94A3B8,
            ahead = 0x6EE7B7, behind = 0xFDA4AF, pb = 0xFCD34D, summary = 0x818CF8, muted = 0x64748B
        )

        // Vanilla chat colors (§a, §e, §c, §6) for the dynamic schemes.
        const val DYN_GREEN = 0x55FF55
        const val DYN_YELLOW = 0xFFFF55
        const val DYN_RED = 0xFF5555
        const val DYN_GOLD = 0xFFAA00
        /** Share of the reference time at which a running split turns from green to yellow. */
        const val DYN_WARN_AT = 0.75f
        /** How far over the reference (as share) a finished split must be to turn red instead of yellow. */
        const val DYN_RED_AT = 0.15f

        val EMPTY_CELL = Cell("", 0xFFFFFF)
        val columnOptions = Column.entries.map { it.label }
        const val COLUMN_DESCRIPTION =
            "Time: real time. Tick Time: server ticks, ignores lag. Lag Lost: real time minus tick time. PB Diff: how far above / below your PB"
    }

    private val splitsHud by HudSetting(
        name = "Move Splits HUD",
        description = "Shows timers for each split",
        defaultText = name
    )

    private val column1 by SelectorSetting("Column 1", COLUMN_DESCRIPTION, Column.TIME.label, columnOptions)
    private val column2 by SelectorSetting("Column 2", COLUMN_DESCRIPTION, Column.TICK_TIME.label, columnOptions)
    private val column3 by SelectorSetting("Column 3", COLUMN_DESCRIPTION, Column.PB_DIFF.label, columnOptions)

    private val colorScheme by SelectorSetting(
        name = "Color Scheme",
        description = "Vexo: default colors. Odin: classic colors. Dynamic: split names and times turn green, yellow or red depending on your PB" +
            (if (hasTargets) " / target" else "") + ". Custom: pick every color yourself",
        default = ColorScheme.VEXO.label,
        options = ColorScheme.entries.filter { hasTargets || it != ColorScheme.DYNAMIC_TARGET }.map { it.label }
    )

    private fun customColor(name: String, description: String, default: Int) = ColorSetting(
        name = "Color: $name",
        description = description,
        default = Color(default),
        allowAlpha = false
    ).apply { dependsOn { ColorScheme.of(colorScheme) == ColorScheme.CUSTOM } }

    private val customLabel = customColor("Split Names", "Names of the splits", VEXO.label!!)
    private val customTime = customColor("Time", "Times of finished splits", VEXO.time)
    private val customCurrent = customColor("Current Split", "Time of the running split", VEXO.current)
    private val customTick = customColor("Tick Time", "Tick time column", VEXO.tick)
    private val customAhead = customColor("Ahead", "Faster than your PB", VEXO.ahead)
    private val customBehind = customColor("Behind", "Slower than your PB and lag lost", VEXO.behind)
    private val customPb = customColor("PB", "PB column", VEXO.pb)
    private val customSummary = customColor("Summary", "Run Pace, Total and the other summary rows", VEXO.summary!!)

    private val show0Time by BooleanSetting(
        name = "Show 0 splits",
        description = "Shows splits which have their time at 0",
        default = false
    )

    private val showRunPace by BooleanSetting(
        name = "Run Pace",
        description = "Fastest run time still possible with your PBs for the remaining splits",
        default = true
    )

    private val showTotalTime by BooleanSetting(
        name = "Total Time",
        description = "Shows the total run time",
        default = true
    )

    private val showLagLost by BooleanSetting(
        name = "Total Lag Lost",
        description = "Shows how much time the whole run lost to server lag",
        default = false
    )

    private val showCurrentSplit by BooleanSetting(
        name = "Current Split HUD",
        description = "Shows only the current split and its tick time",
        default = false
    )

    private val currentSplitHud by HudSetting(
        name = "Move Current Split HUD",
        defaultText = "§70.0s"
    ).dependsOn { showCurrentSplit }

    /** Split names shown on the move-HUD screen, without the summary rows. */
    protected abstract fun previewNames(): List<String>

    /** Target times of the tracked run, used by the "Dynamic (Target)" scheme. */
    protected open fun currentTargets(): PersonalBest? = null

    /** Turns the run's segments (without the total) into the rows shown on the HUD. */
    protected open fun displayRows(segments: List<Segment>): List<DisplayRow> = segments.map(::DisplayRow)

    /** Extra summary rows shown below Run Pace, with their names for the move-HUD preview. */
    protected open fun extraSummaryLines(columns: List<Column>, p: Palette): List<Line> = emptyList()
    protected open fun extraSummaryNames(): List<String> = emptyList()

    private fun activeColumns(): List<Column> = listOf(column1, column2, column3).map(Column::of).filter { it != Column.NONE }

    private fun palette(): Palette = when (ColorScheme.of(colorScheme)) {
        ColorScheme.ODIN, ColorScheme.DYNAMIC_PB, ColorScheme.DYNAMIC_TARGET -> ODIN
        ColorScheme.CUSTOM -> Palette(
            label = customLabel.getRGB() and 0xFFFFFF,
            time = customTime.getRGB() and 0xFFFFFF,
            current = customCurrent.getRGB() and 0xFFFFFF,
            tick = customTick.getRGB() and 0xFFFFFF,
            ahead = customAhead.getRGB() and 0xFFFFFF,
            behind = customBehind.getRGB() and 0xFFFFFF,
            pb = customPb.getRGB() and 0xFFFFFF,
            summary = customSummary.getRGB() and 0xFFFFFF,
            muted = VEXO.muted
        )
        ColorScheme.VEXO -> VEXO
    }

    protected fun render(event: HudRenderEvent) {
        val columns = activeColumns()
        val palette = palette()
        updatePreview(columns, palette)
        if (mc.gui.screen() is MoveActiveHudsGui) return
        if (SplitsManager.currentArea() != area) return
        val rows = SplitsManager.currentRows()
        if (rows.isEmpty()) return

        renderLines(event, buildLines(rows, columns, palette), columns)
        if (showCurrentSplit) renderCurrentSplit(event, rows, palette)
    }

    private fun buildLines(rows: List<SplitRow>, columns: List<Column>, p: Palette): List<Line> {
        val pb = SplitsManager.currentPersonalBest()
        val targets = currentTargets()
        fun millis(store: PersonalBest?, name: String): Long? = store?.get(name)?.let { (it * 1000).toLong() }
        fun segmentOf(row: SplitRow) = Segment(row, millis(pb, row.name), millis(targets, row.name))

        val scheme = ColorScheme.of(colorScheme)
        // Dynamic color for a row's name and time, or null to use the palette.
        fun dynamic(segment: Segment): Int? = when (scheme) {
            ColorScheme.DYNAMIC_PB -> dynamicColor(segment.row, segment.pb, DYN_GOLD)
            ColorScheme.DYNAMIC_TARGET -> dynamicColor(segment.row, segment.target, DYN_GREEN)
            else -> null
        }

        val total = segmentOf(rows.last())
        val segments = rows.dropLast(1).map(::segmentOf)

        val visibleRows = displayRows(segments).filter { it.segment.row.time != 0L || show0Time }
        val lines = visibleRows
            .mapIndexed { i, displayRow ->
                val segment = displayRow.segment
                val color = dynamic(segment)
                Line(
                    labelCell(segment.row.name, color, p),
                    columns.map { cell(it, segment.row, segment.pb, p, color) },
                    separatorBefore = i > 0 && displayRow.phase != visibleRows[i - 1].phase
                )
            }
            .toMutableList()

        if (showRunPace) {
            val pace = runPace(segments, total.row) { it.time }
            val tickPace = runPace(segments, total.row) { it.tickTime * 50 }
            // PB Diff follows whichever time base is shown, preferring real time.
            val diffPace = if (Column.TIME !in columns && Column.TICK_TIME in columns) tickPace else pace
            lines += Line(summaryLabel("§eRun Pace", p), columns.map { column ->
                when (column) {
                    Column.TIME -> pace?.let { Cell(formatTime(it), p.time) } ?: mutedCell(p)
                    Column.TICK_TIME -> tickPace?.let { Cell(formatTime(it), p.tick) } ?: mutedCell(p)
                    Column.PB_DIFF -> if (diffPace != null && total.pb != null) diffCell(diffPace - total.pb, p) else mutedCell(p)
                    else -> EMPTY_CELL
                }
            })
        }
        lines += extraSummaryLines(columns, p)
        if (showTotalTime) {
            val color = dynamic(total)
            val label = if (color != null) Cell("Total", color) else summaryLabel("§eTotal", p)
            lines += Line(label, columns.map { cell(it, total.row, total.pb, p, color) })
        }
        if (showLagLost && columns.isNotEmpty()) {
            lines += Line(summaryLabel("§cLag Lost", p), List(columns.size) { if (it == 0) lagCell(total.row.lag, p) else EMPTY_CELL })
        }
        return lines
    }

    private fun labelCell(name: String, dynamicColor: Int?, p: Palette): Cell = when {
        dynamicColor != null -> Cell(name.removeFormatting(), dynamicColor)
        p.label != null -> Cell(name.removeFormatting(), p.label)
        else -> Cell(name, 0xFFFFFF)
    }

    protected fun summaryLabel(name: String, p: Palette): Cell =
        if (p.summary != null) Cell(name.removeFormatting(), p.summary) else Cell(name, 0xFFFFFF)

    /**
     * Color of a split compared to [reference] (ms), or null if there is nothing to compare.
     * Running: green until [DYN_WARN_AT] of the reference, yellow until the reference, then red.
     * Finished: [beaten] if faster, yellow if at most [DYN_RED_AT] slower, otherwise red.
     */
    private fun dynamicColor(row: SplitRow, reference: Long?, beaten: Int): Int? {
        if (reference == null || reference <= 0 || row.time == 0L) return null
        val ratio = row.time.toFloat() / reference
        return when {
            row.completed && row.time < reference -> beaten
            row.completed -> if (ratio <= 1f + DYN_RED_AT) DYN_YELLOW else DYN_RED
            ratio < DYN_WARN_AT -> DYN_GREEN
            ratio < 1f -> DYN_YELLOW
            else -> DYN_RED
        }
    }

    /** @param dynamic Overrides the time color in the dynamic schemes. */
    private fun cell(column: Column, row: SplitRow, pb: Long?, p: Palette, dynamic: Int?): Cell = when (column) {
        Column.NONE -> EMPTY_CELL
        Column.TIME -> Cell(formatTime(row.time), dynamic ?: if (row.isCurrent) p.current else p.time)
        Column.TICK_TIME -> Cell(formatTime(row.tickTime * 50), p.tick)
        Column.LAG_LOST -> lagCell(row.lag, p)
        Column.PB -> pbCell(pb, p)
        Column.PB_DIFF -> when {
            pb == null || row.time == 0L -> mutedCell(p)
            row.completed -> diffCell(row.time - pb, p)
            row.isCurrent && row.time > pb -> diffCell(row.time - pb, p)
            else -> mutedCell(p)
        }
    }

    protected fun mutedCell(p: Palette) = Cell("-", p.muted)

    protected fun emptyCell() = EMPTY_CELL

    /** Lag is shown to 0.1s; finer values only reflect tick packet timing and make the text jitter. */
    protected fun lagCell(lag: Long, p: Palette): Cell {
        val rounded = lag / 100 * 100
        return if (rounded == 0L) Cell("0s", p.muted) else Cell("+${formatTime(rounded, 1)}", p.behind)
    }

    private fun diffCell(diff: Long, p: Palette): Cell =
        if (diff <= 0) Cell("-${formatTime(abs(diff))}", p.ahead) else Cell("+${formatTime(diff)}", p.behind)

    private fun pbCell(pb: Long?, p: Palette): Cell = pb?.let { Cell(formatTime(it), p.pb) } ?: mutedCell(p)

    /** A cell placed in unscaled pixels relative to the HUD origin. */
    private class Placed(val cell: Cell, val x: Int, val y: Int)

    private class Layout(val cells: List<Placed>, val separatorYs: List<Int>, val width: Int)

    private fun layout(lines: List<Line>, columns: List<Column>): Layout {
        val font = mc.font
        val labelWidth = lines.maxOf { font.width(it.label.text) }
        val columnWidths = columns.mapIndexed { i, column ->
            maxOf(font.width(column.sample), lines.maxOf { font.width(it.cells[i].text) })
        }
        val columnEnds = columnWidths.runningFold(labelWidth) { end, width -> end + COLUMN_GAP + width }.drop(1)

        var offset = 0
        val cells = mutableListOf<Placed>()
        val separatorYs = mutableListOf<Int>()
        lines.forEach { line ->
            if (line.separatorBefore) {
                separatorYs += offset - 1 + SEPARATOR_GAP / 2
                offset += SEPARATOR_GAP
            }
            cells += Placed(line.label, 0, offset)
            line.cells.forEachIndexed { i, cell ->
                if (cell.text.isNotEmpty()) cells += Placed(cell, columnEnds[i] - font.width(cell.text), offset)
            }
            offset += LINE_HEIGHT
        }
        return Layout(cells, separatorYs, columnEnds.lastOrNull() ?: labelWidth)
    }

    private fun renderLines(event: HudRenderEvent, lines: List<Line>, columns: List<Column>) {
        if (lines.isEmpty()) return
        val scale = splitsHud.scale
        val baseX = splitsHud.x.toFloat()
        val baseY = splitsHud.y.toFloat()
        val layout = layout(lines, columns)

        layout.cells.forEach { renderCell(event, it.cell, baseX + it.x * scale, baseY + it.y * scale, scale) }

        if (layout.separatorYs.isNotEmpty()) {
            val pose = event.context.pose()
            pose.pushMatrix()
            pose.translate(baseX, baseY)
            pose.scale(scale, scale)
            layout.separatorYs.forEach { y -> event.context.fill(0, y, layout.width, y + 1, SEPARATOR_COLOR) }
            pose.popMatrix()
        }
    }

    private fun renderCell(event: HudRenderEvent, cell: Cell, x: Float, y: Float, scale: Float) =
        renderString(event.context, cell.text, x, y, scale, cell.color or 0xFF000000.toInt())

    /** Sample lines for the move-HUD screen, drawn with the current colors and columns. */
    private fun updatePreview(columns: List<Column>, p: Palette) {
        fun sample(column: Column) = Cell(column.sample, when (column) {
            Column.NONE, Column.TIME -> p.time
            Column.TICK_TIME -> p.tick
            Column.LAG_LOST, Column.PB_DIFF -> p.behind
            Column.PB -> p.pb
        })
        val cells = columns.map(::sample)
        val summary = listOfNotNull("§eRun Pace".takeIf { showRunPace }) + extraSummaryNames() +
            listOfNotNull("§eTotal".takeIf { showTotalTime })
        val lines = previewNames().map { Line(labelCell(it, null, p), cells) } +
            summary.map { Line(summaryLabel(it, p), cells) } +
            listOfNotNull(Line(summaryLabel("§cLag Lost", p), List(columns.size) { if (it == 0) sample(Column.LAG_LOST) else EMPTY_CELL })
                .takeIf { showLagLost && columns.isNotEmpty() })

        splitsHud.text = lines.joinToString("\n") { it.label.text }
        splitsHud.preview = layout(lines, columns).cells.map { PreviewSegment(it.cell.text, it.x, it.y, it.cell.color) }
        currentSplitHud.preview = listOf(PreviewSegment("0.00s", 0, 0, p.tick))
    }

    /**
     * Fastest total time still reachable: finished segments count with their real time, the running
     * segment with max(elapsed, PB) and the remaining ones with their PB. Null if a needed PB is missing.
     * Never below the elapsed run time, and exactly the run time once the run is finished.
     *
     * @param time Time of a row in ms, real time or tick time.
     */
    private fun runPace(segments: List<Segment>, total: SplitRow, time: (SplitRow) -> Long): Long? {
        if (total.completed) return time(total)
        return segments.map { (row, pb) ->
            when {
                row.completed -> time(row)
                row.isCurrent -> pb?.let { maxOf(it, time(row)) }
                else -> pb
            }
        }.sumOrNull()?.let { maxOf(it, time(total)) }
    }

    protected fun List<Long?>.sumOrNull(): Long? = if (any { it == null }) null else sumOf { it!! }

    private fun renderCurrentSplit(event: HudRenderEvent, rows: List<SplitRow>, p: Palette) {
        val current = rows.find { it.isCurrent } ?: return
        val cell = Cell("${"%.2f".format(current.tickTime / 20f)}s", p.tick)
        renderCell(event, cell, currentSplitHud.x.toFloat(), currentSplitHud.y.toFloat(), currentSplitHud.scale)
    }
}
