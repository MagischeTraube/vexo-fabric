package xyz.vexo.features.impl.kuudra

import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import xyz.vexo.Vexo.mc
import xyz.vexo.config.impl.BooleanSetting
import xyz.vexo.config.impl.HudSetting
import xyz.vexo.config.impl.SelectorSetting
import xyz.vexo.config.impl.SliderSetting
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.HudRenderEvent
import xyz.vexo.events.impl.PacketSendEvent
import xyz.vexo.features.Module
import xyz.vexo.hud.MoveActiveHudsGui


object BackboneAlert : Module(
    name = "Backbone Alert",
    description = "Shows a HUD bar timing the Bonemerang backbone and alerts you to Rend",
    toggled = false
) {
    private val hud by HudSetting(
        name = "Move HUD",
        defaultText = "§a████████████ §b60%"
    )

    private val playSound by BooleanSetting(
        "Sound",
        "Play a sound when the alert fires",
        default = true
    )

    private val advanceTicks by SliderSetting(
        "Advance (Ticks)",
        "Trigger the Rend this many ticks earlier (recommended 2)",
        default = 2.0, min = 0.0, max = 10.0, increment = 1.0
    )

    private val tickAdjustment by SliderSetting(
        "Tick Adjustment",
        "Fine-tune the timing: shift the Rend later (+) or earlier (-)",
        default = 0.0, min = -10.0, max = 10.0, increment = 1.0
    )

    private val rendHud by HudSetting(
        name = "Move REND HUD",
        defaultText = "§l§aREND NOW",
        defaultScale = REND_TEXT_SCALE
    )

    private val rendColor by SelectorSetting(
        "REND Farbe",
        "Farbe der großen REND NOW Anzeige",
        default = "Grün",
        options = listOf("Grün", "Rot")
    )

    private const val BACKBONE_TICKS = 22
    private const val BACKBONE_COOLDOWN_TICKS = 32
    private const val THROW_SCAN_TICKS = 15
    private const val REND_TICKS = 20

    private var ticksRemaining = 0
    private var startingTicks = 0
    private var rendTicksRemaining = 0
    private var cooldownTicks = 0

    private val hitEntityIds = HashSet<Int>()
    private var trackedBoneEntityId = -1
    private var throwScanTicks = 0
    private var throwOrigin: Vec3 = Vec3.ZERO

    private val formatting = Regex("§.")

    override fun onDisable() {
        super.onDisable()
        reset()
    }

    @EventHandler
    fun onPacket(event: PacketSendEvent) {
        val packet = event.packet
        if (packet !is ServerboundUseItemPacket || packet.hand != InteractionHand.MAIN_HAND) return

        val player = mc.player ?: return
        val held = player.mainHandItem
        if (held.isEmpty || cooldownTicks > 0) return

        val name = formatting.replace(held.hoverName.string, "").lowercase()
        if ("bonemerang" !in name) return

        cooldownTicks = BACKBONE_COOLDOWN_TICKS
        throwOrigin = player.eyePosition
        startBackboneTimer(maxOf(1, BACKBONE_TICKS - advanceTicks.toInt() + tickAdjustment.toInt()))
    }

    @EventHandler
    fun onTick(event: ClientTickEvent) {
        val player = mc.player ?: return reset()
        if (mc.level == null) return reset()

        val trackedBackHit = trackBackHit(player)
        val triggeredRend = tick()

        if ((triggeredRend || trackedBackHit) && playSound) {
            player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 2.0f, 1.0f)
        }
    }

    @EventHandler
    fun onRender(event: HudRenderEvent) {
        if (mc.screen is MoveActiveHudsGui || !shouldDisplay()) return

        val ctx = event.context
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(hud.x.toFloat(), hud.y.toFloat())
        pose.scale(hud.scale, hud.scale)

        // The bar only shows progress now; the "REND NOW" alert lives in the separate overlay below.
        renderProgressBar(ctx)

        pose.popMatrix()

        // Separate, larger overlay that screams "REND NOW" the moment the window opens.
        if (isRendActive()) renderRendOverlay(ctx)
    }

    private fun renderRendOverlay(ctx: net.minecraft.client.gui.GuiGraphicsExtractor) {
        val pose = ctx.pose()
        pose.pushMatrix()
        pose.translate(rendHud.x.toFloat(), rendHud.y.toFloat())
        // Scale is baked into the HUD's default scale (REND_TEXT_SCALE) so the move-GUI preview matches.
        pose.scale(rendHud.scale, rendHud.scale)

        // Pulse the alpha so the big alert is impossible to miss.
        val pulse = (Math.sin(System.currentTimeMillis() / 90.0) * 0.5 + 0.5).toFloat()
        val alpha = (0xBB + (0x44 * pulse)).toInt() and 0xFF
        val rgb = if (rendColor == "Rot") 0xFF5555 else 0x55FF55
        val color = (alpha shl 24) or rgb

        val label = Component.literal("REND NOW").withStyle { it.withBold(true) }
        ctx.text(mc.font, label, 0, 0, color)

        pose.popMatrix()
    }

    private fun renderProgressBar(ctx: net.minecraft.client.gui.GuiGraphicsExtractor) {
        val percent = currentPercent()
        val filled = Math.round(percent * (BAR_WIDTH - 2))

        val barColor = when {
            percent > 0.85f -> 0xFF55FF55.toInt()
            percent > 0.6f -> 0xFFFFAA00.toInt()
            else -> 0xFFFF5555.toInt()
        }

        // Border + track.
        ctx.fill(-1, -1, BAR_WIDTH + 1, BAR_HEIGHT + 1, 0xFF000000.toInt())
        ctx.fill(0, 0, BAR_WIDTH, BAR_HEIGHT, 0xCC1C1C1C.toInt())

        // Filled portion with a lighter highlight line on top.
        if (filled > 0) {
            ctx.fill(1, 1, 1 + filled, BAR_HEIGHT - 1, barColor)
            ctx.fill(1, 1, 1 + filled, 2, (barColor and 0x00FFFFFF) or (0x66 shl 24))
        }

        // Percentage to the right of the bar.
        val text = Component.literal("${Math.round(percent * 100f)}%").withStyle { it.withColor(0x55FFFF) }
        ctx.text(mc.font, text, BAR_WIDTH + 4, (BAR_HEIGHT - mc.font.lineHeight) / 2 + 1, 0xFF55FFFF.toInt())
    }

    // --- Backbone timing logic (ported from IQ's BackboneAlertManager) ---

    private fun startBackboneTimer(ticks: Int) {
        startingTicks = ticks
        ticksRemaining = ticks
        rendTicksRemaining = 0
        trackedBoneEntityId = -1
        throwScanTicks = THROW_SCAN_TICKS
        hitEntityIds.clear()
    }

    private fun isRendActive() = rendTicksRemaining > 0

    private fun shouldDisplay() = ticksRemaining > 0 || isRendActive()

    private fun currentPercent(): Float {
        if (startingTicks <= 0) return 0f
        val elapsed = startingTicks - ticksRemaining
        return (elapsed / startingTicks.toFloat()).coerceIn(0f, 1f)
    }

    /** Advances the timers one tick; returns true if the Rend window just opened. */
    private fun tick(): Boolean {
        var triggeredRend = false
        if (ticksRemaining > 0) {
            ticksRemaining--
            if (ticksRemaining <= 0) {
                rendTicksRemaining = REND_TICKS
                triggeredRend = true
            }
        }
        if (cooldownTicks > 0) cooldownTicks--
        if (rendTicksRemaining > 0) rendTicksRemaining--
        return triggeredRend
    }

    private fun trackBackHit(player: Player): Boolean {
        if (ticksRemaining <= 0 || isRendActive()) return false

        val boneStand = getTrackedBoneStand(player) ?: return false
        val bonePos = boneStand.position()

        val hit = livingEntities().firstOrNull { entity ->
            entity.isAlive && entity.id != player.id && entity !is ArmorStand &&
                entity.distanceToSqr(bonePos) <= hitDistanceSq(entity) &&
                isBehindTarget(entity, bonePos) &&
                hitEntityIds.add(entity.id)
        } ?: return false

        ticksRemaining = 0
        rendTicksRemaining = REND_TICKS
        return true
    }

    private fun getTrackedBoneStand(player: Player): ArmorStand? {
        (player.level().getEntity(trackedBoneEntityId) as? ArmorStand)?.let {
            if (isBoneStand(it)) return it
        }
        trackedBoneEntityId = -1
        if (throwScanTicks-- <= 0) return null

        val viewDirection = player.getViewVector(1.0f).normalize()
        var nearest: ArmorStand? = null
        var nearestDistance = Double.MAX_VALUE

        for (stand in armorStands()) {
            if (!isBoneStand(stand)) continue
            val standPos = stand.position()
            if (standPos.distanceToSqr(throwOrigin) > 26.0 * 26.0) continue

            val fromPlayer = standPos.subtract(player.position())
            if (fromPlayer.lengthSqr() <= 0.01) continue
            if (fromPlayer.normalize().dot(viewDirection) < 0.35) continue

            val distance = fromPlayer.lengthSqr()
            if (distance < nearestDistance) {
                nearestDistance = distance
                nearest = stand
            }
        }

        nearest?.let { trackedBoneEntityId = it.id }
        return nearest
    }

    private fun isBoneStand(stand: ArmorStand): Boolean {
        val held = stand.mainHandItem
        return !held.isEmpty && held.`is`(Items.BONE)
    }

    private fun hitDistanceSq(entity: LivingEntity): Double {
        val radius = 0.6 + (entity.bbWidth / 2.0)
        return radius * radius
    }

    private fun isBehindTarget(target: LivingEntity, bonePos: Vec3): Boolean {
        val toBone = bonePos.subtract(target.position())
        val horizontalToBone = Vec3(toBone.x, 0.0, toBone.z)
        if (horizontalToBone.lengthSqr() <= 1.0E-4) return false

        val look = target.getViewVector(1.0f)
        val horizontalLook = Vec3(look.x, 0.0, look.z)
        if (horizontalLook.lengthSqr() <= 1.0E-4) return false

        return horizontalLook.normalize().dot(horizontalToBone.normalize()) < -0.35
    }

    private fun livingEntities(): List<LivingEntity> {
        val level = mc.level ?: return emptyList()
        return level.entitiesForRendering().filterIsInstance<LivingEntity>()
    }

    private fun armorStands(): List<ArmorStand> {
        val level = mc.level ?: return emptyList()
        return level.entitiesForRendering().filterIsInstance<ArmorStand>()
    }

    private fun reset() {
        ticksRemaining = 0
        startingTicks = 0
        rendTicksRemaining = 0
        cooldownTicks = 0
        trackedBoneEntityId = -1
        throwScanTicks = 0
        throwOrigin = Vec3.ZERO
        hitEntityIds.clear()
    }

    private const val BAR_WIDTH = 72
    private const val BAR_HEIGHT = 9
    private const val REND_TEXT_SCALE = 3.0f
}
