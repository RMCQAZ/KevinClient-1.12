package kevin.module.modules.movement.speeds.verus

import kevin.event.MoveEvent
import kevin.module.modules.movement.speeds.SpeedMode
import kevin.utils.MovementUtils

object VerusYPort : SpeedMode("VerusYPort") {
    override fun onMove(event: MoveEvent) {
        if (MovementUtils.isMoving&&
            !mc.player.isInWeb&&
            !mc.player.isInLava&&
            !mc.player.isInWater&&
            !mc.player.isOnLadder&&
            mc.player.ridingEntity == null&&
            !mc.gameSettings.keyBindJump.isKeyDown) {
            mc.gameSettings.keyBindJump.pressed = false
            if (mc.player.onGround) {
                mc.player.jump()
                mc.player.motionY = 0.0
                MovementUtils.strafe(0.61F)
                event.y = 0.41999998688698
            }
            MovementUtils.strafe()
        }
    }
}