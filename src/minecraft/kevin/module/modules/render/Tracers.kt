package kevin.module.modules.render

import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.module.*
import kevin.utils.ColorUtils
import kevin.utils.EntityUtils
import kevin.utils.RenderUtils
import kevin.utils.isClientFriend
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import java.awt.Color

class Tracers : Module("Tracers", "Draws a line to targets around you.", category = ModuleCategory.RENDER) {

    private val colorMode = ListValue("Color", arrayOf("Custom", "DistanceColor", "Rainbow"), "Custom")

    private val thicknessValue = FloatValue("Thickness", 2F, 1F, 5F)

    private val colorRedValue = IntegerValue("R", 0, 0, 255)
    private val colorGreenValue = IntegerValue("G", 160, 0, 255)
    private val colorBlueValue = IntegerValue("B", 255, 0, 255)

    private val botValue = BooleanValue("Bots", true)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val thePlayer = mc.player ?: return

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(thicknessValue.get())
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        GL11.glBegin(GL11.GL_LINES)

        for (entity in mc.world!!.loadedEntityList) {
            if ((entity)!is EntityLivingBase || !botValue.get() /**&& AntiBot.isBot(entity)**/) continue
            if (entity != thePlayer && EntityUtils.isSelected(entity, false)) {
                var dist = (thePlayer.getDistanceToEntity(entity) * 2).toInt()

                if (dist > 255) dist = 255

                val colorMode = colorMode.get().toLowerCase()
                val color = when {
                    (entity)is EntityPlayer && entity.isClientFriend() -> Color(0, 0, 255, 150)
                    colorMode == "custom" -> Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), 150)
                    colorMode == "distancecolor" -> Color(255 - dist, dist, 0, 150)
                    colorMode == "rainbow" -> ColorUtils.rainbow()
                    else -> Color(255, 255, 255, 150)
                }

                drawTraces(entity, color)
            }
        }

        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
    }

    private fun drawTraces(entity: Entity, color: Color) {
        val thePlayer = mc.player ?: return

        val x = (entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosX)
        val y = (entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosY)
        val z = (entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
                - mc.renderManager.renderPosZ)

        val eyeVector = Vec3d(0.0, 0.0, 1.0)
            .rotatePitch((-Math.toRadians(thePlayer.rotationPitch.toDouble())).toFloat())
            .rotateYaw((-Math.toRadians(thePlayer.rotationYaw.toDouble())).toFloat())

        RenderUtils.glColor(color)

        GL11.glVertex3d(eyeVector.x, thePlayer.eyeHeight.toDouble() + eyeVector.y, eyeVector.z)
        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x, y, z)
        GL11.glVertex3d(x, y + entity.height, z)
    }
}