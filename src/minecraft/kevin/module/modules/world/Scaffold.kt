package kevin.module.modules.world

import kevin.event.*
import kevin.font.FontManager
import kevin.module.*
import kevin.utils.*
import kevin.utils.BlockUtils.canBeClicked
import kevin.utils.BlockUtils.isReplaceable
import kevin.utils.RotationUtils.wrapAngleTo180_float
import kevin.utils.timers.MSTimer
import kevin.utils.timers.TickTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockBush
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.stats.StatList
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.*

class Scaffold : Module("Scaffold", "Automatically places blocks beneath your feet.", category = ModuleCategory.WORLD) {
    //private val modeValue = ListValue("Mode", arrayOf("Normal", "Expand"), "Normal")
    private val towerModeValue = ListValue(
        "TowerMode",
        arrayOf("Jump", "Motion", "ConstantMotion", "MotionTP", "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4"),
        "Jump"
    )

    //private val matrixValue = BooleanValue("TowerMatrix", false)
    private val towerNoMoveValue = BooleanValue("TowerNoMove",false)

    // ConstantMotion
    private val constantMotionValue = FloatValue("TowerConstantMotion", 0.42f, 0.1f, 1f)
    private val constantMotionJumpGroundValue = FloatValue("TowerConstantMotionJumpGround", 0.79f, 0.76f, 1f)

    // Teleport
    private val teleportHeightValue = FloatValue("TowerTeleportHeight", 1.15f, 0.1f, 5f)
    private val teleportDelayValue = IntegerValue("TowerTeleportDelay", 0, 0, 20)
    private val teleportGroundValue = BooleanValue("TowerTeleportGround", true)
    private val teleportNoMotionValue = BooleanValue("TowerTeleportNoMotion", false)

    private val towerFakeJump = BooleanValue("TowerFakeJump",true)

    // Delay
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minDelay = minDelayValue.get()
            if (minDelay > newValue) set(minDelay)
        }
    }

    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 0, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelay = maxDelayValue.get()
            if (maxDelay < newValue) set(maxDelay)
        }
    }

    // Placeable delay
    private val placeDelay = BooleanValue("PlaceDelay", true)

    // Autoblock
    private val autoBlockValue = ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")

    // Basic stuff
    @JvmField
    val sprintValue = BooleanValue("Sprint", false)
    private val autoJump = BooleanValue("AutoJump",false)
    private val swingValue = BooleanValue("Swing", true)
    private val searchValue = BooleanValue("Search", true)
    private val downValue = BooleanValue("Down", true)
    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post")

    // Eagle
    private val eagleValue = ListValue("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal")
    private val blocksToEagleValue = IntegerValue("BlocksToEagle", 0, 0, 10)
    private val edgeDistanceValue = FloatValue("EagleEdgeDistance", 0f, 0f, 0.5f)

    // Expand
    private val expandLengthValue = IntegerValue("ExpandLength", 0, 0, 6)

    // Rotation Options
    private val strafeMode = ListValue("Strafe", arrayOf("Off", "AAC"), "Off")
    private val rotationsValue = BooleanValue("Rotations", true)
    private val silentRotationValue = BooleanValue("SilentRotation", true)
    private val keepRotationValue = BooleanValue("KeepRotation", true)
    private val keepLengthValue = IntegerValue("KeepRotationLength", 0, 0, 20)

    // XZ/Y range
    private val searchMode = ListValue("XYZSearch", arrayOf("Auto", "AutoCenter", "Manual"), "AutoCenter")
    private val xzRangeValue = FloatValue("xzRange", 0.8f, 0f, 1f)
    private var yRangeValue = FloatValue("yRange", 0.8f, 0f, 1f)
    private val minDistValue = FloatValue("MinDist", 0.0f, 0.0f, 0.2f)

    // Search Accuracy
    private val searchAccuracyValue: IntegerValue = object : IntegerValue("SearchAccuracy", 8, 1, 16) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            if (maximum < newValue) {
                set(maximum)
            } else if (minimum > newValue) {
                set(minimum)
            }
        }
    }

    // Turn Speed
    private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 180f, 1f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = minTurnSpeedValue.get()
            if (v > newValue) set(v)
            if (maximum < newValue) {
                set(maximum)
            } else if (minimum > newValue) {
                set(minimum)
            }
        }
    }
    private val minTurnSpeedValue: FloatValue = object : FloatValue("MinTurnSpeed", 180f, 1f, 180f) {
        override fun onChanged(oldValue: Float, newValue: Float) {
            val v = maxTurnSpeedValue.get()
            if (v < newValue) set(v)
            if (maximum < newValue) {
                set(maximum)
            } else if (minimum > newValue) {
                set(minimum)
            }
        }
    }
    //跳跃检测
    private val jumpCheckValue = BooleanValue("JumpCheck",false)
    //向下检测
    private val downCheckValue = BooleanValue("DownCheck",true)

    // Zitter
    private val zitterMode = ListValue("Zitter", arrayOf("Off", "Teleport", "Smooth"), "Off")
    private val zitterSpeed = FloatValue("ZitterSpeed", 0.13f, 0.1f, 0.3f)
    private val zitterStrength = FloatValue("ZitterStrength", 0.05f, 0f, 0.2f)

    // Game
    private val timerValue = FloatValue("Timer", 1f, 0.1f, 10f)
    private val speedModifierValue = FloatValue("SpeedModifier", 1f, 0f, 2f)
    private val slowValue = BooleanValue("Slow", false)
    private val slowSpeed = FloatValue("SlowSpeed", 0.6f, 0.2f, 0.8f)

    // Safety
    private val sameYValue = BooleanValue("SameY", false)
    private val sameYJumpUp = BooleanValue("SameYJumpUp", false)
    private val safeWalkValue = BooleanValue("SafeWalk", true)
    private val airSafeValue = BooleanValue("AirSafe", false)

    // Visuals
    private val counterDisplayValue = BooleanValue("Counter", true)
    private val markValue = BooleanValue("Mark", false)

