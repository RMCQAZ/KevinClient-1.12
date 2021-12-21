package kevin.module.modules.movement.flys.vanilla

import kevin.event.UpdateEvent
import kevin.module.modules.movement.flys.FlyMode
import kevin.utils.MovementUtils
import net.minecraft.network.play.client.CPacketKeepAlive

object Vanilla : FlyMode("Vanilla") {
    override fun onUpdate(event: UpdateEvent) {
        if (fly.keepAlive.get()) mc.connection!!.sendPacket(CPacketKeepAlive())
        mc.player.motionY = 0.0
        mc.player.motionX = 0.0
        mc.player.motionZ = 0.0
        mc.player.capabilities.isFlying = false
        if (mc.gameSettings.keyBindJump.isKeyDown) mc.player.motionY += fly.speed.get()
        if (mc.gameSettings.keyBindSneak.isKeyDown) mc.player.motionY -= fly.speed.get()
        MovementUtils.strafe(fly.speed.get())
    }
}