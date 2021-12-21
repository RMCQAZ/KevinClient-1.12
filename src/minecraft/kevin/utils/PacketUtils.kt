package kevin.utils

import net.minecraft.network.Packet

object PacketUtils : Mc() {
    private val packetList = HashSet<Packet<*>>()
    fun sendPacketNoEvent(packet: Packet<*>){
        packetList.add(packet)
        mc.connection!!.sendPacket(packet)
    }
    @JvmStatic
    fun needReceiveEvent(packetIn: Packet<*>) = !packetList.contains(packetIn)
}