// Variables

    // Target block
    private var targetPlace: PlaceInfo? = null

    // Rotation lock
    private var lockRotation: Rotation? = null
    private var lockRotationTimer = TickTimer()


    // Launch position
    private var launchY = 0
    private var facesBlock = false

    // AutoBlock
    private var slot = 0

    // Zitter Direction
    private var zitterDirection = false

    // Delay
    private val delayTimer = MSTimer()
    private val zitterTimer = MSTimer()
    private var delay = 0L

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking: Boolean = false

    // Downwards
    private var shouldGoDown: Boolean = false

    // ENABLING MODULE
    override fun onEnable() {
        if (mc.player == null) return
        launchY = mc.player!!.posY.toInt()
        slot = mc.player!!.inventory.currentItem
        facesBlock = false
    }

    private fun fakeJump() {
        if(!towerFakeJump.get()) return
        mc.player!!.isAirBorne = true
        mc.player!!.addStat(StatList.JUMP)
    }

    private var jumpGround = 0.0
    private val timer = TickTimer()

    /**
     * Move player
     */
    private fun move() {
        val thePlayer = mc.player ?: return

        if (towerNoMoveValue.get()){
            mc.player.motionX = .0
            mc.player.motionZ = .0
        }

        when (towerModeValue.get().lowercase(Locale.getDefault())) {
            "motion" -> if (thePlayer.onGround) {
                fakeJump()
                thePlayer.motionY = 0.42
            } else if (thePlayer.motionY < 0.1) {
                thePlayer.motionY = -0.3
            }
            "motiontp" -> if (thePlayer.onGround) {
                fakeJump()
                thePlayer.motionY = 0.42
            } else if (thePlayer.motionY < 0.23) {
                thePlayer.setPosition(thePlayer.posX, truncate(thePlayer.posY), thePlayer.posZ)
            }
            "packet" -> if (thePlayer.onGround && timer.hasTimePassed(2)) {
                fakeJump()
                mc.connection!!.sendPacket(
                    CPacketPlayer.Position(
                        thePlayer.posX,
                        thePlayer.posY + 0.42, thePlayer.posZ, false
                    )
                )
                mc.connection!!.sendPacket(
                    CPacketPlayer.Position(
                        thePlayer.posX,
                        thePlayer.posY + 0.753, thePlayer.posZ, false
                    )
                )
                thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 1.0, thePlayer.posZ)
                timer.reset()
            }
            "teleport" -> {
                if (teleportNoMotionValue.get()) {
                    thePlayer.motionY = 0.0
                }
                if ((thePlayer.onGround || !teleportGroundValue.get()) && timer.hasTimePassed(teleportDelayValue.get())) {
                    fakeJump()
                    thePlayer.setPositionAndUpdate(
                        thePlayer.posX,
                        thePlayer.posY + teleportHeightValue.get(),
                        thePlayer.posZ
                    )
                    timer.reset()
                }
            }
            "constantmotion" -> {
                if (thePlayer.onGround) {
                    fakeJump()
                    jumpGround = thePlayer.posY
                    thePlayer.motionY = constantMotionValue.get().toDouble()
                }
                if (thePlayer.posY > jumpGround + constantMotionJumpGroundValue.get()) {
                    fakeJump()
                    thePlayer.setPosition(
                        thePlayer.posX,
                        truncate(thePlayer.posY),
                        thePlayer.posZ
                    )
                    thePlayer.motionY = constantMotionValue.get().toDouble()
                    jumpGround = thePlayer.posY
                }
            }
            "aac3.3.9" -> {
                if (thePlayer.onGround) {
                    fakeJump()
                    thePlayer.motionY = 0.4001
                }
                mc.timer.timerSpeed = 1f
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY -= 0.00000945
                    mc.timer.timerSpeed = 1.6f
                }
            }
            "aac3.6.4" -> if (thePlayer.ticksExisted % 4 == 1) {
                thePlayer.motionY = 0.4195464
                thePlayer.setPosition(thePlayer.posX - 0.035, thePlayer.posY, thePlayer.posZ)
            } else if (thePlayer.ticksExisted % 4 == 0) {
                thePlayer.motionY = -0.5
                thePlayer.setPosition(thePlayer.posX + 0.035, thePlayer.posY, thePlayer.posZ)
            }
        }
    }

    private fun sameY(): Boolean = sameYValue.get() && (!sameYJumpUp.get()||!mc.gameSettings.keyBindJump.isKeyDown)

