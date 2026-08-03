package xyz.vexo.utils

import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.decoration.ArmorStand
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.phys.Vec3
import xyz.vexo.Vexo.mc
import xyz.vexo.events.EventHandler
import xyz.vexo.events.impl.ClientTickEvent
import xyz.vexo.events.impl.PacketSendEvent
import xyz.vexo.events.impl.RunEndEvent
import xyz.vexo.features.impl.kuudra.BackboneAlert


object BonemerangRendTracker {
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

    /**
     * @returns a boolean Value whether Bonemerang is traveling
     */
    fun isRendActive(): Boolean = rendTicksRemaining > 0

    fun isTracking(): Boolean = ticksRemaining > 0 || isRendActive()

    /**
     * @return Progress toward the Rend window opening, 0..1. 0 if no timer is currently running.
     */
    fun progress(): Float {
        if (startingTicks <= 0) return 0f
        val elapsed = startingTicks - ticksRemaining
        return (elapsed / startingTicks.toFloat()).coerceIn(0f, 1f)
    }

    @EventHandler
    fun onPacket(event: PacketSendEvent) {
        val packet = event.packet
        if (packet !is ServerboundUseItemPacket || packet.hand != InteractionHand.MAIN_HAND) return

        val player = mc.player ?: return
        val held = player.mainHandItem
        if (held.isEmpty || cooldownTicks > 0) return

        val name = held.hoverName.string.removeFormatting().lowercase()
        if ("bonemerang" !in name) return

        cooldownTicks = BACKBONE_COOLDOWN_TICKS
        throwOrigin = player.eyePosition
        val ticks = BACKBONE_TICKS - BackboneAlert.advanceTicks.toInt() + BackboneAlert.tickAdjustment.toInt()
        startBackboneTimer(maxOf(1, ticks))
    }

    @EventHandler
    fun onTick(@Suppress("UNUSED_PARAMETER") event: ClientTickEvent) {
        val player = mc.player ?: return reset()
        if (mc.level == null) return reset()

        trackBackHit(player)
        tick()
    }

    @EventHandler
    fun onRunEnd(@Suppress("UNUSED_PARAMETER") event: RunEndEvent) {
        reset()
    }

    private fun startBackboneTimer(ticks: Int) {
        startingTicks = ticks
        ticksRemaining = ticks
        rendTicksRemaining = 0
        trackedBoneEntityId = -1
        throwScanTicks = THROW_SCAN_TICKS
        hitEntityIds.clear()
    }

    private fun tick() {
        if (ticksRemaining > 0) {
            ticksRemaining--
            if (ticksRemaining <= 0) rendTicksRemaining = REND_TICKS
        }
        if (cooldownTicks > 0) cooldownTicks--
        if (rendTicksRemaining > 0) rendTicksRemaining--
    }

    private fun trackBackHit(player: Player) {
        if (ticksRemaining <= 0 || isRendActive()) return

        val boneStand = getTrackedBoneStand(player) ?: return
        val bonePos = boneStand.position()

        val hit = livingEntities().firstOrNull { entity ->
            entity.isAlive && entity.id != player.id && entity !is ArmorStand &&
                entity.distanceToSqr(bonePos) <= hitDistanceSq(entity) &&
                isBehindTarget(entity, bonePos) &&
                hitEntityIds.add(entity.id)
        } ?: return

        ticksRemaining = 0
        rendTicksRemaining = REND_TICKS
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
}
