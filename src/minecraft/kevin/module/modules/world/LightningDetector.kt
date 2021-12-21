package kevin.module.modules.world

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.hud.element.elements.Notification
import kevin.module.ListValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.ChatUtils
import net.minecraft.network.play.server.SPacketSpawnGlobalEntity

object LightningDetector : Module("LightningDetector","Detect lightning.",category = ModuleCategory.WORLD) {
    private val mode = ListValue("MessageMode", arrayOf("Chat","Notification"),"Notification")
    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet is SPacketSpawnGlobalEntity) {
            val packet = event.packet
            if(packet.type != 1) return
            when(mode.get()){
                "Chat" -> ChatUtils.messageWithPrefix("§eLightning §9at §cX:${packet.x/32} §cY:${packet.y/32} §cZ:${packet.z/32}")
                "Notification" -> KevinClient.hud.addNotification(Notification("Lightning at X:${packet.x/32} Y:${packet.y/32} Z:${packet.z/32}"),name)
            }
        }
    }
}