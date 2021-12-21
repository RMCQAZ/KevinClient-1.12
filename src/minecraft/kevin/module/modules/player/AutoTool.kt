package kevin.module.modules.player

import kevin.event.ClickBlockEvent
import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.timers.MSTimer
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.util.math.BlockPos

class AutoTool : Module(name = "AutoTool", description = "Automatically selects the best tool in your inventory to mine a block.", category = ModuleCategory.PLAYER) {
    val silentValue = BooleanValue("Silent", true)
    var nowSlot = 0
    private val switchTimer = MSTimer()
    private var needReset = false

    @EventTarget
    fun onClick(event: ClickBlockEvent) {
        switchSlot(event.clickedBlock ?: return)
        switchTimer.reset()
        needReset = true
    }
    @EventTarget fun onUpdate(event: UpdateEvent){
        if (needReset) {
            if (switchTimer.hasTimePassed(100)){
                needReset = false
                if (nowSlot!=mc.player!!.inventory.currentItem){
                    mc.connection!!.sendPacket(CPacketHeldItemChange(mc.player!!.inventory.currentItem))
                    nowSlot = mc.player!!.inventory.currentItem
                }
            }
        }
    }
    @EventTarget fun onPacket(event: PacketEvent){
        if (event.packet is CPacketHeldItemChange) {
            nowSlot = event.packet.slotId
        }
    }

    fun switchSlot(blockPos: BlockPos) {
        var bestSpeed = 1F
        var bestSlot = -1

        val blockState = mc.world!!.getBlockState(blockPos)

        for (i in 0..8) {
            val item = mc.player!!.inventory.getStackInSlot(i) ?: continue
            val speed = item.getStrVsBlock(blockState)

            if (speed > bestSpeed) {
                bestSpeed = speed
                bestSlot = i
            }
        }
        if (bestSlot != -1 && bestSlot != nowSlot) {
            if (!mc.player!!.inventory.getStackInSlot(nowSlot).isEmpty&&mc.player!!.inventory.getStackInSlot(nowSlot).getStrVsBlock(blockState) >= bestSpeed) return
            if (silentValue.get()) {
                mc.connection!!.sendPacket(CPacketHeldItemChange(bestSlot))
                nowSlot = bestSlot
            } else {
                mc.player!!.inventory.currentItem = bestSlot
            }
        }
    }
}