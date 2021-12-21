package kevin.module.modules.render

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.module.modules.player.Reach
import kevin.utils.RenderUtils
import net.minecraft.block.BlockShulkerBox
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityItem
import net.minecraft.inventory.ItemStackHelper
import net.minecraft.item.Item
import net.minecraft.item.ItemShulkerBox
import net.minecraft.item.ItemStack
import net.minecraft.src.Reflector
import net.minecraft.util.EntitySelectors
import net.minecraft.util.NonNullList
import net.minecraft.util.math.AxisAlignedBB
import org.lwjgl.opengl.GL11
import java.awt.Color

class ShulkerBoxPreview : Module("ShulkerBoxPreview", "Previews shulker boxes.", category = ModuleCategory.RENDER) {
    val guiValue = BooleanValue("GUI", true)
    val guiScaleValue = FloatValue("GuiScale",0.75F,0.5F,1F)
    private val itemValue = BooleanValue("Item", true)
    private val scaleValue = FloatValue("Scale",0.5F,0.1F,2F)

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!itemValue.get()) return
        val entity = mc.renderViewEntity ?: return
        val partialTicks = 1F
        val reach = KevinClient.moduleManager.getModule("Reach") as Reach
        val vec3d = entity.getPositionEyes(partialTicks)
        var d0 = if (reach.state) reach.maxRange.toDouble() else mc.playerController.blockReachDistance.toDouble()
        var d1 = d0
        if (mc.playerController.extendedReach()) {
            d1 = 6.0
            d0 = d1
        }
        if (mc.objectMouseOver != null) {
            d1 = mc.objectMouseOver.hitVec.distanceTo(vec3d)
        }
        if (reach.state) {
            d1 = reach.combatReachValue.get().toDouble()
            val movingObjectPosition = entity.rayTrace(d1, partialTicks)
            if (movingObjectPosition != null) d1 = movingObjectPosition.hitVec.distanceTo(vec3d)
        }
        val vec3d1 = entity.getLook(1.0f)
        val vec3d2 = vec3d.addVector(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0)
        val list: MutableList<Entity> = mc.world.getEntitiesInAABBexcluding(
            entity,
            entity.entityBoundingBox.expand(vec3d1.x * d0, vec3d1.y * d0, vec3d1.z * d0).grow(1.0, 1.0, 1.0),
            Predicates.and(EntitySelectors.NOT_SPECTATING,
                Predicate { ent -> ent != null && ent is EntityItem && !ent.item.isEmpty && ent.item.item is ItemShulkerBox })
        )
        val d2 = d1
        val entityList = list.filter { entity1 ->
            val axisalignedbb: AxisAlignedBB =
                entity1.entityBoundingBox.grow(0.2)
            val raytraceresult = axisalignedbb.calculateIntercept(vec3d, vec3d2)

            if (axisalignedbb.contains(vec3d)) {
                if (d2 >= 0.0) {
                    return@filter true
                }
            } else if (raytraceresult != null) {
                val d3: Double = vec3d.distanceTo(raytraceresult.hitVec)
                if (d3 < d2 || d2 == 0.0) {
                    var flag1 = false
                    if (Reflector.ForgeEntity_canRiderInteract.exists()) {
                        flag1 = Reflector.callBoolean(entity1, Reflector.ForgeEntity_canRiderInteract)
                    }
                    if (!flag1 && entity1.lowestRidingEntity === entity.lowestRidingEntity) {
                        if (d2 == 0.0) {
                            return@filter true
                        }
                    } else {
                        return@filter true
                    }
                }
            }
            return@filter false
        }
        entityList.forEach {
            drawPreview(it as EntityItem)
        }
    }
    private fun drawPreview(shulkerBox: EntityItem) {
        val shulker = shulkerBox.item.item as ItemShulkerBox
        val block = shulker.block as BlockShulkerBox
        val color = Color(block.color.colorValue)
        RenderUtils.drawEntityBox(shulkerBox, color, true)

        // Push
        GL11.glPushMatrix()

        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL)
        GL11.glPolygonOffset(1.0f, -10000000f)

        // Translate to player position
        val renderManager = mc.renderManager
        val timer = mc.timer

        GL11.glTranslated( // Translate to player position with render pos and interpolate it
            shulkerBox.lastTickPosX + (shulkerBox.posX - shulkerBox.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX,
            shulkerBox.lastTickPosY + (shulkerBox.posY - shulkerBox.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY + shulkerBox.eyeHeight.toDouble() + 0.55,
            shulkerBox.lastTickPosZ + (shulkerBox.posZ - shulkerBox.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
        )

        // Rotate view to player
        GL11.glRotatef(-mc.renderManager.playerViewY, 0F, 1F, 0F)
        GL11.glRotatef(mc.renderManager.playerViewX, 1F, 0F, 0F)

        // Scale
        var distance = mc.player.getDistanceToEntity(shulkerBox) / 4F

        if (distance < 1F)
            distance = 1F

        val scale = (distance / 150F) * (2F + scaleValue.get())

        // Disable lightning and depth test
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_DEPTH_TEST)

        // Enable blend
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glScalef(-scale, -scale, scale)

        val width = 18 * 9 / 2
        val height = 18 * 3 / 2
        val font = mc.fontRenderer
        val name = shulkerBox.item.displayName

        RenderUtils.drawBorderedRect(
            -width - 2F,
            -height-2F-font.FONT_HEIGHT*scale-8F,
            width + 2F,
            height + 2F,
            2F,
            color.rgb,
            Integer.MIN_VALUE
        )

        font.drawString(name,-width+2,-height-8,color.rgb)

        RenderHelper.enableGUIStandardItemLighting()
        var w = 0
        var h = 0
        shulkerBoxItems(shulkerBox.item).forEach { itemStack ->
            if (!itemStack.isEmpty) {
                mc.renderItem.zLevel = -147F
                mc.renderItem.renderItemAndEffectIntoGUI(itemStack, w*18-width, h*18 - height + 1)
                mc.renderItem.renderItemOverlays(mc.fontRenderer, itemStack, w*18-width, h*18 - height + 1)
                GlStateManager.enableAlpha()
                GlStateManager.disableBlend()
                GlStateManager.enableTexture2D()
            }
            if (w < 8) w++ else {
                h++
                w = 0
            }
        }
        RenderHelper.disableStandardItemLighting()

        // Reset caps
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_BLEND)

        // Reset color
        GlStateManager.resetColor()
        GL11.glColor4f(1F, 1F, 1F, 1F)

        GL11.glPolygonOffset(1.0f, 10000000f)
        GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL)

        // Pop
        GL11.glPopMatrix()
    }

    companion object {
        @JvmStatic
        fun shulkerBoxItems(itemStack: ItemStack): List<ItemStack> {
            val nbtTagCompound = itemStack.tagCompound
            val nonnulllist = NonNullList.withSize(27, ItemStack.EMPTY)
            if (nbtTagCompound != null && nbtTagCompound.hasKey("BlockEntityTag", 10)) {
                val nbttagcompound1 = nbtTagCompound.getCompoundTag("BlockEntityTag")
                if (nbttagcompound1.hasKey("Items", 9)) {
                    ItemStackHelper.loadAllItems(nbttagcompound1, nonnulllist)
                }
            }
            return nonnulllist
        }
    }
}