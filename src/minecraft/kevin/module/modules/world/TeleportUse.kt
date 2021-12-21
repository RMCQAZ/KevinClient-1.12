package kevin.module.modules.world

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.event.UpdateEvent
import kevin.module.*
import kevin.module.modules.render.BlockOverlay
import kevin.utils.*
import kevin.utils.timers.MSTimer
import kevin.utils.timers.TickTimer
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.item.ItemBlock
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.concurrent.CopyOnWriteArrayList

class TeleportUse : Module("TeleportUse", "Allows you to use items over a long distance.", category = ModuleCategory.WORLD) {
    private var points=CopyOnWriteArrayList<Vec3d>()
    private var thread: Thread? = null
    private val doSwing=BooleanValue("Swing",true)
    private val path=BooleanValue("RenderPath",true)
    private val pos=BooleanValue("RenderPos",true)
    private val rangeCheck = BooleanValue("RangeCheck",true)
    private val moveDistanceValue= FloatValue("MoveDistance",5F,2F,15F)
    private val packetMode = ListValue("PacketMode", arrayOf("C04","C06"),"C04")
    private val timer = MSTimer()
    private val aliveTicks = IntegerValue("AliveTicks",20,10,50)
    private val glLineWidthValue = FloatValue("glLineWidth",2F,1F,4F)
    private val blockLineWidthValue = FloatValue("blockLineWidth",2F,1F,4F)
    private val aliveTimer = TickTimer()

    private val colorMode = ListValue("ColorMode", arrayOf("Custom","Rainbow"),"Custom")
    private val colorR = IntegerValue("R",255,0,255)
    private val colorG = IntegerValue("G",255,0,255)
    private val colorB = IntegerValue("B",255,0,255)

    private val onlyBlockV = BooleanValue("OnlyBlock",true)

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

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if (timer.hasTimePassed(250L) && mc.gameSettings.keyBindUseItem.isKeyDown){
            if(thread == null || !thread!!.isAlive) {
                if ((mc.player?.inventory?.getCurrentItem()?.item) !is ItemBlock && onlyBlockV.get()) return
                if (rangeCheck.get()&&(KevinClient.moduleManager.getModule("BlockOverlay") as BlockOverlay).currentBlock!=null) return
                thread = Thread {
                    val entityLookVec = mc.player!!.lookVec ?: return@Thread
                    val lookVec = Vec3d(entityLookVec.x * 300, entityLookVec.y * 300, entityLookVec.z * 300)
                    val posVec = Vec3d(mc.player!!.posX, mc.player!!.posY + 1.62, mc.player!!.posZ)
                    val endBlock = mc.world!!.rayTraceBlocks(posVec, posVec.add(lookVec), false, false, false) ?: return@Thread
                    doTPUse(endBlock)
                }
                aliveTimer.reset()
                points.clear()
                timer.reset()
                thread!!.start()
            }else timer.reset()
        }
        if (aliveTimer.hasTimePassed(aliveTicks.get())) points.clear()
        aliveTimer.update()
    }

    private fun doTPUse(targetBlock: RayTraceResult){

        points.add(Vec3d(mc.player!!.posX,mc.player!!.posY,mc.player!!.posZ))
        val targetBlockPos = targetBlock.blockPos?:return
        val path = PathUtils.findBlinkPath2(mc.player!!.posX,mc.player!!.posY,mc.player!!.posZ,targetBlockPos.x.toDouble(),targetBlockPos.y.toDouble(),targetBlockPos.z.toDouble(),moveDistanceValue.get().toDouble())
        path.forEach {
            val packet = if (packetMode.get() == "C04")
                CPacketPlayer.Position(it.x,it.y,it.z,true)
            else
                CPacketPlayer.PositionRotation(it.x,it.y,it.z,mc.player!!.rotationYaw,mc.player!!.rotationPitch,true)
            mc.connection!!.sendPacket(packet)
            points.add(it)
        }

        val itemStack = mc.player!!.heldItemMainhand ?: return

        if (targetBlock.blockPos == null && targetBlock.sideHit == null) return

        if (mc.playerController.processRightClickBlock(
                mc.player!!,
                mc.world!!,
                targetBlock.blockPos,
                targetBlock.sideHit!!,
                targetBlock.hitVec,
                EnumHand.MAIN_HAND
            ) == EnumActionResult.SUCCESS
        ) {
            if (doSwing.get()) {
                mc.player!!.swingArm(EnumHand.MAIN_HAND)
            } else {
                mc.connection!!.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))
            }
        }


        for(i in path.size-1 downTo 0){
            val vec=path[i]
            val packet = if (packetMode.get() == "C04")
                CPacketPlayer.Position(vec.x,vec.y,vec.z,true)
            else
                CPacketPlayer.PositionRotation(vec.x,vec.y,vec.z,mc.player!!.rotationYaw,mc.player!!.rotationPitch,true)
            mc.connection!!.sendPacket(packet)
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val color = if (colorMode.get() == "Custom") Color(colorR.get(),colorG.get(),colorB.get()) else ColorUtils.rainbow()

        synchronized(points) {
            if(points.isEmpty()||!path.get()) return@synchronized
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

            for (vec in points){
                val x = vec.x - renderPosX
                val y = vec.y - renderPosY
                val z = vec.z - renderPosZ
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

        if (!pos.get()) return
        if (rangeCheck.get()&&(KevinClient.moduleManager.getModule("BlockOverlay") as BlockOverlay).currentBlock!=null) return
        val entityLookVec = mc.player!!.lookVec ?: return
        val lookVec = Vec3d(entityLookVec.x * 300, entityLookVec.y * 300, entityLookVec.z * 300)
        val posVec = Vec3d(mc.player!!.posX, mc.player!!.posY + 1.62, mc.player!!.posZ)
        val endBlock = mc.world!!.rayTraceBlocks(posVec, posVec.add(lookVec), false, false, false) ?: return
        val blockPosition = endBlock.blockPos ?: return
        if (!mc.world!!.worldBorder.contains(blockPosition)) return
        val block = mc.world!!.getBlockState(blockPosition).block
        val partialTicks = event.partialTicks
        val st = BlockUtils.getState(blockPosition) ?:return
        if (st.block.getMaterial(st) == Material.AIR || ((mc.player?.inventory?.getCurrentItem()?.item) !is ItemBlock && onlyBlockV.get())) return

        GlStateManager.enableBlend()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        RenderUtils.glColor(color)
        GL11.glLineWidth(blockLineWidthValue.get())
        GlStateManager.disableTexture2D()
        GL11.glDepthMask(false)
        val thePlayer = mc.player ?: return

        val x = thePlayer.lastTickPosX + (thePlayer.posX - thePlayer.lastTickPosX) * partialTicks
        val y = thePlayer.lastTickPosY + (thePlayer.posY - thePlayer.lastTickPosY) * partialTicks
        val z = thePlayer.lastTickPosZ + (thePlayer.posZ - thePlayer.lastTickPosZ) * partialTicks

        val axisAlignedBB = block.getSelectedBoundingBox(mc.world!!.getBlockState(blockPosition), mc.world, blockPosition)
            .expand(0.0020000000949949026, 0.0020000000949949026, 0.0020000000949949026)
            .offset(-x, -y, -z)

        RenderUtils.drawSelectionBoundingBox(axisAlignedBB)
        GL11.glDepthMask(true)
        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.resetColor()
    }
}