package kevin.module.modules.misc

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.module.Module
import kevin.module.ModuleCategory
import net.minecraft.network.play.client.CPacketResourcePackStatus
import net.minecraft.network.play.server.SPacketResourcePackSend
import java.net.URI
import java.net.URISyntaxException

class ResourcePackSpoof : Module("ResourcePackSpoof", "Prevents servers from forcing you to download their resource pack.", category = ModuleCategory.MISC) {
    @EventTarget
    fun onPacket(event: PacketEvent) {
        if ((event.packet)is SPacketResourcePackSend) {
            val packet = event.packet

            val url = packet.url
            val hash = packet.hash

            try {
                val scheme = URI(url).scheme
                val isLevelProtocol = "level" == scheme

                if ("http" != scheme && "https" != scheme && !isLevelProtocol)
                    throw URISyntaxException(url, "Wrong protocol")

                if (isLevelProtocol && (url.contains("..") || !url.endsWith("/resources.zip")))
                    throw URISyntaxException(url, "Invalid levelstorage resourcepack path")

                mc.connection!!.sendPacket(CPacketResourcePackStatus(CPacketResourcePackStatus.Action.ACCEPTED))
                mc.connection!!.sendPacket(CPacketResourcePackStatus(CPacketResourcePackStatus.Action.SUCCESSFULLY_LOADED))
            } catch (e: URISyntaxException) {
                println("Failed to handle resource pack $e")
                mc.connection!!.sendPacket(CPacketResourcePackStatus(CPacketResourcePackStatus.Action.FAILED_DOWNLOAD))
            }
        }
    }
}