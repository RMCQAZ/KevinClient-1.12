package kevin.module.modules.movement.flys.aac

import kevin.KevinClient
import kevin.event.PacketEvent
import kevin.event.Render3DEvent
import kevin.event.UpdateEvent
import kevin.event.WorldEvent
import kevin.hud.element.elements.Notification
import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.IntegerValue
import kevin.module.ListValue
import kevin.module.modules.movement.flys.FlyMode
import kevin.utils.ColorUtils
import kevin.utils.MovementUtils
import kevin.utils.PacketUtils
import kevin.utils.RenderUtils
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

object AAC5 : FlyMode("AAC5") {
    private val aac5PacketMode = ListValue("AAC5PacketMode", arrayOf("Old", "Rise"), "Old")
    private val aac5UseC04 = BooleanValue("AAC5UseC04", false)
    private val aac5Purse = IntegerValue("AAC5.2.0Purse", 7, 3, 20)

    private val renderPath = BooleanValue("RenderPath",true)
    private val glLineWidthValue = FloatValue("RenderPathLineWidth",2F,0.5F,4F)
    private val colorMode = ListValue("ColorMode", arrayOf("Custom","Rainbow"),"Custom")
    private val colorR = IntegerValue("R",255,0,255)
    private val colorG = IntegerValue("G",255,0,255)
    private val colorB = IntegerValue("B",255,0,255)

    private var aac5FlyClip = false
    private var aac5FlyStart = false
    private var aac5nextFlag = false

    private val aac5C03List = CopyOnWriteArrayList<CPacketPlayer>()

    override fun onEnable() {
        if (mc.isSingleplayer) {
            KevinClient.hud.addNotification(
                Notification("Use AAC5 Flys will crash single player"),"Fly")
            fly.state = false
            return
        }

        fly.flyTimer.reset()
        aac5FlyClip=false
        aac5FlyStart=false
        aac5nextFlag=false
        mc.player.setPosition(mc.player.posX, mc.player.posY + 0.42, mc.player.posZ)
    }
    override fun onDisable() {
        if (mc.isSingleplayer) return
        sendAAC5Packets()
        mc.player.noClip = false
        mc.timer.timerSpeed = 1F
    }

    override fun onRender3D(event: Render3DEvent) {
        if (!renderPath.get()||aac5C03List.isEmpty()) return
        val color = if (colorMode.get() == "Custom") Color(colorR.get(),colorG.get(),colorB.get()) else ColorUtils.rainbow()

        val renderPosX = mc.renderManager.viewerPosX
        val renderPosY = mc.renderManager.viewerPosY
        val renderPosZ = mc.renderManager.viewerPosZ

        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDepthMask(false)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)

