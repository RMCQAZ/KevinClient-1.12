package kevin.module.modules.combat

import kevin.event.AttackEvent
import kevin.event.EventTarget
import kevin.module.IntegerValue
import kevin.module.Module
import kevin.module.ModuleCategory
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.CPacketEntityAction

class SuperKnockback : Module("SuperKnockback", "Increases knockback dealt to other entities.", category = ModuleCategory.COMBAT) {

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if ((event.targetEntity)is EntityLivingBase) {
            if (event.targetEntity.hurtTime > hurtTimeValue.get()) return

            val player = mc.player ?: return

            if (player.isSprinting) mc.connection!!.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SPRINTING))

            mc.connection!!.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SPRINTING))
            mc.connection!!.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.STOP_SPRINTING))
            mc.connection!!.sendPacket(CPacketEntityAction(player, CPacketEntityAction.Action.START_SPRINTING))
            player.isSprinting = true
            player.serverSprintState = true
        }
    }
}