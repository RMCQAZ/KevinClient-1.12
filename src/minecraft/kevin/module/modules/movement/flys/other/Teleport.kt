package kevin.module.modules.movement.flys.other

import kevin.event.MotionEvent
import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.modules.movement.flys.FlyMode
import net.minecraft.network.play.client.CPacketPlayer
import kotlin.math.cos
import kotlin.math.sin

object Teleport : FlyMode("Teleport") {
    private val teleportLongValue = FloatValue("TeleportLong",10F,0.1F,20F)
    private val teleportTimer = FloatValue("TeleportTimer",0.1F,0.1F,1F)
    private val teleportHighPacket = BooleanValue("TeleportHPacket",true)
    private val teleportHigh = FloatValue("TeleportHigh",-1F,-4F,4F)
    private val teleportYMotion = FloatValue("TeleportYMotion",-0.05F,-1F,1F)
    private val teleportMotion = FloatValue("TeleportMotion",2F,0F,5F)
    private val teleportResetMotion = BooleanValue("TeleportResetMotion",true)
    private val teleportSetPos = BooleanValue("TeleportSetPos",false)

    override fun onMotion(event: MotionEvent) {
        mc.player!!.jumpMovementFactor = 0F
        mc.player!!.motionY = teleportYMotion.get().toDouble()
        mc.timer.timerSpeed = teleportTimer.get()
        if (mc.player!!.ticksExisted % 2 == 0){
            val playerYaw = mc.player!!.rotationYaw * Math.PI / 180
            mc.connection!!.sendPacket(
                CPacketPlayer.Position(
                    mc.player!!.posX + teleportLongValue.get() * -sin(playerYaw),
                    mc.player!!.posY,
                    mc.player!!.posZ + teleportLongValue.get() * cos(playerYaw),
                    false
                )
            )
            if(teleportHighPacket.get()) {
                mc.connection!!.sendPacket(
                    CPacketPlayer.Position(
                        mc.player!!.posX,
                        mc.player!!.posY + teleportHigh.get(),
                        mc.player!!.posZ,
                        false
                    )
                )
            }
            mc.player!!.motionX = teleportMotion.get() * -sin(playerYaw)
            mc.player!!.motionZ = teleportMotion.get() * cos(playerYaw)
            if (teleportSetPos.get())
                mc.player.setPosition(
                    mc.player!!.posX + teleportLongValue.get() * -sin(playerYaw),
                    mc.player!!.posY,
                    mc.player!!.posZ + teleportLongValue.get() * cos(playerYaw)
                )
        }else{
            if (teleportResetMotion.get()) mc.player!!.motionY = .0
            mc.player!!.motionX = .0
            mc.player!!.motionZ = .0
        }
    }
    override fun onDisable() {
        mc.timer.timerSpeed = 1F
    }
}