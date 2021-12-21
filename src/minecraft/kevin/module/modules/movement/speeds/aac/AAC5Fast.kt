package kevin.module.modules.movement.speeds.aac

import kevin.KevinClient
import kevin.event.UpdateEvent
import kevin.module.modules.movement.Strafe
import kevin.module.modules.movement.speeds.SpeedMode
import kevin.utils.MovementUtils

object AAC5Fast : SpeedMode("AAC5Fast") {
    override fun onUpdate(event: UpdateEvent){
        if (!MovementUtils.isMoving) return
        if (mc.player.isInWater || mc.player.isInLava || mc.player.isOnLadder || mc.player.isInWeb) return
        if (mc.player.onGround) {
            val strafe = KevinClient.moduleManager.getModule("Strafe") as Strafe
            if (strafe.state && strafe.allDirectionsJumpValue.get()) {
                val yaw = mc.player.rotationYaw
                mc.player.rotationYaw = strafe.getMoveYaw()
                mc.player.jump()
                mc.player.rotationYaw = yaw
            } else {
                mc.player.jump()
            }
            mc.player.speedInAir = 0.0201F
            mc.timer.timerSpeed = 0.94F
        }
        if (mc.player.fallDistance > 0.7 && mc.player.fallDistance < 1.3) {
            mc.player.speedInAir = 0.02F
            mc.timer.timerSpeed = 1.8F
        }
    }
}