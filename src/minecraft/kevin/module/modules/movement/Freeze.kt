package kevin.module.modules.movement

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.ListValue
import kevin.module.Module
import kevin.module.ModuleCategory
import net.minecraft.network.play.client.CPacketPlayer

class Freeze : Module("Freeze", "Allows you to stay stuck in mid air.", category = ModuleCategory.MOVEMENT) {
    private val mode = ListValue("Mode", arrayOf("SetDead","NoMove"),"SetDead")
    private val resetMotionValue = BooleanValue("ResetMotion",false)
    private val lockRotation = BooleanValue("LockRotation",true)

    private var motionX = .0
    private var motionY = .0
    private var motionZ = .0
    private var rotationYaw = .0F
    private var rotationPitch = .0F

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player!!

        when(mode.get()){
            "SetDead" -> {
                thePlayer.isDead = true
                thePlayer.rotationYaw = thePlayer.cameraYaw
                thePlayer.rotationPitch = thePlayer.cameraPitch
            }
            "NoMove" -> {
                mc.player.motionX = .0
                mc.player.motionY = .0
                mc.player.motionZ = .0
                mc.player.speedInAir = .0F
            }
        }
    }
    @EventTarget fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is CPacketPlayer&&(packet is CPacketPlayer.Rotation || packet is CPacketPlayer.PositionRotation)) {
            if (!lockRotation.get()) return
            when(mode.get()){
                "NoMove" -> {
                    packet.yaw = rotationYaw
                    packet.pitch = rotationPitch
                }
            }
        }
    }

    override fun onDisable() {
        when(mode.get()){
            "SetDead" -> mc.player?.isDead = false
            "NoMove" -> mc.player.speedInAir = .02F
        }
        if (resetMotionValue.get()) {
            mc.player.motionX = .0
            mc.player.motionY = .0
            mc.player.motionZ = .0
        } else {
            mc.player.motionX = motionX
            mc.player.motionY = motionY
            mc.player.motionZ = motionZ
        }
    }
    override fun onEnable() {
        val thePlayer = mc.player!!
        motionX = thePlayer.motionX
        motionY = thePlayer.motionY
        motionZ = thePlayer.motionZ
        rotationYaw = thePlayer.rotationYaw
        rotationPitch = thePlayer.rotationPitch
    }
}