package kevin.module.modules.misc

import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.module.Module
import kevin.utils.firstItem
import kevin.utils.hotbarSlot
import kevin.utils.hotbarSlots
import kevin.utils.switchHotbar
import kevin.utils.timers.MSTimer
import net.minecraft.init.Items
import net.minecraft.network.play.client.CPacketPlayerTryUseItem
import net.minecraft.util.EnumHand
import net.minecraft.util.math.RayTraceResult
import org.lwjgl.input.Mouse

class MidClickPearl : Module("MidClickPearl", "Throws a pearl automatically when you middle click in air.") {
    private val delayTimer = MSTimer()
    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if (mc.currentScreen == null && Mouse.isButtonDown(2) && delayTimer.hasTimePassed(200L)) {
            delayTimer.reset()
            val objectMouseOver = mc.objectMouseOver
            if (objectMouseOver == null || objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) {
                val pearlSlot = mc.player.hotbarSlots.firstItem(Items.ENDER_PEARL)
                if (pearlSlot != null) {
                    switchHotbar(pearlSlot.hotbarSlot) {
                        mc.connection!!.sendPacket(CPacketPlayerTryUseItem(EnumHand.MAIN_HAND))
                    }
                }
            }
        }
    }
}