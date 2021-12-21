package kevin.module.modules.combat

import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.event.UpdateEvent
import kevin.module.*
import kevin.utils.EntityUtils
import kevin.utils.RenderUtils
import kevin.utils.RotationUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.item.ItemBow
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import java.awt.Color

class BowAura : Module("BowAura","Bow KillAura.",category = ModuleCategory.COMBAT) {

    private val silentValue = BooleanValue("Silent", true)
    private val predictValue = BooleanValue("Predict", true)
    private val throughWallsValue = BooleanValue("ThroughWalls", false)
    private val predictSizeValue = FloatValue("PredictSize", 2F, 0.1F, 5F)
    private val priorityValue = ListValue("Priority", arrayOf("Health", "Distance", "Direction"), "Direction")
    private val maxDistance = FloatValue("MaxDistance",100F,5F,200F)
    private val markValue = BooleanValue("Mark", true)

    private var target: Entity? = null

    @EventTarget fun onUpdate(event: UpdateEvent){
        target = null
        val invBow = bow ?: return
        target = getTarget(priorityValue.get()) ?: return
        mc.player!!.inventory.currentItem = invBow
        mc.gameSettings.keyBindUseItem.pressed = true
        RotationUtils.faceBow(target, silentValue.get(), predictValue.get(), predictSizeValue.get())
        if (mc.player.isHandActive&&mc.player.itemInUseMaxCount > 20){
            mc.gameSettings.keyBindUseItem.pressed = false
            mc.player.stopActiveHand()
            mc.connection!!.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
        }
    }
    @EventTarget fun onRender3D(event: Render3DEvent){
        if (!markValue.get()||target==null) return
        RenderUtils.drawPlatform(target, Color(37, 126, 255, 70))
    }
    override fun onDisable() { target = null }
    private val bow: Int?
    get() {
        var arrow = false
        for (inv in mc.player.inventory.mainInventory){
            if (inv != null&&inv.item==Items.ARROW) {
                arrow = true
                break
            }
        }
        if (!arrow&&!mc.playerController.isInCreativeMode) return null
        for (i in 0..8) {
            val itemStack = mc.player.inventory.getStackInSlot(i) ?: continue
            if (itemStack.item is ItemBow) {
                return i
            }
        }
        return null
    }
    private fun getTarget(priorityMode: String): Entity? {
        val targets = mc.world!!.loadedEntityList.filter {
            it is EntityLivingBase&&
                    EntityUtils.isSelected(it, true)&&
                    (throughWallsValue.get() || mc.player!!.canEntityBeSeen(it))&&
                    mc.player.getDistanceToEntity(it)<=maxDistance.get()
        }
        return when {
            priorityMode.equals("distance", true) -> targets.minByOrNull { mc.player!!.getDistanceToEntity(it) }
            priorityMode.equals("direction", true) -> targets.minByOrNull { RotationUtils.getRotationDifference(it) }
            priorityMode.equals("health", true) -> targets.minByOrNull { (it as EntityLivingBase).health }
            else -> null
        }
    }
}