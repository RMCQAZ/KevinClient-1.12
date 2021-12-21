package kevin.utils

import kevin.KevinClient
import net.minecraft.client.Minecraft
import net.minecraft.util.text.TextComponentString

object ChatUtils {
    fun message(message: String) = Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessage(TextComponentString(message))
    fun messageWithPrefix(message: String) = Minecraft.getMinecraft().ingameGUI.chatGUI.printChatMessage(TextComponentString("§l§7[§l§9${KevinClient.clientName}§l§7] $message"))
}