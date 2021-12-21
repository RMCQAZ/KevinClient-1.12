package kevin.module.modules.movement.speeds.other

import kevin.KevinClient
import kevin.event.EventState
import kevin.event.PacketEvent
import kevin.event.PlayerMoveEvent
import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.IntegerValue
import kevin.module.modules.movement.Fly
import kevin.module.modules.movement.Step
import kevin.module.modules.movement.speeds.SpeedMode
import kevin.utils.EntityUtils.betterPosition
import kevin.utils.MovementUtils
import kevin.utils.MovementUtils.applyJumpBoostPotionEffects
import kevin.utils.MovementUtils.applySpeedPotionEffects
import kevin.utils.MovementUtils.speedEffectMultiplier
import kevin.utils.distanceSqTo
import kevin.utils.timers.TrollHackTimer
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.network.play.server.SPacketEntityVelocity
import net.minecraft.network.play.server.SPacketExplosion
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.util.*
import kotlin.math.*

@Suppress("NOTHING_TO_INLINE")
object Troll : SpeedMode("Troll") {

    private val bbtt = BooleanValue("2B2T", false)
    private val timerBoost = FloatValue("Timer Boost", 1.09f, 1.0f, 1.5f)
    private val baseSpeed = FloatValue("Base Speed", 0.2873f, 0.0f, 0.5f)
    private val maxStepSpeed = FloatValue("Max Step Speed", 0.35f, 0.0f, 0.5f)
    private val maxSpeed = FloatValue("Max Speed", 1.0f, 0.1f, 2.0f)
    private val airDecay = FloatValue("Air Decay", 0.9937f, 0.0f, 1.0f)
    private val autoJump = BooleanValue("Auto Jump", true)
    private val jumpMotion = FloatValue("Jump Motion", 0.4f, 0.1f, 0.5f)
    private val jumpBoost = FloatValue("Jump Boost", 2.0f, 1.0f, 4.0f)
    private val jumpDecay = FloatValue("Jump Decay", 0.66f, 0.0f, 1.0f)
    private val maxJumpSpeed = FloatValue("Max Jump Speed", 0.548f, 0.1f, 2.0f)
    private val jumpDelay = IntegerValue("Jump Delay", 5, 0, 10)

    private val velocityBoost = FloatValue("Velocity Boost", 0.5f, 0.0f, 2.0f)
    private val minBoostSpeed = FloatValue("Min Boost Speed", 0.2f, 0.0f, 1.0f)
    private val maxBoostSpeed = FloatValue("Max Boost Speed", 0.6f, 0.0f, 1.0f)
    private val maxYSpeed = FloatValue("Max Y Speed", 0.5f, 0.0f, 1.0f)
    private val boostDelay = IntegerValue("Boost Delay", 250, 0, 5000)
    private val boostTimeout = IntegerValue("Boost Timeout", 500, 0, 2500)
    private val boostDecay = FloatValue("Boost Decay", 0.98f, 0.1f, 1.0f)
    private val boostRange = FloatValue("Boost Range", 4.0f, 1.0f, 8.0f)

    private val wallCheck = BooleanValue("Wall Check", false)
    private val stepPause = IntegerValue("Step Pause", 4, 0, 20)
    private val reverseStepPause = IntegerValue("Reverse Step Pause", 0, 0, 10)
    private val hRange = FloatValue("H Range", 0.15f, 0.0f, 2.0f)

    private val boostTimer = TrollHackTimer()

    private var state = State.AIR
    private var moveSpeed = 0.0
    private var prevPos: Pair<Double,Double>? = null
    private var lastDist = 0.0

    private var strafeTicks = 0x22
    private var burrowTicks = 0x22
    private var jumpTicks = 0x22
    private var stepTicks = 0x22
    private var reverseStepPauseTicks = 0x22
    private var stepPauseTicks = 0x22
    private var inHoleTicks = 0x22
    private var rubberBandTicks = 0x22

    private var prevCollided = false
    private var prevSpeed = 0.0

    private var boostingSpeed = 0.0
    private val boostList = Collections.synchronizedCollection(ArrayList<BoostInfo>())
    private val stepHeights = doubleArrayOf(0.605, 1.005, 1.505, 2.005, 2.505)

    private enum class State {
        JUMP, DECAY, AIR
    }

    override fun onEnable() {
        reset()
        resetBoost()

        strafeTicks = 0x22
        burrowTicks = 0x22
        jumpTicks = 0x22
        stepTicks = 0x22
        reverseStepPauseTicks = 0x22
        stepPauseTicks = 0x22
        inHoleTicks = 0x22
        rubberBandTicks = 0x22
    }