// UPDATE EVENTS

    /** @param */

    @EventTarget
    fun onUpdate(event: UpdateEvent) {

        //if (event.eventState == UpdateState.OnUpdate) return

        if (!sameY()) launchY = mc.player!!.posY.toInt()

        mc.timer.timerSpeed = timerValue.get()
        shouldGoDown =
            downValue.get() && !sameY() && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && blocksAmount > 1
        if (shouldGoDown) {
            mc.gameSettings.keyBindSneak.pressed = false
        }
        if (slowValue.get()) {
            mc.player!!.motionX = mc.player!!.motionX * slowSpeed.get()
            mc.player!!.motionZ = mc.player!!.motionZ * slowSpeed.get()
        }
        if (mc.player!!.onGround) {
            when (zitterMode.get().toLowerCase()) {
                "off" -> return
                "smooth" -> {
                    if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
                        mc.gameSettings.keyBindRight.pressed = false
                    }
                    if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
                        mc.gameSettings.keyBindLeft.pressed = false
                    }
                    if (zitterTimer.hasTimePassed(100)) {
                        zitterDirection = !zitterDirection
                        zitterTimer.reset()
                    }
                    if (zitterDirection) {
                        mc.gameSettings.keyBindRight.pressed = true
                        mc.gameSettings.keyBindLeft.pressed = false
                    } else {
                        mc.gameSettings.keyBindRight.pressed = false
                        mc.gameSettings.keyBindLeft.pressed = true
                    }
                }
                "teleport" -> {
                    MovementUtils.strafe(zitterSpeed.get())
                    val yaw: Double =
                        Math.toRadians(mc.player!!.rotationYaw + if (zitterDirection) 90.0 else -90.0)
                    mc.player!!.motionX = mc.player!!.motionX - sin(yaw) * zitterStrength.get()
                    mc.player!!.motionZ = mc.player!!.motionZ + cos(yaw) * zitterStrength.get()
                    zitterDirection = !zitterDirection
                }
            }
        }
        // Eagle
        if (!eagleValue.get().equals("Off", true) && !shouldGoDown) {
            var dif = 0.5
            if (edgeDistanceValue.get() > 0 && !shouldGoDown) {
                for (facingType in EnumFacing.values()) {
                    if (facingType != EnumFacing.NORTH && facingType != EnumFacing.EAST && facingType != EnumFacing.SOUTH && facingType != EnumFacing.WEST)
                        continue
                    val blockPosition = BlockPos(
                        mc.player!!.posX,
                        mc.player!!.posY - 1.0,
                        mc.player!!.posZ
                    )
                    val neighbor = blockPosition.offset(facingType, 1)
                    if (mc.world!!.getBlockState(neighbor).block == (Blocks.AIR)) {
                        val calcDif = (if (facingType == EnumFacing.NORTH || facingType == EnumFacing.SOUTH)
                            abs((neighbor.z + 0.5) - mc.player!!.posZ) else
                            abs((neighbor.x + 0.5) - mc.player!!.posX)) - 0.5
                        if (calcDif < dif)
                            dif = calcDif
                    }
                }
            }
            if (placedBlocksWithoutEagle >= blocksToEagleValue.get()) {
                val shouldEagle: Boolean = mc.world!!.getBlockState(
                    BlockPos(
                        mc.player!!.posX,
                        mc.player!!.posY - 1.0,
                        mc.player!!.posZ
                    )
                ).block == (Blocks.AIR) || dif < edgeDistanceValue.get()
                if (eagleValue.get().equals("Silent", true) && !shouldGoDown) {
                    if (eagleSneaking != shouldEagle) {
                        mc.connection!!.sendPacket(
                            CPacketEntityAction(
                                mc.player!!, if (shouldEagle)
                                    CPacketEntityAction.Action.START_SNEAKING
                                else
                                    CPacketEntityAction.Action.STOP_SNEAKING
                            )
                        )
                    }
                    eagleSneaking = shouldEagle
                } else {
                    mc.gameSettings.keyBindSneak.pressed = shouldEagle
                    placedBlocksWithoutEagle = 0
                }
            } else {
                placedBlocksWithoutEagle++
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.player == null) return
        val packet = event.packet
        if ((packet) is CPacketHeldItemChange) {
            slot = packet.slotId
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (strafeMode.get().equals("Off", true))
            return

        update()
        if (rotationsValue.get()
            && (keepRotationValue.get() || !lockRotationTimer.hasTimePassed(keepLengthValue.get()))
            && lockRotation != null
        ) {
            if (targetPlace == null) {
                var yaw = 0F
                for (i in 0..7) {
                    if (abs(
                            RotationUtils.getAngleDifference(
                                lockRotation!!.yaw,
                                (i * 45).toFloat()
                            )
                        ) < abs(RotationUtils.getAngleDifference(lockRotation!!.yaw, yaw))
                    ) {
                        yaw = wrapAngleTo180_float((i * 45).toFloat())
                    }
                }
                lockRotation!!.yaw = yaw
            }
            setRotation(lockRotation!!)
            lockRotationTimer.update()
        }
        lockRotation?.applyStrafeToPlayer(event)
        event.cancelEvent()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val eventState: EventState = event.eventState
        //AutoJump
        if (mc.player.onGround
            && mc.player.jumpTicks == 0
            && MovementUtils.isMoving
            && !mc.player.isInLava
            && !mc.player.isInWater
            && !mc.player.isInWeb
            && !mc.player.isOnLadder
            && !mc.gameSettings.keyBindJump.isKeyDown
            && autoJump.get()
            && eventState == EventState.PRE){
            mc.player.jump()
        }

        // Lock Rotation
        if (rotationsValue.get()
            && (keepRotationValue.get() || !lockRotationTimer.hasTimePassed(keepLengthValue.get()))
            && lockRotation != null
            && strafeMode.get().equals("Off", true)
        ) {
            setRotation(lockRotation!!)
            if (eventState == EventState.POST)
                lockRotationTimer.update()
        }

        // Face block
        if ((facesBlock || !rotationsValue.get()) && placeModeValue.get()
                .equals(eventState.stateName, true)
        )
            place()

        if (eventState == EventState.PRE) {
            timer.update()
            val update = if (!autoBlockValue.get().equals("Off", ignoreCase = true)) {
                InventoryUtils.findAutoBlockBlock() != -1 || !mc.player.heldItemMainhand.isEmpty && mc.player.heldItemMainhand.item is ItemBlock
            } else {
                !mc.player.heldItemMainhand.isEmpty && mc.player.heldItemMainhand.item is ItemBlock
            }
            if (update&&mc.gameSettings.keyBindJump.isKeyDown) move()
        }

        // Update and search for a new block
        if (eventState == EventState.PRE && strafeMode.get().equals("Off", true))
            update()

        // Reset placeable delay
        if (targetPlace == null && placeDelay.get())
            delayTimer.reset()
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (!(towerModeValue equal "Jump")&&mc.gameSettings.keyBindJump.isKeyDown) event.cancelEvent()
    }

    fun update() {
        val isHeldItemBlock: Boolean =
            !mc.player!!.heldItemMainhand.isEmpty && (mc.player!!.heldItemMainhand.item) is ItemBlock
        if (if (!autoBlockValue.get()
                    .equals("Off", true)
            ) InventoryUtils.findAutoBlockBlock() == -1 && !isHeldItemBlock else !isHeldItemBlock
        )
            return
        findBlock(expandLengthValue.get()!=0&&!(jumpCheckValue.get()&&mc.gameSettings.keyBindJump.isKeyDown)&&!(downCheckValue.get()&&shouldGoDown))
    }

    private fun setRotation(rotation: Rotation) {
        if (silentRotationValue.get()) {
            RotationUtils.setTargetRotation(rotation, 0)
        } else {
            mc.player!!.rotationYaw = rotation.yaw
            mc.player!!.rotationPitch = rotation.pitch
        }
    }

    // Search for new target block
    private fun findBlock(expand: Boolean) {
        val blockPosition: BlockPos =
            if (shouldGoDown) (if (mc.player!!.posY == mc.player!!.posY.toInt() + 0.5) BlockPos(
                mc.player!!.posX,
                mc.player!!.posY - 0.6,
                mc.player!!.posZ
            )
            else BlockPos(mc.player!!.posX, mc.player!!.posY - 0.6, mc.player!!.posZ).down())
            else
                (if (sameY() && launchY <= mc.player!!.posY) BlockPos(
                    mc.player!!.posX,
                    launchY - 1.0,
                    mc.player!!.posZ
                ) else (if (mc.player!!.posY == mc.player!!.posY.toInt() + 0.5) BlockPos(mc.player!!) else BlockPos(
                    mc.player!!.posX,
                    mc.player!!.posY,
                    mc.player!!.posZ
                ).down()))
        if (!expand && (!isReplaceable(blockPosition) || search(blockPosition, !shouldGoDown)))
            return

        if (expand) {
            for (i in 0 until expandLengthValue.get()) {
                if (search(
                        blockPosition.add(
                            when (mc.player!!.horizontalFacing) {
                                EnumFacing.WEST -> -i
                                EnumFacing.EAST -> i
                                else -> 0
                            }, 0,
                            when (mc.player!!.horizontalFacing) {
                                EnumFacing.NORTH -> -i
                                EnumFacing.SOUTH -> i
                                else -> 0
                            }
                        ), false
                    )
                )
                    return
            }
        } else if (searchValue.get()) {
            for (x in -1..1) {
                for (z in -1..1) {
                    if (search(blockPosition.add(x, 0, z), !shouldGoDown)) {
                        return
                    }
                }
            }
        }
    }

    fun place() {
        if (targetPlace == null) {
            if (placeDelay.get())
                delayTimer.reset()
            return
        }

        if (!delayTimer.hasTimePassed(delay) || sameY() && launchY - 1 != targetPlace!!.vec3.y.toInt())
            return

        val currentItem = mc.player!!.inventory.currentItem

        var itemStack = mc.player!!.heldItemMainhand
        if (itemStack.isEmpty || (itemStack.item) !is ItemBlock ||
            ((itemStack.item as ItemBlock).block) is BlockBush || mc.player!!.heldItemMainhand.stackSize <= 0
        ) {

            val blockSlot = InventoryUtils.findAutoBlockBlock()

            if (blockSlot == -1)
                return

            when (autoBlockValue.get().toLowerCase()) {
                "off" -> return
                "pick" -> {
                    mc.player!!.inventory.currentItem = blockSlot - 36
                    mc.playerController.updateController()
                }
                /**
                "spoof" -> {
                    if (blockSlot - 36 != slot) {
                        mc.connection!!.sendPacket(CPacketHeldItemChange(blockSlot - 36))
                        mc.player!!.inventory.currentItem = blockSlot - 36
                    }
                }**/
                "spoof","switch" -> {
                    if (blockSlot - 36 != slot) {
                        mc.connection!!.sendPacket(CPacketHeldItemChange(blockSlot - 36))
                        mc.player!!.inventory.currentItem = blockSlot - 36
                    }
                }
            }
            itemStack = mc.player!!.inventoryContainer.getSlot(blockSlot).stack
        }

        if (mc.playerController.processRightClickBlock(
                mc.player!!,
                mc.world!!,
                targetPlace!!.blockPos,
                targetPlace!!.enumFacing,
                targetPlace!!.vec3,
                EnumHand.MAIN_HAND
            )  == EnumActionResult.SUCCESS
        ) {
            delayTimer.reset()
            delay = if (!placeDelay.get()) 0 else TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

            if (mc.player!!.onGround) {
                val modifier: Float = speedModifierValue.get()
                mc.player!!.motionX = mc.player!!.motionX * modifier
                mc.player!!.motionZ = mc.player!!.motionZ * modifier
            }

            if (swingValue.get()) {
                mc.player!!.swingArm(EnumHand.MAIN_HAND)
            } else {
                mc.connection!!.sendPacket(CPacketAnimation())
            }
        }
        if (autoBlockValue.get().equals("Spoof", true)) {
            mc.player!!.inventory.currentItem = currentItem
        }
        if (autoBlockValue.get().equals("Switch", true)) {
            mc.player!!.inventory.currentItem = currentItem
            if (slot != mc.player!!.inventory.currentItem) {
                mc.connection!!.sendPacket(CPacketHeldItemChange(mc.player!!.inventory.currentItem))
            }
        }
        targetPlace = null
    }

    // DISABLING MODULE
    override fun onDisable() {
        if (mc.player == null) return
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking)
                mc.connection!!.sendPacket(
                    CPacketEntityAction(
                        mc.player!!,
                        CPacketEntityAction.Action.STOP_SNEAKING
                    )
                )
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight))
            mc.gameSettings.keyBindRight.pressed = false
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft))
            mc.gameSettings.keyBindLeft.pressed = false

        lockRotation = null
        facesBlock = false
        mc.timer.timerSpeed = 1f
        shouldGoDown = false

        if (slot != mc.player!!.inventory.currentItem) {
            mc.connection!!.sendPacket(CPacketHeldItemChange(mc.player!!.inventory.currentItem))
        }
    }

    // Entity movement event
    /** @param event */
    @EventTarget
    fun onMove(event: MoveEvent) {
        if (!safeWalkValue.get() || shouldGoDown)
            return
        if (airSafeValue.get() || mc.player!!.onGround)
            event.isSafeWalk = true
    }

    // Scaffold visuals
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (counterDisplayValue.get()) {
            GL11.glPushMatrix()
            val info = "Blocks: §7$blocksAmount"
            val scaledResolution = ScaledResolution(mc)

            RenderUtils.drawBorderedRect(
                scaledResolution.scaledWidth / 2 - 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 5.toFloat(),
                scaledResolution.scaledWidth / 2 + FontManager.font40.getStringWidth(info) + 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 16.toFloat(), 3f, Color.BLACK.rgb, Color.BLACK.rgb
            )

            GlStateManager.resetColor()

            FontManager.font40.drawString(
                info, scaledResolution.scaledWidth / 2.toFloat(),
                scaledResolution.scaledHeight / 2 + 7.toFloat(), Color.WHITE.rgb
            )

            GlStateManager.enableBlend()

            GL11.glPopMatrix()
        }
    }
