package kevin.command.commands

import kevin.command.Command
import net.minecraft.network.play.client.CPacketChatMessage

class SayCommand : Command("Say") {
    override fun exec(args: Array<String>) {
        if (args.size < 2) {
            chatSyntax("say <Message>")
            return
        }
        val list = args.toMutableList()
        list.removeFirst()
        val stringBuilder = StringBuilder()
        for (msg in list){
            stringBuilder.append("$msg ")
        }
        mc.player.connection.sendPacket(CPacketChatMessage(stringBuilder.toString().removeSuffix(" ")))
    }
}