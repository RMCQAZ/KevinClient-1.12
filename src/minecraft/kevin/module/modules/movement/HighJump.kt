package kevin.module.modules.movement

import kevin.event.EventTarget
import kevin.event.JumpEvent
import kevin.event.MoveEvent
import kevin.event.UpdateEvent
import kevin.module.*
import kevin.utils.BlockUtils.getBlock
import kevin.utils.MovementUtils
import kevin.utils.timers.MSTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockPane
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos

class HighJump : Module("HighJump", "Allows you to jump higher.", category = ModuleCategory.MOVEMENT) {
    private val heightValue = FloatValue("Height", 2f, 1.1f, 5f)
    private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Damage", "AACv3", "DAC", "Mineplex", "Timer", "Matrix","MatrixWater"), "Vanilla")
    private val glassValue = BooleanValue("OnlyGlassPane", false)
    private val timerValue = FloatValue("Timer",0.1f,0.01f,1f)
    private val waitTimeValue = IntegerValue("WaitTime",1,0,5)
    private val flyValue = BooleanValue("Fly",false)

    private var jumpState = 1
    private var fly = false
    private var flyState = 0
    private var timer = -1
    private var timerlock = false

    private var matrixStatus=0
    private var matrixWasTimer=false
    private val matrixTimer = MSTimer()

    override fun onDisable() {
        when(modeValue.get()){
            "Timer" -> {
                mc.timer.timerSpeed = 1F
                jumpState = 1
                fly = false
                flyState = 0
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        val thePlayer = mc.player!!

        if (modeValue equal "Timer"){
            if(mc.player!!.onGround){
                if (fly) {
                    state = false
                    return
                }
                mc.timer.timerSpeed = timerValue.get()
                mc.player!!.jump()
                jumpState = 2
            } else {
                if(jumpState == 2) {
                    mc.timer.timerSpeed = 1F
                    if (!flyValue.get()) state = false
                    if (!timerlock) {
                        timerlock = true
                        timer = 0
                    }
                    if(timer >= waitTimeValue.get())
                        timerlock = false
                    fly = true
                    timer = -1
                }

                if (jumpState != 2){
                    jumpState = 2
                }
            }
            if (timer != -1)
                timer += 1

            if(fly){
                flyState += 1
                if (flyState >= 6){
                    mc.player!!.motionY = .015
                    flyState = 0
                }
            }
            return
        }

        if (glassValue.get() && (getBlock(BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))!is BlockPane)
            return

        when (modeValue.get().toLowerCase()) {
            "damage" -> if (thePlayer.hurtTime > 0 && thePlayer.onGround) thePlayer.motionY += 0.42f * heightValue.get()
            "aacv3" -> if (!thePlayer.onGround) thePlayer.motionY += 0.059
            "dac" -> if (!thePlayer.onGround) thePlayer.motionY += 0.049999
            "mineplex" -> if (!thePlayer.onGround) MovementUtils.strafe(0.35f)
            "matrixwater" -> {
                if (mc.player.isInWater) {
                    if (mc.world.getBlockState(BlockPos(mc.player.posX, mc.player.posY + 1, mc.player.posZ)).block == Block.getBlockById(9)) {
                        mc.player.motionY = 0.18
                    } else if (mc.world.getBlockState(BlockPos(mc.player.posX, mc.player.posY, mc.player.posZ)).block == Block.getBlockById(9)) {
                        mc.player.motionY = heightValue.get().toDouble()
                        mc.player.onGround = true
                    }
                }
            }
            "matrix" -> {
                if (matrixWasTimer) {
                    mc.timer.timerSpeed = 1.00f
                    matrixWasTimer = false
                }
                if ((mc.world.getCollisionBoxes(mc.player, mc.player.entityBoundingBox.offset(0.0, mc.player.motionY, 0.0).expand(0.0, 0.0, 0.0)).isNotEmpty()
                            || mc.world.getCollisionBoxes(mc.player, mc.player.entityBoundingBox.offset(0.0, -4.0, 0.0).expand(0.0, 0.0, 0.0)).isNotEmpty())
                    && mc.player.fallDistance > 10) {
                    if (!mc.player.onGround) {
                        mc.timer.timerSpeed = 0.1f
                        matrixWasTimer = true
                    }
                }
                if (matrixTimer.hasTimePassed(1000) && matrixStatus==1) {
                    mc.timer.timerSpeed = 1.0f
                    mc.player.motionX = 0.0
                    mc.player.motionZ = 0.0
                    matrixStatus=0
                    return
                }
                if (matrixStatus==1 && mc.player.hurtTime > 0) {
                    mc.timer.timerSpeed = 1.0f
                    mc.player.motionY = 3.0
                    mc.player.motionX = 0.0
                    mc.player.motionZ = 0.0
                    mc.player.jumpMovementFactor = 0.00f
                    matrixStatus=0
                    return
                }
                if (matrixStatus==2) {
                    mc.player.connection.sendPacket(CPacketAnimation())
                    mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, false))
                    repeat(8) {
                        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.3990, mc.player.posZ, false))
                        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, false))
                    }
                    mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, true))
                    mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY, mc.player.posZ, true))
                    mc.timer.timerSpeed = 0.6f
                    matrixStatus=1
                    matrixTimer.reset()
                    mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, BlockPos(mc.player.posX, mc.player.posY - 1, mc.player.posZ), EnumFacing.UP))
                    mc.player.connection.sendPacket(CPacketAnimation())
                    return
                }
                if (mc.player.isCollidedHorizontally && matrixStatus==0 && mc.player.onGround) {
                    mc.player.connection.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK, BlockPos(mc.player.posX, mc.player.posY - 1, mc.player.posZ), EnumFacing.UP))
                    mc.player.connection.sendPacket(CPacketAnimation())
                    matrixStatus=2
                    mc.timer.timerSpeed = 0.05f
                }
                if (mc.player.isCollidedHorizontally && mc.player.onGround) {
                    mc.player.motionX = 0.0
                    mc.player.motionZ = 0.0
                    mc.player.onGround = false
                }
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent?) {
        val thePlayer = mc.player ?: return

        if (glassValue.get() && (getBlock(BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))!is BlockPane)
            return
        if (!thePlayer.onGround) {
            if ("mineplex" == modeValue.get().toLowerCase()) {
                thePlayer.motionY += if (thePlayer.fallDistance == 0.0f) 0.0499 else 0.05
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val thePlayer = mc.player ?: return
        if (modeValue equal "Timer"){
            event.motion = heightValue.get()
            return
        }

        if (glassValue.get() && (getBlock(BlockPos(thePlayer.posX, thePlayer.posY, thePlayer.posZ)))!is BlockPane)
            return
        when (modeValue.get().toLowerCase()) {
            "vanilla" -> event.motion = event.motion * heightValue.get()
            "mineplex" -> event.motion = 0.47f
        }
    }

    override val tag: String
        get() = modeValue.get()
}