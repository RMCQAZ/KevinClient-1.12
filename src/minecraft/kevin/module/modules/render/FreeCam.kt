package kevin.module.modules.render

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.MovementUtils
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer

class FreeCam : Module("FreeCam", "Allows you to move out of your body.", category = ModuleCategory.RENDER) {
    private val speedValue = FloatValue("Speed", 0.8f, 0.1f, 2f)
    private val flyValue = BooleanValue("Fly", true)
    private val noClipValue = BooleanValue("NoClip", true)

    private var fakePlayer: EntityOtherPlayerMP? = null

    private var oldX = 0.0
    private var oldY = 0.0
    private var oldZ = 0.0

    override fun onEnable() {
        val thePlayer = mc.player ?: return

        oldX = thePlayer.posX
        oldY = thePlayer.posY
        oldZ = thePlayer.posZ

        val playerMP = EntityOtherPlayerMP(mc.world!!, thePlayer.gameProfile)


        playerMP.rotationYawHead = thePlayer.rotationYawHead;
        playerMP.renderYawOffset = thePlayer.renderYawOffset;
        playerMP.rotationYawHead = thePlayer.rotationYawHead
        playerMP.copyLocationAndAnglesFrom(thePlayer)

        mc.world!!.addEntityToWorld(-1000, playerMP)

        if (noClipValue.get())
            thePlayer.noClip = true

        fakePlayer = playerMP
    }

    override fun onDisable() {
        val thePlayer = mc.player

        if (thePlayer == null || fakePlayer == null)
            return

        thePlayer.setPositionAndRotation(oldX, oldY, oldZ, thePlayer.rotationYaw, thePlayer.rotationPitch)

        mc.world!!.removeEntityFromWorld(fakePlayer!!.entityId)
        fakePlayer = null

        thePlayer.motionX = 0.0
        thePlayer.motionY = 0.0
        thePlayer.motionZ = 0.0
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        val thePlayer = mc.player!!

        if (noClipValue.get())
            thePlayer.noClip = true

        thePlayer.fallDistance = 0.0f

        if (flyValue.get()) {
            val value = speedValue.get()

            thePlayer.motionY = 0.0
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0

            if (mc.gameSettings.keyBindJump.isKeyDown)
                thePlayer.motionY += value

            if (mc.gameSettings.keyBindSneak.isKeyDown)
                thePlayer.motionY -= value

            MovementUtils.strafe(value)
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if ((packet) is CPacketPlayer || (packet) is CPacketEntityAction)
            event.cancelEvent()
    }
}