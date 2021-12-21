package kevin.module.modules.misc

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.event.WorldEvent
import kevin.module.Module
import kevin.utils.ChatUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.SPacketEntityStatus

class TotemCount : Module("TotemCount", "Count how many totem has popped.") {
    private val entityMap = HashMap<EntityLivingBase,Int>()
    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is SPacketEntityStatus && packet.opCode.toInt() == 35) {
            val entity = packet.getEntity(mc.world)
            if (entity !is EntityLivingBase || entity == mc.player) return
            if (entityMap[entity] == null)
                entityMap[entity] = 1
            else entityMap[entity] = entityMap[entity]!! + 1
            ChatUtils.messageWithPrefix("§5${entity.name} popped §4${entityMap[entity]} §5totem(s)§7.")
        }
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val diedEntities = entityMap.filter { it.key.isDead || it.key.health == 0F }
        diedEntities.forEach { (e, t) ->
            ChatUtils.messageWithPrefix("§5${e.name} died after popped §4$t §5totem(s)§7.")
        }
        diedEntities.forEach{ entityMap.remove(it.key) }
    }
    @EventTarget
    fun onWorld(event: WorldEvent) {
        entityMap.clear()
    }
    override fun onDisable() {
        entityMap.clear()
    }
}