        aac5C03List.forEach {
            val x = it.x - renderPosX
            val y = it.y - renderPosY
            val z = it.z - renderPosZ
            val width = 0.3
            val height = mc.player!!.eyeHeight.toDouble()
            GL11.glLoadIdentity()
            mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 2)
            RenderUtils.glColor(color)
            GL11.glLineWidth(glLineWidthValue.get())
            GL11.glBegin(GL11.GL_LINE_STRIP)
            GL11.glVertex3d(x - width, y, z - width)
            GL11.glVertex3d(x - width, y, z - width)
            GL11.glVertex3d(x - width, y + height, z - width)
            GL11.glVertex3d(x + width, y + height, z - width)
            GL11.glVertex3d(x + width, y, z - width)
            GL11.glVertex3d(x - width, y, z - width)
            GL11.glVertex3d(x - width, y, z + width)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_LINE_STRIP)
            GL11.glVertex3d(x + width, y, z + width)
            GL11.glVertex3d(x + width, y + height, z + width)
            GL11.glVertex3d(x - width, y + height, z + width)
            GL11.glVertex3d(x - width, y, z + width)
            GL11.glVertex3d(x + width, y, z + width)
            GL11.glVertex3d(x + width, y, z - width)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_LINE_STRIP)
            GL11.glVertex3d(x + width, y + height, z + width)
            GL11.glVertex3d(x + width, y + height, z - width)
            GL11.glEnd()
            GL11.glBegin(GL11.GL_LINE_STRIP)
            GL11.glVertex3d(x - width, y + height, z + width)
            GL11.glVertex3d(x - width, y + height, z - width)
            GL11.glEnd()
        }

        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
        GL11.glColor4f(1F, 1F, 1F, 1F)
    }
    override fun onWorld(event: WorldEvent) {
        fly.state = false
    }
    override fun onUpdate(event: UpdateEvent) {
        mc.player.noClip=!MovementUtils.isMoving
        if(!fly.flyTimer.hasTimePassed(1000) || !aac5FlyStart) {
            mc.player.motionY = 0.0
            mc.player.motionX = 0.0
            mc.player.motionZ = 0.0
            mc.player.jumpMovementFactor = 0.00f
            mc.timer.timerSpeed = 0.32F
            return
        }else {
            if(!aac5FlyClip) {
                mc.timer.timerSpeed = 0.19F
            }else{
                aac5FlyClip=false
                mc.timer.timerSpeed = 1.2F
            }
        }
        mc.player.motionY = 0.0
        mc.player.motionX = 0.0
        mc.player.motionZ = 0.0
        mc.player.capabilities.isFlying = false
        if (mc.gameSettings.keyBindJump.isKeyDown) mc.player.motionY += fly.speed.get()
        if (mc.gameSettings.keyBindSneak.isKeyDown) mc.player.motionY -= fly.speed.get()
        MovementUtils.strafe(fly.speed.get())
    }
    override fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is SPacketPlayerPosLook){
            aac5FlyStart=true
            if(fly.flyTimer.hasTimePassed(2000)) {
                aac5FlyClip=true
                mc.timer.timerSpeed = 1.3F
            }
            aac5nextFlag=true
        }
        if (packet is CPacketPlayer){
            event.cancelEvent()
            val f = mc.player.width / 2.0
            // need to no collide else will flag
            if(aac5nextFlag || !mc.world.checkBlockCollision(AxisAlignedBB(packet.x - f, packet.y, packet.z - f, packet.x + f, packet.y + mc.player.height, packet.z + f))){
                aac5C03List.add(packet)
                aac5nextFlag=false
                event.cancelEvent()
                if(fly.flyTimer.hasTimePassed(1000) && aac5C03List.size > aac5Purse.get()) {
                    sendAAC5Packets()
                }
            }
        }
    }
    private fun sendAAC5Packets() {
        var yaw = mc.player.rotationYaw
        var pitch = mc.player.rotationPitch
        if (aac5PacketMode.get().equals("Old",true)) {
            for (packet in aac5C03List) {
                if (packet.moving) {
                    PacketUtils.sendPacketNoEvent(packet)
                    if (packet.rotating) {
                        yaw = packet.yaw
                        pitch = packet.pitch
                    }
                    if (aac5UseC04.get()) {
                        PacketUtils.sendPacketNoEvent(CPacketPlayer.Position(packet.x, 1e+159, packet.z, true))
                        PacketUtils.sendPacketNoEvent(CPacketPlayer.Position(packet.x, packet.y, packet.z, true))
                    } else {
                        PacketUtils.sendPacketNoEvent(
                            CPacketPlayer.PositionRotation(
                                packet.x,
                                1e+159,
                                packet.z,
                                yaw,
                                pitch,
                                true
                            )
                        )
                        PacketUtils.sendPacketNoEvent(
                            CPacketPlayer.PositionRotation(
                                packet.x,
                                packet.y,
                                packet.z,
                                yaw,
                                pitch,
                                true
                            )
                        )
                    }
                }
            }
        } else {
            for (packet in aac5C03List) {
                if (packet.moving) {
                    PacketUtils.sendPacketNoEvent(packet)
                    if (packet.rotating) {
                        yaw = packet.yaw
                        pitch = packet.pitch
                    }
                    if (aac5UseC04.get()) {
                        PacketUtils.sendPacketNoEvent(
                            CPacketPlayer.Position(
                                packet.x,
                                -1e+159,
                                packet.z + 10,
                                true
                            )
                        )
                        PacketUtils.sendPacketNoEvent(CPacketPlayer.Position(packet.x, packet.y, packet.z, true))
                    } else {
                        PacketUtils.sendPacketNoEvent(
                            CPacketPlayer.PositionRotation(
                                packet.x,
                                -1e+159,
                                packet.z + 10,
                                yaw,
                                pitch,
                                true
                            )
                        )
                        PacketUtils.sendPacketNoEvent(
                            CPacketPlayer.PositionRotation(
                                packet.x,
                                packet.y,
                                packet.z,
                                yaw,
                                pitch,
                                true
                            )
                        )
                    }
                }
            }
        }
        aac5C03List.clear()
    }
}