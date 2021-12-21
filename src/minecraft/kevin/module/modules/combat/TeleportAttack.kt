package kevin.module.modules.combat

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.Render3DEvent
import kevin.event.UpdateEvent
import kevin.module.*
import kevin.utils.*
import kevin.utils.timers.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumHand
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color

class TeleportAttack : Module("TeleportAttack","Attack the target over a long distance!", category = ModuleCategory.COMBAT) {
    private val packetMode = ListValue("PacketMode", arrayOf("C04PacketPlayerPosition","C06PacketPlayerPosLook"),"C04PacketPlayerPosition")
    private val colorModeV = ListValue("ColorMode", arrayOf("Custom","Rainbow"),"Custom")
    private val colorR = IntegerValue("R",255,0,255)
    private val colorG = IntegerValue("G",255,0,255)
    private val colorB = IntegerValue("B",255,0,255)
    private val modeV = ListValue("Mode", arrayOf("Aura","ClickAura"),"Aura")
    private val targetsV = IntegerValue("Targets",3,1,10)
    private val cpsV = IntegerValue("CPS",1,1,20)
    private val distV = IntegerValue("Distance",50,20,150)
    private val moveDistanceV = FloatValue("MoveDistance",5F,2F,15F)
    private val swingV = BooleanValue("Swing",true)
    private val pathV = BooleanValue("DrawPath",true)
    private val noRegen = BooleanValue("NoRegen",true)
    private val aliveTicks = IntegerValue("AliveTicks",20,10,50)
    private val glLineWidthValue = FloatValue("LineWidth",2F,1F,4F)
    private val aliveTimer = MSTimer()
    private val timer = MSTimer()
    private var points = ArrayList<Vec3d>()
    private var thread: Thread? = null
    private fun getDelay():Int = 1000/cpsV.get()

    override fun onEnable() {
        timer.reset()
        points.clear()
        aliveTimer.reset()
    }
    override fun onDisable() {
        timer.reset()
        points.clear()
        aliveTimer.reset()
    }
    override val tag: String get() = modeV.get()

    @EventTarget
    fun onPacket(event: PacketEvent){
        if(event.packet is SPacketPlayerPosLook){
            timer.reset()
        }
        val isMovePacket=(event.packet is CPacketPlayer.Position || event.packet is CPacketPlayer.PositionRotation)
        if(noRegen.get()&&event.packet is CPacketPlayer&&!isMovePacket){
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if (modeV.get() == "ClickAura" && aliveTimer.hasTimePassed(aliveTicks.get() * 50L)) points.clear()
        if(!timer.hasTimePassed(getDelay().toLong())) return
        when (modeV.get().toLowerCase()){
            "aura" -> {
                if(thread == null || !thread!!.isAlive){
                    thread = Thread { doTeleportAura() }
                    points.clear()
                    timer.reset()
                    thread!!.start()
                }else{
                    timer.reset()
                }
            }

            "clickaura" -> {
                if(mc.gameSettings.keyBindAttack.isKeyDown&&(thread == null || !thread!!.isAlive)){
                    thread = Thread { doTeleportAura() }
                    aliveTimer.reset()
                    points.clear()
                    timer.reset()
                    thread!!.start()
                }else{
                    timer.reset()
                }
            }
        }
    }

    private fun doTeleportAura(){
        val targets=ArrayList<EntityLivingBase>()
        val entityList = mc.world!!.loadedEntityList.toList()
        try {
            for(entity in entityList){
                if(entity is EntityLivingBase && EntityUtils.isSelected(entity,true)
                    &&mc.player!!.getDistanceToEntity(entity)<distV.get()){
                    targets.add(entity)
                }
            }
        } catch (e: ConcurrentModificationException){
            println(e)
            return
        }
        if(targets.size==0) return
        targets.sortBy { mc.player!!.getDistanceToEntity(it) }
        var count = 0
        val playerPos=Vec3d(mc.player!!.posX,mc.player!!.posY,mc.player!!.posZ)
        points.add(playerPos)
        for(entity in targets){
            count++
            if(count>targetsV.get()) break
            attackEntity(entity)
        }
    }

    private fun attackEntity(entity: EntityLivingBase){
        val path = PathUtils.findBlinkPath2(mc.player!!.posX,mc.player!!.posY,mc.player!!.posZ,entity.posX,entity.posY,entity.posZ,moveDistanceV.get().toDouble())
        path.forEach {
            val packet = if (packetMode.get() == "C04PacketPlayerPosition") CPacketPlayer.Position(it.x,it.y,it.z,true) else CPacketPlayer.PositionRotation(it.x,it.y,it.z,mc.player!!.rotationYaw,mc.player!!.rotationPitch,true)
            mc.connection!!.sendPacket(packet)
            points.add(it)
        }
        if(swingV.get())
        {
            mc.player!!.swingArm(EnumHand.MAIN_HAND)
        }
        else
        {
            mc.connection!!.sendPacket(CPacketAnimation())
        }
        mc.playerController.attackEntity(mc.player!!,entity)
        for(i in path.size-1 downTo 0){
            val vec=path[i]
            val packet = if (packetMode.get() == "C04PacketPlayerPosition")
                CPacketPlayer.Position(vec.x,vec.y,vec.z,true)
            else
                CPacketPlayer.PositionRotation(vec.x,vec.y,vec.z,mc.player!!.rotationYaw,mc.player!!.rotationPitch,true)
            mc.connection!!.sendPacket(packet)
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        synchronized(points) {
            if(points.isEmpty()||!pathV.get()) return
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

            val colorD = if (colorModeV.get() == "Custom") Color(colorR.get(),colorG.get(),colorB.get()) else ColorUtils.rainbow()

            for (vec in points){
                val x = vec.x - renderPosX
                val y = vec.y - renderPosY
                val z = vec.z - renderPosZ
                val width = 0.3
                val height = mc.player!!.eyeHeight.toDouble()
                GL11.glLoadIdentity()
                mc.entityRenderer.setupCameraTransform(mc.timer.renderPartialTicks, 2)
                RenderUtils.glColor(colorD)
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
    }
}