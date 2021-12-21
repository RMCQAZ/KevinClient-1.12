package kevin.module.modules.movement

import kevin.event.EventTarget
import kevin.event.JumpEvent
import kevin.event.StrafeEvent
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.Module
import kevin.module.ModuleCategory
import org.lwjgl.input.Keyboard
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Strafe : Module("Strafe","Allows you to freely move in mid air.", Keyboard.KEY_NONE, ModuleCategory.MOVEMENT){

    private var strengthValue= FloatValue("Strength", 0.5F, 0F, 1F)
    private var noMoveStopValue = BooleanValue("NoMoveStop", false)
    private var onGroundStrafeValue = BooleanValue("OnGroundStrafe", false)
    var allDirectionsJumpValue = BooleanValue("AllDirectionsJump", false)

    private var wasDown: Boolean = false
    private var jump: Boolean = false

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (jump) {
            event.cancelEvent()
        }
    }

    override fun onEnable() {
        wasDown = false
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mc.player!!.onGround && mc.gameSettings.keyBindJump.isKeyDown && allDirectionsJumpValue.get() && (mc.player!!.movementInput.moveForward != 0F || mc.player!!.movementInput.moveStrafe != 0F) && !(mc.player!!.isInWater || mc.player!!.isInLava || mc.player!!.isOnLadder || mc.player!!.isInWeb)) {
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                mc.gameSettings.keyBindJump.pressed = false
                wasDown = true
            }
            val yaw = mc.player!!.rotationYaw
            mc.player!!.rotationYaw = getMoveYaw()
            mc.player!!.jump()
            mc.player!!.rotationYaw = yaw
            jump = true
            if (wasDown) {
                mc.gameSettings.keyBindJump.pressed = true
                wasDown = false
            }
        } else {
            jump = false
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        val shotSpeed = sqrt((mc.player!!.motionX * mc.player!!.motionX) + (mc.player!!.motionZ * mc.player!!.motionZ))
        val speed = (shotSpeed * strengthValue.get())
        val motionX = (mc.player!!.motionX * (1 - strengthValue.get()))
        val motionZ = (mc.player!!.motionZ * (1 - strengthValue.get()))
        if (!(mc.player!!.movementInput.moveForward != 0F || mc.player!!.movementInput.moveStrafe != 0F)) {
            if (noMoveStopValue.get()) {
                mc.player!!.motionX = 0.0
                mc.player!!.motionZ = 0.0
            }
            return
        }
        if (!mc.player!!.onGround || onGroundStrafeValue.get()) {
            val yaw = getMoveYaw()
            mc.player!!.motionX = (((-sin(Math.toRadians(yaw.toDouble())) * speed) + motionX))
            mc.player!!.motionZ = (((cos(Math.toRadians(yaw.toDouble())) * speed) + motionZ))
        }
    }


    fun getMoveYaw(): Float {
        var moveYaw = mc.player!!.rotationYaw
        if (mc.player!!.moveForward != 0F && mc.player!!.moveStrafing == 0F) {
            moveYaw += if(mc.player!!.moveForward > 0) 0 else 180
        } else if (mc.player!!.moveForward != 0F && mc.player!!.moveStrafing != 0F) {
            if (mc.player!!.moveForward > 0) {
                moveYaw += if (mc.player!!.moveStrafing > 0) -45 else 45
            } else {
                moveYaw -= if (mc.player!!.moveStrafing > 0) -45 else 45
            }
            moveYaw += if(mc.player!!.moveForward > 0) 0 else 180
        } else if (mc.player!!.moveStrafing != 0F && mc.player!!.moveForward == 0F) {
            moveYaw += if(mc.player!!.moveStrafing > 0) -90 else 90
        }
        return moveYaw
    }
}