package kevin.module.modules.movement.speeds.aac

import kevin.event.UpdateEvent
import kevin.module.modules.movement.speeds.SpeedMode
import kevin.utils.MovementUtils

object AAC5Long : SpeedMode("AAC5Long") {
    override fun onUpdate(event: UpdateEvent){
        if (!MovementUtils.isMoving) return
        if (mc.player.isInWater || mc.player.isInLava || mc.player.isOnLadder || mc.player.isInWeb) return

        if (mc.player.onGround) {
            mc.gameSettings.keyBindJump.pressed = false
            mc.player.jump()
        }
        if (!mc.player.onGround && mc.player.fallDistance <= 0.1) {
            mc.player.speedInAir = 0.02F
            mc.timer.timerSpeed = 1.5F
        }
        if (mc.player.fallDistance > 0.1 && mc.player.fallDistance < 1.3) {
            mc.timer.timerSpeed = 0.7F
        }
        if (mc.player.fallDistance >= 1.3) {
            mc.timer.timerSpeed = 1F
            mc.player.speedInAir = 0.02F
        }
    }
}