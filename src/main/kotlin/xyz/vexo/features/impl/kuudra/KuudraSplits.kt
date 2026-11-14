package xyz.vexo.features.impl.kuudra

import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.features.SplitsModule
import xyz.vexo.utils.PersonalBest
import xyz.vexo.utils.SplitArea
import xyz.vexo.utils.SplitsManager

/**
 * Split timers for Kuudra with personal bests per tier.
 */
object KuudraSplits : SplitsModule(
    name = "Kuudra Splits",
    description = "Provides visual timers for Kuudra",
    area = SplitArea.KUUDRA,
    hasTargets = false
) {
    /** Kuudra personal bests keyed by tier (1-5). */
    val kuudraPBs: Map<Int, PersonalBest> = (1..5).associateWith { PersonalBest("KuudraT$it") }

    /** Kept as a list so [initSettings] registers (and saves) every store. */
    private val pbStores: List<PersonalBest> = kuudraPBs.values.toList()

    @EventHandler
    fun onRender(event: HudRenderEvent) = render(event)

    override fun previewNames(): List<String> = SplitsManager.kuudraSplitNames(5).dropLast(1)
}
