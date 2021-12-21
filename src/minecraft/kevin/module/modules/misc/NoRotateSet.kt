package kevin.module.modules.misc

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.module.BooleanValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.RotationUtils
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook

class NoRotateSet : Module("NoRotateSet", "Prevents the server from rotating your head.", category = ModuleCategory.MISC) {
    private val confirmValue = BooleanValue("Confirm", true)
    private val illegalRotationValue = BooleanValue("ConfirmIllegalRotation", false)
    private val noZeroValue = BooleanValue("NoZero", false)

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val thePlayer = mc.player ?: return

        if ((event.packet)is SPacketPlayerPosLook) {
            val packet = event.packet

            if (noZeroValue.get() && packet.yaw == 0F && packet.pitch == 0F)
                return

            if (illegalRotationValue.get() || packet.pitch <= 90 && packet.pitch >= -90 &&
                RotationUtils.serverRotation != null && packet.yaw != RotationUtils.serverRotation.yaw &&
                packet.pitch != RotationUtils.serverRotation.pitch) {

                if (confirmValue.get())
                    mc.connection!!.sendPacket(CPacketPlayer.Rotation(packet.yaw, packet.pitch, thePlayer.onGround))
            }

            packet.yaw = thePlayer.rotationYaw
            packet.pitch = thePlayer.rotationPitch
        }
    }

}