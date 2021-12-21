package kevin.module.modules.movement.flys.vanilla

import kevin.event.UpdateEvent
import kevin.module.modules.movement.flys.FlyMode

object Creative : FlyMode("Creative") {
    override fun onEnable() {
        mc.player.capabilities.allowFlying = true
    }
    override fun onUpdate(event: UpdateEvent) {
        if (!mc.player.capabilities.allowFlying) mc.player.capabilities.allowFlying = true
    }
    override fun onDisable() {
        mc.player.capabilities.allowFlying = mc.playerController.isInCreativeMode || mc.playerController.isSpectatorMode
    }
}