    override fun onDisable() {
        reset()
    }

    override fun onPacket(event: PacketEvent) {
        when (event.packet) {
            is SPacketPlayerPosLook -> {
                rubberBandTicks = 0
                reset()
                resetBoost()
                boostTimer.reset(1000L)
            }
            is SPacketExplosion -> {
                handleVelocity(event, Vec3d(event.packet.x, event.packet.y, event.packet.z), hypot(event.packet.motionX.toDouble(), event.packet.motionZ.toDouble()), event.packet.motionY.toDouble())
            }
            is SPacketEntityVelocity -> {
                if (event.packet.entityID == mc.player.entityId) {
                    handleVelocity(event, null, hypot(event.packet.motionX / 8000.0, event.packet.motionZ / 8000.0) - moveSpeed, event.packet.motionY / 8000.0)
                }
            }
        }
    }

    override fun onPostTick() {
        strafeTicks++
        burrowTicks++
        jumpTicks++
        stepTicks++
        reverseStepPauseTicks++
        stepPauseTicks++
        inHoleTicks++
        rubberBandTicks++
    }

    override fun onPlayerTravel() {
        val playerPos = mc.player.betterPosition
        val box = mc.world.getBlockState(playerPos).getCollisionBoundingBox(mc.world, playerPos)
        if (box != null && box.maxY + playerPos.y > mc.player.posY) {
            burrowTicks = 0
        }

        if (!shouldStrafe()) {
            reset()
            return
        }

        if (isBurrowed()) mc.timer.timerSpeed = timerBoost.get()
        strafeTicks = 0
    }

    override fun onPlayerMove(event: PlayerMoveEvent) {
        when(event.eventState){
            EventState.PRE -> {
                if (shouldStrafe()) {
                    val yaw = MovementUtils.calcMoveYaw()
                    val motionX = -sin(yaw)
                    val motionZ = cos(yaw)
                    val baseSpeed = mc.player.applySpeedPotionEffects(baseSpeed.get().toDouble())

                    updateState(baseSpeed, motionX, motionZ)
                    updateFinalSpeed(baseSpeed)
                    if (boostTimeout.get() > 0) applyVelocityBoost()

                    val boostedSpeed = min(moveSpeed + boostingSpeed, mc.player.applySpeedPotionEffects(maxSpeed.get().toDouble()))
                    mc.player.motionX = motionX * baseSpeed
                    mc.player.motionZ = motionZ * baseSpeed
                    event.x = motionX * boostedSpeed
                    event.z = motionZ * boostedSpeed

                    if (!prevCollided && !mc.player.isCollidedHorizontally && jumpTicks > 2) {
                        prevSpeed = moveSpeed
                        stepTicks = 0
                    }
                    prevPos = mc.player.posX to mc.player.posZ
                    prevCollided = mc.player.isCollidedHorizontally
                } else if (strafeTicks <= 1) {
                    mc.player.motionX = 0.0
                    mc.player.motionZ = 0.0
                    event.x = 0.0
                    event.z = 0.0
                }
            }
            EventState.POST -> {
                if (jumpTicks == 0) {
                    mc.player.stepHeight = 0.6f
                }
                prevPos?.let {
                    lastDist = hypot(it.first - mc.player.posX, it.second - mc.player.posZ)
                }
                prevSpeed *= 0.9937106918238994
                updateVelocityBoost()
            }
        }
    }

    private inline fun handleVelocity(event: PacketEvent, pos: Vec3d?, speed: Double, motionY: Double) {
        if (velocityBoost.get() == 0.0f) return
        if (isBurrowed()) return
        if (abs(motionY) > maxYSpeed.get()) return
        if (pos != null && mc.player.distanceSqTo(pos) > (boostRange.get() * boostRange.get())) return

        val newSpeed = min(speed * velocityBoost.get(), maxBoostSpeed.get().toDouble())

        if (newSpeed >= minBoostSpeed.get()) {
            if (boostTimeout.get() == 0) {
                if (!prevCollided && !mc.player.isCollidedHorizontally) {
                    synchronized(boostList) {
                        if (newSpeed > boostingSpeed && boostTimer.tickAndReset(0L)) {
                            boostingSpeed = newSpeed
                            boostTimer.reset(boostDelay.get())
                            event.cancelEvent()
                        }
                    }
                }
            } else {
                synchronized(boostList) {
                    boostList.add(BoostInfo(pos, newSpeed, System.currentTimeMillis() + boostTimeout.get()))
                    event.cancelEvent()
                }
            }
        }
    }

