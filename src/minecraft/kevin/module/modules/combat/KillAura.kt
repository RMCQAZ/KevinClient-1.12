package kevin.module.modules.combat

import kevin.KevinClient
import kevin.event.*
import kevin.module.*
import kevin.module.modules.exploit.TP
import kevin.module.modules.misc.HideAndSeekHack
import kevin.module.modules.misc.Teams
import kevin.utils.*
import kevin.utils.timers.MSTimer
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.MobEffects
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.GameType
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*
import kotlin.math.*

class KillAura : Module("KillAura","Automatically attacks targets around you.", Keyboard.KEY_R, ModuleCategory.COMBAT) {
    /**
     * OPTIONS
     */

    // CPS - Attack speed
    private val maxCPS: IntegerValue = object : IntegerValue("MaxCPS", 8, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minCPS.get()
            if (i > newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), this.get())
        }
    }

    private val minCPS: IntegerValue = object : IntegerValue("MinCPS", 5, 1, 20) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxCPS.get()
            if (i < newValue) set(i)

            attackDelay = TimeUtils.randomClickDelay(this.get(), maxCPS.get())
        }
    }

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

    // Range
    private val rangeValue = FloatValue("Range", 3.7f, 1f, 8f)
    private val throughWallsRangeValue = FloatValue("ThroughWallsRange", 3f, 0f, 8f)
    private val rangeSprintReducementValue = FloatValue("RangeSprintReducement", 0f, 0f, 0.4f)

    // Modes
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "Direction", "LivingTime"), "Distance")
    private val targetModeValue = ListValue("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")

    // Bypass
    private val swingValue = BooleanValue("Swing", true)
    private val keepSprintValue = BooleanValue("KeepSprint", true)

    // AutoBlock
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Packet", "AfterTick", "Keep"), "Packet")
    private val interactAutoBlockValue = BooleanValue("InteractAutoBlock", true)
    private val blockRate = IntegerValue("BlockRate", 100, 1, 100)

    private val coolDownValue = FloatValue("CoolDown", 1f, 0f, 1f)

    // Raycast
    private val raycastValue = BooleanValue("RayCast", true)
    private val raycastIgnoredValue = BooleanValue("RayCastIgnored", false)
    private val livingRaycastValue = BooleanValue("LivingRayCast", true)

    // Bypass
    private val aacValue = BooleanValue("AAC", false)

    // Turn Speed
    private val maxTurnSpeed: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 0f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minTurnSpeed.get()
            if (v > newValue) set(v)
        }
    }

    private val minTurnSpeed: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 0f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxTurnSpeed.get()
            if (v < newValue) set(v)
        }
    }

    private val silentRotationValue = BooleanValue("SilentRotation", true)
    private val rotationStrafeValue = ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off")
    private val randomCenterValue = BooleanValue("RandomCenter", true)
    private val outborderValue = BooleanValue("Outborder", false)
    private val fovValue = FloatValue("FOV", 180f, 0f, 180f)

    // Predict
    private val predictValue = BooleanValue("Predict", true)

    private val maxPredictSize: FloatValue = object : FloatValue("MaxPredictSize", 1f, 0.1f, 5f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minPredictSize.get()
            if (v > newValue) set(v)
        }
    }

    private val minPredictSize: FloatValue = object : FloatValue("MinPredictSize", 1f, 0.1f, 5f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxPredictSize.get()
            if (v < newValue) set(v)
        }
    }

    // Bypass
    private val failRateValue = FloatValue("FailRate", 0f, 0f, 100f)
    private val fakeSwingValue = BooleanValue("FakeSwing", true)
    private val noInventoryAttackValue = BooleanValue("NoInvAttack", false)
    private val noInventoryDelayValue = IntegerValue("NoInvDelay", 200, 0, 500)
    private val limitedMultiTargetsValue = IntegerValue("LimitedMultiTargets", 0, 0, 50)

    // Visuals
    private val markValue = BooleanValue("Mark", true)
    private val fakeSharpValue = BooleanValue("FakeSharp", true)

    /**
     * MODULE
     */

    // Target
    var target: Entity? = null
    private var currentTarget: Entity? = null
    private var hitable = false
    private val prevTargetEntities = mutableListOf<Int>()

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0L
    private var clicks = 0

    // Container Delay
    private var containerOpen = -1L

    // Fake block status
    var blockingStatus = false

    /**
     * Enable kill aura module
     */
    override fun onEnable() {
        mc.player ?: return
        mc.world ?: return

        updateTarget()
    }

    //Keep AutoBlock
    private val keepAutoBlock: Boolean
    get() = autoBlockValue equal "Keep"

    /**
     * Disable kill aura module
     */
    override fun onDisable() {
        target = null
        currentTarget = null
        hitable = false
        prevTargetEntities.clear()
        attackTimer.reset()
        clicks = 0

        stopBlocking()
    }

    /**
     * Motion event
     */
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.POST) {
            target ?: return
            currentTarget ?: return

            // Update hitable
            updateHitable()

            // AutoBlock
            if (autoBlockValue.get().equals("AfterTick", true) && canBlock)
                startBlocking(currentTarget!!, hitable)

            return
        }

        if (rotationStrafeValue.get().equals("Off", true))
            update()
    }

    /**
     * Strafe event
     */
    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (rotationStrafeValue.get().equals("Off", true))
            return

        update()

        if (currentTarget != null && RotationUtils.targetRotation != null) {
            when (rotationStrafeValue.get().toLowerCase()) {
                "strict" -> {
                    val (yaw) = RotationUtils.targetRotation ?: return
                    var strafe = event.strafe
                    var forward = event.forward
                    val friction = event.friction

                    var f = strafe * strafe + forward * forward

                    if (f >= 1.0E-4F) {
                        f = sqrt(f)

                        if (f < 1.0F)
                            f = 1.0F

                        f = friction / f
                        strafe *= f
                        forward *= f

                        val yawSin = sin((yaw * Math.PI / 180F).toFloat())
                        val yawCos = cos((yaw * Math.PI / 180F).toFloat())

                        val player = mc.player!!

                        player.motionX += strafe * yawCos - forward * yawSin
                        player.motionZ += forward * yawCos + strafe * yawSin
                    }
                    event.cancelEvent()
                }
                "silent" -> {
                    update()

                    RotationUtils.targetRotation.applyStrafeToPlayer(event)
                    event.cancelEvent()
                }
            }
        }
    }

    fun update() {
        if (cancelRun || (noInventoryAttackValue.get() && ((mc.currentScreen)is GuiContainer ||
                    System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())))
            return

        // Update target
        updateTarget()

        if (target == null) {
            stopBlocking()
            return
        }

        // Target
        currentTarget = target

        if (!targetModeValue.get().equals("Switch", ignoreCase = true) && isEnemy(currentTarget))
            target = currentTarget
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent){
        val packet: Packet<*> = event.packet

        if (packet is CPacketPlayer) {
            if (RotationUtils.targetRotation != null && !RotationUtils.keepCurrentRotation && (RotationUtils.targetRotation.yaw != RotationUtils.serverRotation.yaw || RotationUtils.targetRotation.pitch != RotationUtils.serverRotation.pitch)) {
                packet.yaw = RotationUtils.targetRotation.yaw
                packet.pitch = RotationUtils.targetRotation.pitch
                packet.rotating = true
            }
            if (packet.rotating) RotationUtils.serverRotation = Rotation(packet.yaw, packet.pitch)
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onTick(event: TickEvent?) {
        if (RotationUtils.targetRotation != null) {
            RotationUtils.keepLength--
            if (RotationUtils.keepLength <= 0) RotationUtils.reset()
        }
        if (RotationUtils.random.nextGaussian() > 0.8) RotationUtils.x = Math.random()
        if (RotationUtils.random.nextGaussian() > 0.8) RotationUtils.y = Math.random()
        if (RotationUtils.random.nextGaussian() > 0.8) RotationUtils.z = Math.random()
    }

    /**
     * Update event
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {

        // Debug ChatUtils().message("Yaw:${RotationUtils.serverRotation.yaw} Pitch:${RotationUtils.serverRotation.pitch}")

        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
            return
        }

        if (noInventoryAttackValue.get() && ((mc.currentScreen)is GuiContainer ||
                    System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())) {
            target = null
            currentTarget = null
            hitable = false
            if ((mc.currentScreen)is GuiContainer) containerOpen = System.currentTimeMillis()
            return
        }

        if (target != null && currentTarget != null && mc.player!!.getCooledAttackStrength(0.0F) >= coolDownValue.get()) {
            while (clicks > 0) {
                runAttack()
                clicks--
            }
        }
    }

    /**
     * Render event
     */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (cancelRun) {
            target = null
            currentTarget = null
            hitable = false
            stopBlocking()
            return
        }

        if (noInventoryAttackValue.get() && ((mc.currentScreen) is GuiContainer ||
                    System.currentTimeMillis() - containerOpen < noInventoryDelayValue.get())) {
            target = null
            currentTarget = null
            hitable = false
            if ((mc.currentScreen) is GuiContainer) containerOpen = System.currentTimeMillis()
            return
        }

        target ?: return

        if (markValue.get() && !targetModeValue.get().equals("Multi", ignoreCase = true))
            RenderUtils.drawPlatform(target, if (hitable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70))

        if (currentTarget != null && attackTimer.hasTimePassed(attackDelay) &&
            (currentTarget !is EntityLivingBase || (currentTarget as EntityLivingBase).hurtTime <= hurtTimeValue.get())) {
            clicks++
            attackTimer.reset()
            attackDelay = TimeUtils.randomClickDelay(minCPS.get(), maxCPS.get())
        }
    }

    /**
     * Handle entity move
     */
    @EventTarget
    fun onEntityMove(event: EntityMovementEvent) {
        val movedEntity = event.movedEntity

        if (target == null || movedEntity != currentTarget)
            return

        updateHitable()
    }

    /**
     * Attack enemy
     */
    private fun runAttack() {
        target ?: return
        currentTarget ?: return
        val thePlayer = mc.player ?: return
        val theWorld = mc.world ?: return

        // Settings
        val failRate = failRateValue.get()
        val swing = swingValue.get()
        val multi = targetModeValue.get().equals("Multi", ignoreCase = true)
        val openInventory = aacValue.get() && (mc.currentScreen) is GuiContainer
        val failHit = failRate > 0 && Random().nextInt(100) <= failRate

        // Close inventory when open
        if (openInventory)
            mc.connection!!.sendPacket(CPacketCloseWindow())

        // Check is not hitable or check failrate

        if (!hitable || failHit) {
            if (swing && (fakeSwingValue.get() || failHit))
                thePlayer.swingArm(EnumHand.MAIN_HAND)
        } else {
            // Attack
            if (!multi) {
                attackEntity(currentTarget!!)
            } else {
                var targets = 0

                for (entity in theWorld.loadedEntityList) {
                    val distance = thePlayer.getDistanceToEntityBox(entity)

                    if ((entity is EntityLivingBase || HideAndSeekHack.isHider(entity)) && isEnemy(entity) && distance <= getRange(entity)) {
                        attackEntity(entity)

                        targets += 1

                        if (limitedMultiTargetsValue.get() != 0 && limitedMultiTargetsValue.get() <= targets)
                            break
                    }
                }
            }

            prevTargetEntities.add(if (aacValue.get()) target!!.entityId else currentTarget!!.entityId)

            if (target == currentTarget)
                target = null
        }

        // Open inventory
        if (openInventory)
            mc.connection!!.sendPacket(CPacketEntityAction(mc.player,CPacketEntityAction.Action.OPEN_INVENTORY))
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        // Reset fixed target to null
        target = null

        // Settings
        val hurtTime = hurtTimeValue.get()
        val fov = fovValue.get()
        val switchMode = targetModeValue.get().equals("Switch", ignoreCase = true)

        // Find possible targets
        val targets = mutableListOf<Entity>()

        val theWorld = mc.world!!
        val thePlayer = mc.player!!

        for (entity in theWorld.loadedEntityList) {
            if ((entity !is EntityLivingBase && !HideAndSeekHack.isHider(entity)) || !isEnemy(entity) || (switchMode && prevTargetEntities.contains(entity.entityId)))
                continue

            val distance = thePlayer.getDistanceToEntityBox(entity)
            val entityFov = RotationUtils.getRotationDifference(entity)

            if (distance <= maxRange && (fov == 180F || entityFov <= fov) && (entity !is EntityLivingBase || entity.hurtTime <= hurtTime))
                targets.add(entity)
        }

        // Sort targets by priority
        when (priorityValue.get().toLowerCase()) {
            "distance" -> targets.sortBy { thePlayer.getDistanceToEntityBox(it) } // Sort by distance
            "health" -> targets.sortBy { if (it is EntityLivingBase) it.health else thePlayer.getDistanceToEntityBox(it).toFloat() } // Sort by health
            "direction" -> targets.sortBy { RotationUtils.getRotationDifference(it) } // Sort by FOV
            "livingtime" -> targets.sortBy { -it.ticksExisted } // Sort by existence
        }

        // Find best target
        for (entity in targets) {
            // Update rotations to current target
            if (!updateRotations(entity)) // when failed then try another target
                continue

            // Set target to current entity
            target = entity
            return
        }

        // Cleanup last targets when no target found and try again
        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    /**
     * Check if [entity] is selected as enemy with current target options and other modules
     */
    private fun isEnemy(entity: Entity?): Boolean {
        if (HideAndSeekHack.isHider(entity)){
            return true
        } else if (HideAndSeekHack.isSeeker()) return false
        if ((entity) is EntityLivingBase && (EntityUtils.targetDeath || isAlive(entity)) && entity != mc.player) {
            if (!EntityUtils.targetInvisible && entity.isInvisible)
                return false

            if (EntityUtils.targetPlayer && (entity) is EntityPlayer) {

                if (entity.isSpectator /**|| AntiBot.isBot(player)**/ ) return false
/**
                if (player.isClientFriend() && !LiquidBounce.moduleManager[NoFriends::class.java].state)
                    return false
 **/
                val teams = KevinClient.moduleManager.getModule("Teams") as Teams

                return !teams.state || !teams.isInYourTeam(entity)
            }

            return EntityUtils.targetMobs && entity.isMob() || EntityUtils.targetAnimals && entity.isAnimal()
        }

        return false
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: Entity) {
        // Stop blocking
        val thePlayer = mc.player!!

        if ((/**thePlayer.isBlocking ||**/ blockingStatus)&&!keepAutoBlock)
            stopBlocking()

        // Call attack event
        EventManager.callEvent(AttackEvent(entity))

        // Attack target

        mc.connection!!.sendPacket(CPacketUseEntity(entity))

        if (swingValue.get()) thePlayer.swingArm(EnumHand.MAIN_HAND)

        if (keepSprintValue.get()) {
            // Critical Effect
            if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder &&
                !thePlayer.isInWater && !thePlayer.isPotionActive(MobEffects.BLINDNESS) && !thePlayer.isRiding)
                thePlayer.onCriticalHit(entity)

            // Enchant Effect
            if (entity is EntityLivingBase && EnchantmentHelper.getModifierForCreature(thePlayer.heldItemMainhand, entity.creatureAttribute) > 0F)
                thePlayer.onEnchantmentCritical(entity)
        } else {
            if (mc.playerController.currentGameType != GameType.SPECTATOR)
                thePlayer.attackTargetEntityWithCurrentItem(entity)
        }

        // Extra critical effects
        val criticals = KevinClient.moduleManager.getModule("Criticals") as Criticals

        for (i in 0..2) {
            // Critical Effect
            if (thePlayer.fallDistance > 0F && !thePlayer.onGround && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isPotionActive(MobEffects.BLINDNESS) && thePlayer.ridingEntity == null || criticals.state && criticals.msTimer.hasTimePassed(criticals.delayValue.get().toLong()) && !thePlayer.isInWater && !thePlayer.isInLava && !thePlayer.isInWeb)
                thePlayer.onCriticalHit(target!!)

            // Enchant Effect
            if (target is EntityLivingBase && EnchantmentHelper.getModifierForCreature(thePlayer.heldItemMainhand, (target as EntityLivingBase).creatureAttribute) > 0.0f || fakeSharpValue.get())
                thePlayer.onEnchantmentCritical(target!!)
        }

        // Start blocking after attack
        if ((autoBlockValue.get().equals("Packet", true)||keepAutoBlock) && (/**thePlayer.isBlocking ||**/ canBlock))
            startBlocking(entity, interactAutoBlockValue.get())

        thePlayer.resetCooldown()
    }

    /**
     * Update killaura rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        if (maxTurnSpeed.get() <= 0F)
            return true

        var boundingBox = entity.entityBoundingBox

        if (predictValue.get())
            boundingBox = boundingBox.offset(
                (entity.posX - entity.prevPosX - (mc.player!!.posX - mc.player!!.prevPosX)) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                (entity.posY - entity.prevPosY - (mc.player!!.posY - mc.player!!.prevPosY)) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get()),
                (entity.posZ - entity.prevPosZ - (mc.player!!.posZ - mc.player!!.prevPosZ)) * RandomUtils.nextFloat(minPredictSize.get(), maxPredictSize.get())
            )

        val (_, rotation) = RotationUtils.searchCenter(
            boundingBox,
            outborderValue.get() && !attackTimer.hasTimePassed(attackDelay / 2),
            randomCenterValue.get(),
            predictValue.get(),
            mc.player!!.getDistanceToEntityBox(entity) < throughWallsRangeValue.get(),
            maxRange
        ) ?: return false

        val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, rotation,
            (Math.random() * (maxTurnSpeed.get() - minTurnSpeed.get()) + minTurnSpeed.get()).toFloat())

        if (silentRotationValue.get())
            RotationUtils.setTargetRotation(limitedRotation, if (aacValue.get()) 15 else 0)
        else
            limitedRotation.toPlayer(mc.player!!)

        return true
    }

    /**
     * Check if enemy is hitable with current rotations
     */
    private fun updateHitable() {
        // Disable hitable check if turn speed is zero
        if (maxTurnSpeed.get() <= 0F) {
            hitable = true
            return
        }

        val reach = min(maxRange.toDouble(), mc.player!!.getDistanceToEntityBox(target!!)) + 1

        if (raycastValue.get()) {
            val raycastedEntity = RaycastUtils.raycastEntity(reach, object : RaycastUtils.EntityFilter {
                override fun canRaycast(entity: Entity?): Boolean {
                    return (!livingRaycastValue.get() || (((entity) is EntityLivingBase || HideAndSeekHack.isHider(entity)) && (entity) !is EntityArmorStand)) &&
                            (isEnemy(entity) || raycastIgnoredValue.get() || aacValue.get() && mc.world!!.getEntitiesWithinAABBExcludingEntity(entity, entity!!.entityBoundingBox).isNotEmpty())
                }

            })

            if (raycastValue.get() && raycastedEntity != null && (raycastedEntity is EntityLivingBase || HideAndSeekHack.isHider(raycastedEntity))
                /**&& (LiquidBounce.moduleManager[NoFriends::class.java].state || !(classProvider.isEntityPlayer(raycastedEntity) && raycastedEntity.asEntityPlayer().isClientFriend()))**/)
                currentTarget = raycastedEntity

            hitable = if (maxTurnSpeed.get() > 0F) currentTarget == raycastedEntity else true
        } else
            hitable = RotationUtils.isFaced(currentTarget, reach)
    }

    /**
     * Start blocking
     */
    private fun startBlocking(interactEntity: Entity, interact: Boolean) {
        if (!(blockRate.get() > 0 && Random().nextInt(100) <= blockRate.get()))
            return

        if (interact) {
            val positionEye = mc.renderViewEntity?.getPositionEyes(1F)

            val expandSize = interactEntity.collisionBorderSize.toDouble()
            val boundingBox = interactEntity.entityBoundingBox.expand(expandSize, expandSize, expandSize)

            val (yaw, pitch) = RotationUtils.targetRotation ?: Rotation(mc.player!!.rotationYaw, mc.player!!.rotationPitch)
            val yawCos = cos(-yaw * 0.017453292F - Math.PI.toFloat())
            val yawSin = sin(-yaw * 0.017453292F - Math.PI.toFloat())
            val pitchCos = -cos(-pitch * 0.017453292F)
            val pitchSin = sin(-pitch * 0.017453292F)
            val range = min(maxRange.toDouble(), mc.player!!.getDistanceToEntityBox(interactEntity)) + 1
            val lookAt = positionEye!!.addVector(yawSin * pitchCos * range, pitchSin * range, yawCos * pitchCos * range)

            val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
            val hitVec = movingObject.hitVec

            mc.connection!!.sendPacket(CPacketUseEntity(interactEntity, EnumHand.MAIN_HAND, Vec3d(
                hitVec.x - interactEntity.posX,
                hitVec.y - interactEntity.posY,
                hitVec.z - interactEntity.posZ)
            ))
            mc.connection!!.sendPacket(CPacketUseEntity(interactEntity, EnumHand.MAIN_HAND))
        }

        mc.connection!!.sendPacket(CPacketPlayerTryUseItemOnBlock(
            BlockPos(-1, -1, -1),
            EnumFacing.DOWN, EnumHand.MAIN_HAND, 0.0F, 0.0F, 0.0F))
        blockingStatus = true
    }


    /**
     * Stop blocking
     */
    private fun stopBlocking() {
        if (blockingStatus) {
            mc.connection!!.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            blockingStatus = false
        }
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun: Boolean
        inline get() = mc.player!!.isSpectator || !isAlive(mc.player!!)
                || KevinClient.moduleManager.getModule("Blink")!!.state || KevinClient.moduleManager.getModule("FreeCam")!!.state || (KevinClient.moduleManager.getModule("TP")!!.state&&(KevinClient.moduleManager.getModule("TP") as TP).mode.get().equals("AAC",true))

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0 ||
            aacValue.get() && entity.hurtTime > 5

    /**
     * Check if player is able to block
     */
    private val canBlock: Boolean
        inline get() = false // mc.player!!.heldItemMainhand != null && (mc.player!!.heldItemMainhand.item) is ItemSword

    /**
     * Range
     */
    private val maxRange: Float
        get() = max(rangeValue.get(), throughWallsRangeValue.get())

    private fun getRange(entity: Entity) =
        (if (mc.player!!.getDistanceToEntityBox(entity) >= throughWallsRangeValue.get()) rangeValue.get() else throughWallsRangeValue.get()) - if (mc.player!!.isSprinting) rangeSprintReducementValue.get() else 0F

    /**
     * HUD Tag
     */
    override val tag: String
        get() = targetModeValue.get()

    val isBlockingChestAura: Boolean
        get() = this.state && target != null
}