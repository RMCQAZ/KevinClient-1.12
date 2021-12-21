package kevin.module.modules.movement.speeds.other

import kevin.module.modules.movement.speeds.SpeedMode
import kevin.utils.MovementUtils

object AutoJump : SpeedMode("AutoJump") {
    override fun onPreMotion() {
        if (mc.player.onGround
            && mc.player.jumpTicks == 0
            && MovementUtils.isMoving
            && !mc.player.isInLava
            && !mc.player.isInWater
            && !mc.player.isInWeb
            && !mc.player.isOnLadder
            && !mc.gameSettings.keyBindJump.isKeyDown) {
            mc.player.jump()
        }
    }
}