    private inline fun shouldStrafe(): Boolean {
        return !mc.player.capabilities.isFlying
                && !mc.player.isElytraFlying
                && !mc.gameSettings.keyBindSneak.isKeyDown
                && !(mc.player.isInWater
                || mc.player.isInLava
                || mc.world.containsAnyLiquid(mc.player.entityBoundingBox.expand(0.0, -1.0, 0.0)))
                && !mc.player.isInWeb
                && !mc.player.isOnLadder
                && !KevinClient.moduleManager.getModule(Fly::class.java).state
                && MovementUtils.isMoving
    }

    private inline fun updateState(baseSpeed: Double, motionX: Double, motionZ: Double) {
        if (mc.player.onGround) {
            state = State.JUMP
        }

        when (state) {
            State.JUMP -> {
                if (mc.player.onGround) {
                    if (autoJump.get()) {
                        if (jumpTicks >= jumpDelay.get()) {
                            smartJump(motionX, motionZ)
                            state = State.DECAY
                        }
                    } else if (abs(mc.player.motionY - mc.player.applyJumpBoostPotionEffects(0.42)) <= 0.01) {
                        jump()
                        state = State.DECAY
                    }
                }
            }
            State.DECAY -> {
                val decayFactor = if (bbtt.get()) max(jumpDecay.get(), 0.795f) else jumpDecay.get()
                val jumpBoostDecay = decayFactor * (lastDist - baseSpeed)
                moveSpeed = lastDist - jumpBoostDecay
                state = State.AIR
            }
            State.AIR -> {
                var decayFactor = airDecay.get().toDouble()
                if (decayFactor == 0.9937) decayFactor = 0.9937106918238994
                moveSpeed = lastDist * decayFactor
            }
        }
    }

    private inline fun smartJump(motionX: Double, motionZ: Double) {
        val dist = calcBlockDistAhead(motionX * 6.0, motionZ * 6.0)
        val stepHeight = calcStepHeight(dist, motionX, motionZ)
        val multiplier = mc.player.speedEffectMultiplier

        if (wallCheck.get() && dist < 3.0 * multiplier && stepHeight > 1.114514) return
        val step = KevinClient.moduleManager.getModule(Step::class.java) as Step
        if (dist < 1.4 * multiplier && step.state && step.isValidHeight(stepHeight)) return
        if (stepPauseTicks < stepPause.get() || reverseStepPauseTicks < reverseStepPause.get()) return

        jump()
    }

    private inline fun AxisAlignedBB.toDetectBox(playerY: Double): AxisAlignedBB {
        return AxisAlignedBB(
            this.minX - hRange.get(), this.minY, this.minZ - hRange.get(),
            this.maxX + hRange.get(), max(this.maxX, playerY), this.maxZ + hRange.get()
        )
    }

    private inline fun calcBlockDistAhead(offsetX: Double, offsetZ: Double): Double {
        if (mc.player.isCollidedHorizontally) return 0.0

        val box = mc.player.entityBoundingBox
        val x = if (offsetX > 0.0) box.maxX else box.minX
        val z = if (offsetX > 0.0) box.maxZ else box.minZ

        return min(
            mc.world.rayTraceDist(Vec3d(x, box.minY + 0.6, z), offsetX, offsetZ),
            mc.world.rayTraceDist(Vec3d(x, box.maxY + 0.6, z), offsetX, offsetZ)
        )
    }

    private inline fun WorldClient.rayTraceDist(start: Vec3d, offsetX: Double, offsetZ: Double): Double {
        return this.rayTraceBlocks(start, start.addVector(offsetX, 0.0, offsetZ), false, true, false)
            ?.hitVec?.let {
                val x = start.x - it.x
                val z = start.z - it.z
                sqrt(x.pow(2) + z.pow(2))
            } ?: 999.0
    }

    private inline fun jump() {
        mc.player.motionY = calcJumpMotion()
        mc.player.isAirBorne = true
        mc.player.stepHeight = 0.0f
        jumpTicks = 0

        if (boostingSpeed > 0.1 || rubberBandTicks <= 2 || boostList.isNotEmpty()) {
            moveSpeed *= 1.2
        } else {
            moveSpeed = min(moveSpeed * jumpBoost.get(), mc.player.applySpeedPotionEffects(maxJumpSpeed.get().toDouble()))
        }
    }

    private inline fun calcJumpMotion(): Double {
        val motion = when {
            isBurrowed() -> 0.42
            jumpMotion.get() == 0.4F -> 0.399399995803833
            else -> jumpMotion.get().toDouble()
        }

        return mc.player.applyJumpBoostPotionEffects(motion)
    }

