package kevin.module.modules.misc

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.module.Module
import kevin.module.TextValue
import net.minecraft.network.play.client.CPacketChatMessage

class ChatSuffix : Module("ChatSuffix","Automatically suffix what you say.") {
    private val suffix = TextValue("Suffix"," | <KevinClient>-${KevinClient.version}")
    @EventTarget
    fun onPacket(event: PacketEvent){
        if (event.packet is CPacketChatMessage &&
            !event.packet.message.endsWith(suffix.get())&&
            !event.packet.message.startsWith("/")){
            event.packet.message += suffix.get()
        }
    }
}