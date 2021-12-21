package kevin.module.modules.movement.speeds.other

import kevin.module.modules.movement.speeds.SpeedMode
import kevin.utils.MovementUtils
import kotlin.math.cos
import kotlin.math.sin

object YPort : SpeedMode("YPort") {
    private var jumps = 0
    override fun onPreMotion() {
        if (mc.player!!.isOnLadder
            || mc.player!!.isInWater
            || mc.player!!.isInLava
            || mc.player!!.isInWeb
            || !MovementUtils.isMoving
            || mc.gameSettings.keyBindJump.isKeyDown) return
        if (jumps >= 4 && mc.player!!.onGround) jumps = 0
        if (mc.player!!.onGround) {
            mc.player!!.motionY = if (jumps <= 1) 0.42 else 0.4
            val f = mc.player!!.rotationYaw * 0.017453292f
            mc.player!!.motionX -= sin(f) * 0.2f
            mc.player!!.motionZ += cos(f) * 0.2f
            jumps++
        } else if (jumps <= 1) mc.player!!.motionY = -5.0
        MovementUtils.strafe()
    }
    override fun onDisable() {
        jumps = 0
    }
}