    private inline fun updateFinalSpeed(baseSpeed: Double) {
        if (!isBurrowed()) {
            moveSpeed = max(moveSpeed, baseSpeed)

            if (prevCollided && stepTicks < 10 && rubberBandTicks <= 2) {
                val stepSpeed = min(prevSpeed, mc.player.applySpeedPotionEffects(maxStepSpeed.get().toDouble()))
                moveSpeed = max(moveSpeed, stepSpeed)
                if (!mc.player.isCollidedHorizontally) prevSpeed = 0.0
            }
        } else {
            moveSpeed = baseSpeed
        }

        moveSpeed = min(moveSpeed, mc.player.applySpeedPotionEffects(maxSpeed.get().toDouble()))
    }

    private inline fun applyVelocityBoost() {
        if (isBurrowed()) {
            resetBoost()
        } else {
            val removeTime = System.currentTimeMillis()
            boostList.removeIf {
                it.time < removeTime || it.speed < 0.1
            }

            if (jumpTicks != 0 && boostTimer.tick(boostDelay.get())) {
                val rangeSq = (boostRange.get() * boostRange.get())
                synchronized(boostList) {
                    boostList.asSequence()
                        .filter { it.speed > boostingSpeed }
                        .filter { it.pos == null || mc.player.distanceSqTo(it.pos) <= rangeSq }
                        .maxByOrNull { it.speed }
                        ?.let {
                            boostingSpeed = it.speed
                            boostTimer.reset(boostDelay.get())
                            resetBoost()
                        }
                }
            }
        }
    }

    private inline fun updateVelocityBoost() {
        val decay = boostDecay.get() * if (mc.player.onGround) {
            val blockPos = BlockPos(mc.player.posX, mc.player.entityBoundingBox.minY - 1.0, mc.player.posZ)
            val blockState = mc.world.getBlockState(blockPos)
            blockState.block.slipperiness * 0.91
        } else {
            0.91
        }

        synchronized(boostList) {
            boostList.forEach {
                it.speed *= decay
            }
        }

        if (mc.player.isCollidedHorizontally) {
            boostingSpeed = 0.0
        } else {
            boostingSpeed *= decay
        }
    }

    private inline fun calcStepHeight(dist: Double, motionX: Double, motionZ: Double): Double {
        val pos = mc.player.betterPosition
        if (mc.world.getBlockState(pos).getCollisionBoundingBox(mc.world, pos) != null) return 0.0

        val i = max(dist.roundToInt(), 1)
        var minStepHeight = Double.MAX_VALUE

        val x = motionX * i
        val z = motionZ * i
        minStepHeight = checkBox(minStepHeight, x, 0.0)
        minStepHeight = checkBox(minStepHeight, 0.0, z)

        return if (minStepHeight == Double.MAX_VALUE) 0.0 else minStepHeight
    }

    private inline fun checkBox(minStepHeight: Double, offsetX: Double, offsetZ: Double): Double {
        val box = mc.player.entityBoundingBox.offset(offsetX, 0.0, offsetZ)
        if (!mc.world.collidesWithAnyBlock(box)) return minStepHeight

        var stepHeight = minStepHeight

        for (y in stepHeights) {
            if (y > minStepHeight) break

            val stepBox = AxisAlignedBB(
                box.minX, box.minY + y - 0.5, box.minZ,
                box.maxX, box.minY + y, box.maxZ
            )
            val boxList = mc.world.getCollisionBoxes(null, stepBox)
            val maxHeight = boxList.maxOfOrNull { it.maxY } ?: continue
            val maxStepHeight = maxHeight - mc.player.posY

            if (!mc.world.collidesWithAnyBlock(box.offset(0.0, maxStepHeight, 0.0))) {
                stepHeight = maxStepHeight
                break
            }
        }

        return stepHeight
    }

    fun reset() {
        mc.player?.jumpMovementFactor = 0.02f
        mc.timer.timerSpeed = 1F

        state = State.AIR
        moveSpeed = mc.player?.applySpeedPotionEffects(baseSpeed.get().toDouble()) ?: baseSpeed.get().toDouble()
        prevPos = null
        lastDist = 0.0

        prevCollided = false
        prevSpeed = 0.0

        boostingSpeed = 0.0
    }

    private inline fun resetBoost() {
        boostList.clear()
    }

    private inline fun isBurrowed(): Boolean {
        return burrowTicks < 10
    }

    fun resetReverseStep() {
        stepPauseTicks = 0
    }

    fun resetStep() {
        stepPauseTicks = 0
        prevSpeed = 0.0
    }

    private class BoostInfo(
        val pos: Vec3d?,
        var speed: Double,
        val time: Long
    )
}