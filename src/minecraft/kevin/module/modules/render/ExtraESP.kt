package kevin.module.modules.render

import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.module.*
import kevin.utils.ColorUtils
import kevin.utils.RenderUtils
import kevin.utils.WorldToScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.entity.item.EntityTNTPrimed
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector3f
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

class ExtraESP : Module("ExtraESP", "More ESPs.", category = ModuleCategory.RENDER) {

    private val modeArray = arrayOf(
        "Box",
        "OtherBox",
        "2D",
        "None"
    )
    private val defaultMode = "Box"

    private val tnt = ListValue("TNT",modeArray,defaultMode)
    private val tntRainbow = BooleanValue("TNT-Rainbow",false)
    private val tntRed = IntegerValue("TNT-Red",255,0,255)
    private val tntGreen = IntegerValue("TNT-Green",0,0,255)
    private val tntBlue = IntegerValue("TNT-Blue",0,0,255)
    private val tntAlpha = IntegerValue("TNT-Alpha",255,0,255)

    private val crystal = ListValue("End_Crystal",modeArray,defaultMode)
    private val crystalRainbow = BooleanValue("Crystal-Rainbow",false)
    private val crystalRed = IntegerValue("Crystal-Red",0,0,255)
    private val crystalGreen = IntegerValue("Crystal-Green",111,0,255)
    private val crystalBlue = IntegerValue("Crystal-Blue",255,0,255)
    private val crystalAlpha = IntegerValue("Crystal-Alpha",255,0,255)

    private val fallingBlock = ListValue("FallingBlock",modeArray,defaultMode)
    private val fallingBlockRainbow = BooleanValue("FallingBlock-Rainbow",false)
    private val fallingBlockRed = IntegerValue("FallingBlock-Red",0,0,255)
    private val fallingBlockGreen = IntegerValue("FallingBlock-Green",255,0,255)
    private val fallingBlockBlue = IntegerValue("FallingBlock-Blue",0,0,255)
    private val fallingBlockAlpha = IntegerValue("FallingBlock-Alpha",255,0,255)

    private val boat = ListValue("Boat",modeArray,defaultMode)
    private val boatRainbow = BooleanValue("Boat-Rainbow",false)
    private val boatRed = IntegerValue("Boat-Red",0,0,255)
    private val boatGreen = IntegerValue("Boat-Green",0,0,255)
    private val boatBlue = IntegerValue("Boat-Blue",255,0,255)
    private val boatAlpha = IntegerValue("Boat-Alpha",255,0,255)

