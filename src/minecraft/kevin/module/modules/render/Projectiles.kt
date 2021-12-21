package kevin.module.modules.render

import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.module.IntegerValue
import kevin.module.ListValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.ColorUtils
import kevin.utils.RenderUtils
import kevin.utils.RotationUtils
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.Entity
import net.minecraft.item.*
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.RayTraceResult
import net.minecraft.util.math.Vec3d
import org.lwjgl.opengl.GL11
import org.lwjgl.util.glu.Cylinder
import org.lwjgl.util.glu.GLU
import java.awt.Color
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class Projectiles : Module("Projectiles", "Allows you to see where arrows will land.", category = ModuleCategory.RENDER) {
    private val colorMode = ListValue("Color", arrayOf("Custom", "BowPower", "Rainbow"), "Custom")

    private val colorRedValue = IntegerValue("R", 0, 0, 255)
    private val colorGreenValue = IntegerValue("G", 160, 0, 255)
    private val colorBlueValue = IntegerValue("B", 255, 0, 255)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val thePlayer = mc.player ?: return
        val theWorld = mc.world ?: return

        val heldItem = thePlayer.heldItemMainhand ?: return

        val item = heldItem.item
        val renderManager = mc.renderManager
        var isBow = false
        var motionFactor = 1.5F
        var motionSlowdown = 0.99F
        val gravity: Float
        val size: Float

        // Check items
        if ((item)is ItemBow) {
            if (!thePlayer.isHandActive)
                return

            isBow = true
            gravity = 0.05F
            size = 0.3F

            // Calculate power of bow
            var power = thePlayer.itemInUseMaxCount / 20f
            power = (power * power + power * 2F) / 3F
            if (power < 0.1F)
                return

            if (power > 1F)
                power = 1F

            motionFactor = power * 3F
        } else if ((item)is ItemFishingRod) {
            gravity = 0.04F
            size = 0.25F
            motionSlowdown = 0.92F
        } else if ((item)is ItemPotion && heldItem.metadata and 16384 != 0) {
            gravity = 0.05F
            size = 0.25F
            motionFactor = 0.5F
        } else {
            if ((item) !is ItemSnowball && (item)!is ItemEnderPearl && (item)!is ItemEgg)
                return

            gravity = 0.03F
            size = 0.25F
        }

        // Yaw and pitch of player
        val yaw = if (RotationUtils.targetRotation != null)
            RotationUtils.targetRotation.yaw
        else
            thePlayer.rotationYaw

        val pitch = if (RotationUtils.targetRotation != null)
            RotationUtils.targetRotation.pitch
        else
            thePlayer.rotationPitch

        val yawRadians = yaw / 180f * Math.PI.toFloat()
        val pitchRadians = pitch / 180f * Math.PI.toFloat()

        // Positions
        var posX = renderManager.renderPosX - cos(yawRadians) * 0.16F
        var posY = renderManager.renderPosY + thePlayer.eyeHeight - 0.10000000149011612
        var posZ = renderManager.renderPosZ - sin(yawRadians) * 0.16F

        // Motions
        var motionX = (-sin(yawRadians) * cos(pitchRadians)
                * if (isBow) 1.0 else 0.4)
        var motionY = -sin((pitch +
                if ((item)is ItemPotion && heldItem.metadata and 16384 != 0) -20 else 0)
                / 180f * 3.1415927f) * if (isBow) 1.0 else 0.4
        var motionZ = (cos(yawRadians) * cos(pitchRadians)
                * if (isBow) 1.0 else 0.4)
        val distance = sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)

        motionX /= distance
        motionY /= distance
        motionZ /= distance
        motionX *= motionFactor
        motionY *= motionFactor
        motionZ *= motionFactor

        // Landing
        var landingPosition: RayTraceResult? = null
        var hasLanded = false
        var hitEntity = false

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        // Start drawing of path
        GL11.glDepthMask(false)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_ALPHA_TEST)
        GL11.glDisable(GL11.GL_TEXTURE_2D)

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        when (colorMode.get().toLowerCase()) {
            "custom" -> {
                RenderUtils.glColor(Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get(), 255))
            }
            "bowpower" -> {
                RenderUtils.glColor(interpolateHSB(Color.RED, Color.GREEN, (motionFactor / 30) * 10))
            }
            "rainbow" -> {
                RenderUtils.glColor(ColorUtils.rainbow())
            }
        }
        GL11.glLineWidth(2f)

        worldRenderer.begin(GL11.GL_LINE_STRIP, DefaultVertexFormats.POSITION)

        while (!hasLanded && posY > 0.0) {
            // Set pos before and after
            var posBefore = Vec3d(posX, posY, posZ)
            var posAfter = Vec3d(posX + motionX, posY + motionY, posZ + motionZ)

            // Get landing position
            landingPosition = theWorld.rayTraceBlocks(posBefore, posAfter, false,
                true, false)

            // Set pos before and after
            posBefore = Vec3d(posX, posY, posZ)
            posAfter = Vec3d(posX + motionX, posY + motionY, posZ + motionZ)

            // Check if arrow is landing
            if (landingPosition != null) {
                hasLanded = true
                posAfter = Vec3d(landingPosition.hitVec.x, landingPosition.hitVec.y, landingPosition.hitVec.z)
            }

            // Set arrow box
            val arrowBox = AxisAlignedBB(posX - size, posY - size, posZ - size, posX + size,
                posY + size, posZ + size).expand(motionX, motionY, motionZ).expand(1.0, 1.0, 1.0)

            val chunkMinX = floor((arrowBox.minX - 2.0) / 16.0).toInt()
            val chunkMaxX = floor((arrowBox.maxX + 2.0) / 16.0).toInt()
            val chunkMinZ = floor((arrowBox.minZ - 2.0) / 16.0).toInt()
            val chunkMaxZ = floor((arrowBox.maxZ + 2.0) / 16.0).toInt()

            // Check which entities colliding with the arrow
            val collidedEntities = mutableListOf<Entity>()

            for (x in chunkMinX..chunkMaxX)
                for (z in chunkMinZ..chunkMaxZ)
                    theWorld.getChunkFromChunkCoords(x, z)
                        .getEntitiesWithinAABBForEntity(thePlayer, arrowBox, collidedEntities, null)

            // Check all possible entities
            for (possibleEntity in collidedEntities) {
                if (possibleEntity.canBeCollidedWith() && possibleEntity != thePlayer) {
                    val possibleEntityBoundingBox = possibleEntity.entityBoundingBox
                        .expand(size.toDouble(), size.toDouble(), size.toDouble())

                    val possibleEntityLanding = possibleEntityBoundingBox
                        .calculateIntercept(posBefore, posAfter) ?: continue

                    hitEntity = true
                    hasLanded = true
                    landingPosition = possibleEntityLanding
                }
            }

            // Affect motions of arrow
            posX += motionX
            posY += motionY
            posZ += motionZ

            val blockState = theWorld.getBlockState(BlockPos(posX, posY, posZ))

            // Check is next position water
            if (blockState.block.getMaterial(blockState) == Material.WATER) {
                // Update motion
                motionX *= 0.6
                motionY *= 0.6
                motionZ *= 0.6
            } else { // Update motion
                motionX *= motionSlowdown.toDouble()
                motionY *= motionSlowdown.toDouble()
                motionZ *= motionSlowdown.toDouble()
            }

            motionY -= gravity.toDouble()

            // Draw path
            worldRenderer.pos(posX - renderManager.renderPosX, posY - renderManager.renderPosY,
                posZ - renderManager.renderPosZ).endVertex()
        }

        // End the rendering of the path
        tessellator.draw()
        GL11.glPushMatrix()
        GL11.glTranslated(posX - renderManager.renderPosX, posY - renderManager.renderPosY,
            posZ - renderManager.renderPosZ)

        if (landingPosition != null) {
            // Switch rotation of hit cylinder of the hit axis
            when (landingPosition.sideHit!!.axis.ordinal) {
                0 -> GL11.glRotatef(90F, 0F, 0F, 1F)
                2 -> GL11.glRotatef(90F, 1F, 0F, 0F)
            }

            // Check if hitting a entity
            if (hitEntity)
                RenderUtils.glColor(Color(255, 0, 0, 150))
        }

        // Rendering hit cylinder
        GL11.glRotatef(-90F, 1F, 0F, 0F)

        val cylinder = Cylinder()
        cylinder.drawStyle = GLU.GLU_LINE
        cylinder.draw(0.2F, 0F, 0F, 60, 1)

        GL11.glPopMatrix()
        GL11.glDepthMask(true)

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_ALPHA_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)

        GL11.glColor4f(1F, 1F, 1F, 1F)
    }

    fun interpolateHSB(startColor: Color, endColor: Color, process: Float): Color? {
        val startHSB = Color.RGBtoHSB(startColor.red, startColor.green, startColor.blue, null)
        val endHSB = Color.RGBtoHSB(endColor.red, endColor.green, endColor.blue, null)

        val brightness = (startHSB[2] + endHSB[2]) / 2
        val saturation = (startHSB[1] + endHSB[1]) / 2

        val hueMax = if (startHSB[0] > endHSB[0]) startHSB[0] else endHSB[0]
        val hueMin = if (startHSB[0] > endHSB[0]) endHSB[0] else startHSB[0]

        val hue = (hueMax - hueMin) * process + hueMin
        return Color.getHSBColor(hue, saturation, brightness)
    }
}