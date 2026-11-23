package xyz.vexo.features.impl.dungeons

import xyz.vexo.config.Setting
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.features.SplitsModule
import xyz.vexo.utils.DungeonFloor
import xyz.vexo.utils.PersonalBest
import xyz.vexo.utils.PersonalBestTimeSetting
import xyz.vexo.utils.SplitArea
import xyz.vexo.utils.SplitRow
import xyz.vexo.utils.SplitsManager
import xyz.vexo.utils.floor7SplitGroup
import xyz.vexo.utils.formatTime
import xyz.vexo.utils.removeFormatting
import xyz.vexo.utils.terminalSectionNames

/**
 * Split timers for Dungeons with editable personal bests and target times.
 */
object DungeonSplits : SplitsModule(
    name = "Dungeon Splits",
    description = "Provides visual timers for Dungeons",
    area = SplitArea.DUNGEON,
    hasTargets = true
) {
    private const val TERMINALS_LABEL = "§6Terminals"
    private const val CLEAR_SPLITS = 3

    private val separatorLines by BooleanSetting(
        name = "Separator Lines",
        description = "Draws a line between the clear phase and the boss phase",
        default = false
    )

    private val terminalSections by BooleanSetting(
        name = "Terminal Sections",
        description = "Shows S1 - S4 indented below the Terminals row",
        default = true
    )

    private val bossEntrySplit by BooleanSetting(
        name = "Boss Entry Split",
        description = "Adds a split for boss entry",
        default = true
    )

    private val showScore300 by BooleanSetting(
        name = "300 Score Time",
        description = "Shows when the run reached 300 score (Floor 4 - 7)",
        default = false
    )

    val dungeonPBs: Map<DungeonFloor, PersonalBest> =
        DungeonFloor.entries.associateWith { PersonalBest("Dungeon${it.name}") }

    /** Kept as a list so [initSettings] registers (and saves) every store. */
    private val pbStores: List<PersonalBest> = dungeonPBs.values.toList()
    private val targetStores: List<PersonalBest> = dungeonPBs.keys.map { PersonalBest("Target${it.name}") }

    /** Target times for the "Dynamic (Target)" scheme, keyed like [dungeonPBs]. */
    val dungeonTargets: Map<DungeonFloor, PersonalBest> = dungeonPBs.keys.zip(targetStores).toMap()

    private val editTimes by SelectorSetting(
        name = "Edit Times",
        description = "Whether the fields below edit your personal bests or your target times",
        default = "Personal Bests",
        options = listOf("Personal Bests", "Targets")
    )

    private val pbMode by SelectorSetting(
        name = "PB Mode",
        description = "Which mode's times to show below",
        default = "Normal",
        options = listOf("Normal", "Master Mode")
    )

    private val pbFloor by SelectorSetting(
        name = "PB Floor",
        description = "Which floor's times to show below",
        default = "Floor 7",
        options = (1..7).map { "Floor $it" }
    )

    private fun selectedPbFloor(): DungeonFloor =
        DungeonFloor.valueOf((if (pbMode == "Master Mode") "M" else "F") + pbFloor.removePrefix("Floor "))

    /** One text field per split of every floor; only the selected floor's fields are shown. */
    private fun timeEditors(stores: Map<DungeonFloor, PersonalBest>, mode: String, description: String): List<Setting<*>> =
        stores.filterKeys { it != DungeonFloor.E }.flatMap { (floor, store) ->
            SplitsManager.dungeonSplitNames(floor).map { key ->
                PersonalBestTimeSetting(
                    name = key.removeFormatting(),
                    description = description,
                    pb = store,
                    key = key
                ).dependsOn { editTimes == mode && selectedPbFloor() == floor }
            }
        }

    private val pbEditors = timeEditors(
        dungeonPBs, "Personal Bests",
        "Your personal best for this split, e.g. 1m 23.45s. Leave empty to clear it"
    )

    private val targetEditors = timeEditors(
        dungeonTargets, "Targets",
        "Your target time for this split, used by the Dynamic (Target) color scheme. Leave empty to clear it"
    )

    @EventHandler
    fun onRender(event: HudRenderEvent) = render(event)

    override fun previewNames(): List<String> = floor7SplitGroup.map { it.name }.flatMap { name ->
        when {
            name != terminalSectionNames.first() -> listOf(name)
            terminalSections -> listOf(TERMINALS_LABEL, name)
            else -> listOf(TERMINALS_LABEL)
        }
    }.filter { terminalSections || it !in terminalSectionNames }.map(::indented)

    override fun currentTargets(): PersonalBest? = SplitsManager.currentDungeonFloor()?.let { dungeonTargets[it] }

    override fun displayRows(segments: List<Segment>): List<DisplayRow> {
        val merged = if (terminalSections) segments else mergeTerminals(segments)
        fun phase(index: Int) = if (separatorLines && index >= CLEAR_SPLITS) 1 else 0

        val rows = merged.mapIndexed { i, segment -> segment to phase(i) }.toMutableList()
        if (bossEntrySplit && merged.size > CLEAR_SPLITS) {
            val entry = merged.take(CLEAR_SPLITS)
            val entryRow = SplitRow(
                "§9Boss Entry",
                entry.sumOf { it.row.time },
                entry.sumOf { it.row.tickTime },
                isCurrent = false,
                completed = entry.last().row.completed,
                lag = entry.sumOf { it.row.lag }
            )
            rows.add(CLEAR_SPLITS, Segment(entryRow, entry.map { it.pb }.sumOrNull(), entry.map { it.target }.sumOrNull()) to 0)
        }
        if (terminalSections) {
            val firstSection = rows.indexOfFirst { it.first.row.name in terminalSectionNames }
            if (firstSection >= 0) {
                val sections = rows.map { it.first }.filter { it.row.name in terminalSectionNames }
                rows.add(firstSection, terminalsSegment(sections) to rows[firstSection].second)
            }
        }
        return rows.map { (segment, phase) ->
            DisplayRow(segment.copy(row = segment.row.copy(name = indented(segment.row.name))), phase)
        }
    }

    override fun extraSummaryNames(): List<String> = listOfNotNull("§a300 Score".takeIf { showScore300 })

    override fun extraSummaryLines(columns: List<Column>, p: Palette): List<Line> {
        val floorNumber = SplitsManager.currentDungeonFloor()?.number ?: 0
        if (!showScore300 || floorNumber !in 4..7) return emptyList()
        val reached = SplitsManager.score300Time()
        return listOf(Line(summaryLabel("§a300 Score", p), columns.map { column ->
            when (column) {
                Column.TIME -> reached?.let { Cell(formatTime(it.time), p.time) } ?: mutedCell(p)
                Column.TICK_TIME -> reached?.let { Cell(formatTime(it.ticks * 50), p.tick) } ?: mutedCell(p)
                Column.LAG_LOST -> reached?.let { lagCell((it.time - it.ticks * 50).coerceAtLeast(0), p) } ?: mutedCell(p)
                else -> emptyCell()
            }
        }))
    }

    private fun indented(name: String) =
        if (terminalSections && name in terminalSectionNames) " §7- $name" else name

    /** Replaces the S1 - S4 rows with one Terminals row. */
    private fun mergeTerminals(segments: List<Segment>): List<Segment> {
        val sections = segments.filter { it.row.name in terminalSectionNames }
        if (sections.isEmpty()) return segments
        val firstIndex = segments.indexOf(sections.first())
        return segments.filterNot { it in sections }.toMutableList().apply { add(firstIndex, terminalsSegment(sections)) }
    }

    private fun terminalsSegment(sections: List<Segment>) = Segment(
        SplitRow(
            TERMINALS_LABEL,
            sections.sumOf { it.row.time },
            sections.sumOf { it.row.tickTime },
            isCurrent = sections.any { it.row.isCurrent },
            completed = sections.all { it.row.completed },
            lag = sections.sumOf { it.row.lag }
        ),
        sections.map { it.pb }.sumOrNull(),
        sections.map { it.target }.sumOrNull()
    )
}
