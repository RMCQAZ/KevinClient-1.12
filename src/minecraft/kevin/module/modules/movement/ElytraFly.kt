package kevin.module.modules.movement

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.MovementUtils
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemElytra
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.server.SPacketPlayerPosLook

class ElytraFly : Module("ElytraFly","Makes you fly faster on Elytra.",category = ModuleCategory.MOVEMENT)  {

    private val auto = BooleanValue("Auto",true)
    private val stopOnDisable = BooleanValue("StopOnDisable",true)
    private val vertical = FloatValue("Vertical",1f, 0.1f, 5f)
    private val horizontal = FloatValue("Horizontal",2f, 0.1f, 5f)

    override fun onDisable() {
        if (stopOnDisable.get()&&mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST).item == Items.ELYTRA) {
            stopElytraFly()
            mc.player.setFlag(7,false)
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent){
        val chestSlot = mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST)
        val packet = event.packet
        if (chestSlot.item != Items.ELYTRA
            || !ItemElytra.isUsable(chestSlot)
            || mc.player.onGround
            || packet !is SPacketPlayerPosLook
            || !auto.get()) {
            return
        }
        mc.player.connection.sendPacket(CPacketEntityAction(mc.player,CPacketEntityAction.Action.START_FALL_FLYING))
        mc.player.setFlag(7,true)
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if (mc.player.isRiding) {
            return
        }
        val chestSlot = mc.player.getItemStackFromSlot(EntityEquipmentSlot.CHEST)
        if (chestSlot.item != Items.ELYTRA || !ItemElytra.isUsable(chestSlot)) {
            return
        }
        if (auto.get()&&!mc.player.isElytraFlying){
            if (mc.player.onGround)
                mc.player.jump()
            else {
                mc.player.connection.sendPacket(CPacketEntityAction(mc.player,CPacketEntityAction.Action.START_FALL_FLYING))
                mc.player.setFlag(7,true)
            }
        }
        // If player is flying
        if (mc.player.isElytraFlying) {
            mc.player.motionX = .0
            mc.player.motionZ = .0
            if (MovementUtils.isMoving) {
                MovementUtils.strafe(horizontal.get())
            }
            mc.player.motionY = when {
                mc.gameSettings.keyBindJump.isKeyDown -> vertical.get().toDouble()
                mc.gameSettings.keyBindSprint.isKeyDown -> -vertical.get().toDouble()
                else -> .0
            }
        }
    }

    private fun stopElytraFly(){
        mc.playerController.windowClick(0, 6, 0, ClickType.PICKUP, mc.player)
        Thread(){
            Thread.sleep(50)
            mc.playerController.windowClick(0, 6, 0, ClickType.PICKUP, mc.player)
        }.start()
    }
}