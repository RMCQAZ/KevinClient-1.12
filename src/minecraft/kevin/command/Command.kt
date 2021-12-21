package kevin.command

import kevin.KevinClient
import kevin.utils.ChatUtils
import kevin.utils.Mc
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.init.SoundEvents

abstract class Command(val commandName: String,vararg val alias: String) : Mc() {
    open fun exec(args: Array<String>){}
    open fun tabComplete(args: Array<String>): List<String> = listOf()
    protected fun playEdit() = mc.soundHandler.playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,1F))
    protected fun chat(msg: String) = ChatUtils.messageWithPrefix("§3$msg")
    protected fun chatSyntax(syntax: String) = ChatUtils.messageWithPrefix("§3Syntax: §7${KevinClient.commandManager.prefix}$syntax")
}