// SCAFFOLD VISUALS
    /** @param  */
    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!markValue.get()) return
        for (i in 0 until if (expandLengthValue.get()!=0) expandLengthValue.get() + 1 else 2) {
            val blockPos = BlockPos(
                mc.player!!.posX + when (mc.player!!.horizontalFacing) {
                    EnumFacing.WEST -> -i.toDouble()
                    EnumFacing.EAST -> i.toDouble()
                    else -> 0.0
                },
                if (sameY() && launchY <= mc.player!!.posY) launchY - 1.0 else mc.player!!.posY - (if (mc.player!!.posY == mc.player!!.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                mc.player!!.posZ + when (mc.player!!.horizontalFacing) {
                    EnumFacing.NORTH -> -i.toDouble()
                    EnumFacing.SOUTH -> i.toDouble()
                    else -> 0.0
                }
            )
            val placeInfo: PlaceInfo? = PlaceInfo.get(blockPos)
            if (isReplaceable(blockPos) && placeInfo != null) {
                RenderUtils.drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
                break
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checks        visible
     * @return
     */

    private fun search(blockPosition: BlockPos, checks: Boolean): Boolean {
        facesBlock = false
        if (!isReplaceable(blockPosition)) return false

        // Search Ranges
        val xzRV = xzRangeValue.get().toDouble()
        val xzSSV = calcStepSize(xzRV.toFloat())
        val yRV = yRangeValue.get().toDouble()
        val ySSV = calcStepSize(yRV.toFloat())
        val eyesPos = Vec3d(
            mc.player!!.posX,
            mc.player!!.entityBoundingBox.minY + mc.player!!.eyeHeight,
            mc.player!!.posZ
        )
        var placeRotation: PlaceRotation? = null
        for (facingType in EnumFacing.values()) {
            val neighbor = blockPosition.offset(facingType)
            if (!canBeClicked(neighbor)) continue
            val dirVec = Vec3d(facingType.directionVec)
            val auto = searchMode.get().equals("Auto", true)
            val center = searchMode.get().equals("AutoCenter", true)
            var xSearch = if (auto) 0.1 else 0.5 - xzRV / 2
            while (xSearch <= if (auto) 0.9 else 0.5 + xzRV / 2) {
                var ySearch = if (auto) 0.1 else 0.5 - yRV / 2
                while (ySearch <= if (auto) 0.9 else 0.5 + yRV / 2) {
                    var zSearch = if (auto) 0.1 else 0.5 - xzRV / 2
                    while (zSearch <= if (auto) 0.9 else 0.5 + xzRV / 2) {
                        val posVec = Vec3d(blockPosition).addVector(
                            if (center) 0.5 else xSearch,
                            if (center) 0.5 else ySearch,
                            if (center) 0.5 else zSearch
                        )
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec.add(Vec3d(dirVec.x * 0.5, dirVec.y * 0.5, dirVec.z * 0.5))
                        if (checks && (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(
                                posVec.add(dirVec)
                            ) || mc.world!!.rayTraceBlocks(
                                eyesPos, hitVec,
                                false,
                                true,
                                false
                            ) != null)
                        ) {
                            zSearch += if (auto) 0.1 else xzSSV
                            continue
                        }

                        // Face block
                        val diffX = hitVec.x - eyesPos.x
                        val diffY = hitVec.y - eyesPos.y
                        val diffZ = hitVec.z - eyesPos.z
                        val diffXZ = sqrt(diffX * diffX + diffZ * diffZ)
                        if ((facingType == EnumFacing.NORTH || facingType == EnumFacing.EAST || facingType == EnumFacing.SOUTH || facingType == EnumFacing.WEST) && minDistValue.get() > 0) {
                            val diff: Double = abs(if (facingType == EnumFacing.NORTH || facingType == EnumFacing.SOUTH) diffZ else diffX)
                            if (diff < minDistValue.get() || diff > 0.3f) {
                                zSearch += if (auto) 0.1 else xzSSV
                                continue
                            }
                        }
                        val rotation = Rotation(
                            wrapAngleTo180_float(Math.toDegrees(atan2(diffZ, diffX)).toFloat() - 90f),
                            wrapAngleTo180_float(-Math.toDegrees(atan2(diffY, diffXZ)).toFloat())
                        )
                        val rotationVector = RotationUtils.getVectorForRotation(rotation)
                        val vector = eyesPos.addVector(
                            rotationVector.x * distanceSqPosVec,
                            rotationVector.y * distanceSqPosVec,
                            rotationVector.z * distanceSqPosVec
                        )
                        val obj = mc.world!!.rayTraceBlocks(
                            eyesPos, vector,
                            false,
                            false,
                            true
                        )
                        if (obj!!.typeOfHit != RayTraceResult.Type.BLOCK || obj.blockPos != neighbor) {
                            zSearch += if (auto) 0.1 else xzSSV
                            continue
                        }
                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(
                                placeRotation.rotation
                            )
                        ) {
                            placeRotation = PlaceRotation(PlaceInfo(neighbor, facingType.opposite, hitVec), rotation)
                        }

                        zSearch += if (auto) 0.1 else xzSSV
                    }
                    ySearch += if (auto) 0.1 else ySSV
                }
                xSearch += if (auto) 0.1 else xzSSV
            }
        }
        if (placeRotation == null) return false
        if (rotationsValue.get()) {
            if (minTurnSpeedValue.get() < 180) {
                val limitedRotation = RotationUtils.limitAngleChange(
                    RotationUtils.serverRotation,
                    placeRotation.rotation,
                    (Math.random() * (maxTurnSpeedValue.get() - minTurnSpeedValue.get()) + minTurnSpeedValue.get()).toFloat()
                )

                if ((10 * wrapAngleTo180_float(limitedRotation.yaw)).roundToInt() == (10 * wrapAngleTo180_float(
                        placeRotation.rotation.yaw
                    )).roundToInt() &&
                    (10 * wrapAngleTo180_float(limitedRotation.pitch)).roundToInt() == (10 * wrapAngleTo180_float(
                        placeRotation.rotation.pitch
                    )).roundToInt()
                ) {
                    setRotation(placeRotation.rotation)
                    lockRotation = placeRotation.rotation
                    facesBlock = true
                } else {
                    setRotation(limitedRotation)
                    lockRotation = limitedRotation
                    facesBlock = false
                }
            } else {
                setRotation(placeRotation.rotation)
                lockRotation = placeRotation.rotation
                facesBlock = true
            }
            lockRotationTimer.reset()
        }
        targetPlace = placeRotation.placeInfo
        return true
    }

    private fun calcStepSize(range: Float): Double {
        var accuracy: Double = searchAccuracyValue.get().toDouble()
        accuracy += accuracy % 2 // If it is set to uneven it changes it to even. Fixes a bug
        return if (range / accuracy < 0.01) 0.01 else (range / accuracy)
    }

    // RETURN HOTBAR AMOUNT
    private val blocksAmount: Int
        get() {
            var amount = 0
            for (i in 36..44) {
                val itemStack: ItemStack? = mc.player!!.inventoryContainer.getSlot(i).stack
                if (itemStack != null && (itemStack.item) is ItemBlock) {
                    val block: Block = (itemStack.item as ItemBlock).block
                    val heldItem: ItemStack? = mc.player!!.heldItemMainhand
                    if (heldItem != null && heldItem == itemStack || !InventoryUtils.BLOCK_BLACKLIST.contains(block) && (block) !is BlockBush) {
                        amount += itemStack.stackSize
                    }
                }
            }
            return amount
        }
    override val tag: String
        get() = if (!(towerModeValue equal "Jump")&&mc.gameSettings.keyBindJump.isKeyDown) "Tower" else if (mc.gameSettings.keyBindJump.isKeyDown) "JumpUp" else if (shouldGoDown) "Down" else if (expandLengthValue.get()!=0) "Expand" else "Normal"
}