    @EventTarget
    fun onRender3D(event : Render3DEvent) {
        mc.world.loadedEntityList.forEach {
            when(it){
                is EntityTNTPrimed -> drawESP(tnt,it,
                    if (tntRainbow.get())
                        ColorUtils.rainbow(tntAlpha.get())
                    else Color(tntRed.get(),tntGreen.get(),tntBlue.get(),tntAlpha.get())
                )

                is EntityEnderCrystal -> drawESP(crystal,it,
                    if (crystalRainbow.get())
                        ColorUtils.rainbow(crystalAlpha.get())
                    else Color(crystalRed.get(),crystalGreen.get(),crystalBlue.get(),crystalAlpha.get())
                )

                is EntityFallingBlock -> drawESP(fallingBlock,it,
                    if (fallingBlockRainbow.get())
                        ColorUtils.rainbow(fallingBlockAlpha.get())
                    else Color(fallingBlockRed.get(),fallingBlockGreen.get(),fallingBlockBlue.get(),fallingBlockAlpha.get())
                )

                is EntityBoat -> drawESP(boat,it,
                    if (boatRainbow.get())
                        ColorUtils.rainbow(boatAlpha.get())
                    else Color(boatRed.get(),boatGreen.get(),boatBlue.get(),boatAlpha.get())
                )
            }
        }
    }
    private fun drawESP(mode: ListValue,entityIn: Entity,color: Color){
        when(mode.get()){
            "Box", "OtherBox" -> RenderUtils.drawEntityBox(entityIn, color, mode equal "Box")
            "2D" -> {
                val mvMatrix = WorldToScreen.getMatrix(GL11.GL_MODELVIEW_MATRIX)
                val projectionMatrix = WorldToScreen.getMatrix(GL11.GL_PROJECTION_MATRIX)

                GL11.glPushAttrib(GL11.GL_ENABLE_BIT)
                GL11.glEnable(GL11.GL_BLEND)
                GL11.glDisable(GL11.GL_TEXTURE_2D)
                GL11.glDisable(GL11.GL_DEPTH_TEST)
                GL11.glMatrixMode(GL11.GL_PROJECTION)
                GL11.glPushMatrix()
                GL11.glLoadIdentity()
                GL11.glOrtho(0.0, mc.displayWidth.toDouble(), mc.displayHeight.toDouble(), 0.0, -1.0, 1.0)
                GL11.glMatrixMode(GL11.GL_MODELVIEW)
                GL11.glPushMatrix()
                GL11.glLoadIdentity()
                GL11.glDisable(GL11.GL_DEPTH_TEST)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                GlStateManager.enableTexture2D()
                GL11.glDepthMask(true)
                GL11.glLineWidth(3.5f)

                val renderManager = mc.renderManager
                val timer = mc.timer
                val bb = entityIn.entityBoundingBox
                    .offset(-entityIn.posX, -entityIn.posY, -entityIn.posZ)
                    .offset(entityIn.lastTickPosX + (entityIn.posX - entityIn.lastTickPosX) * timer.renderPartialTicks,
                        entityIn.lastTickPosY + (entityIn.posY - entityIn.lastTickPosY) * timer.renderPartialTicks,
                        entityIn.lastTickPosZ + (entityIn.posZ - entityIn.lastTickPosZ) * timer.renderPartialTicks)
                    .offset(-renderManager.renderPosX, -renderManager.renderPosY, -renderManager.renderPosZ)
                val boxVertices = arrayOf(doubleArrayOf(bb.minX, bb.minY, bb.minZ), doubleArrayOf(bb.minX, bb.maxY, bb.minZ), doubleArrayOf(bb.maxX, bb.maxY, bb.minZ), doubleArrayOf(bb.maxX, bb.minY, bb.minZ), doubleArrayOf(bb.minX, bb.minY, bb.maxZ), doubleArrayOf(bb.minX, bb.maxY, bb.maxZ), doubleArrayOf(bb.maxX, bb.maxY, bb.maxZ), doubleArrayOf(bb.maxX, bb.minY, bb.maxZ))
                var minX = Float.MAX_VALUE
                var minY = Float.MAX_VALUE
                var maxX = -1f
                var maxY = -1f
                for (boxVertex in boxVertices) {
                    val screenPos = WorldToScreen.worldToScreen(Vector3f(boxVertex[0].toFloat(), boxVertex[1].toFloat(), boxVertex[2].toFloat()), mvMatrix, projectionMatrix, mc.displayWidth, mc.displayHeight)
                        ?: continue
                    minX = min(screenPos.x, minX)
                    minY = min(screenPos.y, minY)
                    maxX = max(screenPos.x, maxX)
                    maxY = max(screenPos.y, maxY)
                }
                if (minX > 0 || minY > 0 || maxX <= mc.displayWidth || maxY <= mc.displayWidth) {
                    GL11.glColor4f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f, 1.0f)
                    GL11.glBegin(GL11.GL_LINE_LOOP)
                    GL11.glVertex2f(minX, minY)
                    GL11.glVertex2f(minX, maxY)
                    GL11.glVertex2f(maxX, maxY)
                    GL11.glVertex2f(maxX, minY)
                    GL11.glEnd()
                }

                GL11.glEnable(GL11.GL_DEPTH_TEST)
                GL11.glMatrixMode(GL11.GL_PROJECTION)
                GL11.glPopMatrix()
                GL11.glMatrixMode(GL11.GL_MODELVIEW)
                GL11.glPopMatrix()
                GL11.glPopAttrib()
            }
            else -> return
        }
    }
}