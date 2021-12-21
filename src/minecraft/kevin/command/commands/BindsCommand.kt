package kevin.command.commands

import kevin.KevinClient
import kevin.command.Command
import org.lwjgl.input.Keyboard

class BindsCommand : Command("Binds") {
    override fun exec(args: Array<String>) {
        if (args.size >= 2){
            if (args[1].equals("clear",true)){
                for (module in KevinClient.moduleManager.modules) module.keyBind = Keyboard.KEY_NONE
                chat("§9Removed All Binds!")
                return
            }
        }
        chat("§9Binds:")
        KevinClient.moduleManager.modules.filter { it.keyBind != Keyboard.KEY_NONE }.forEach {
            chat("§b> §9${it.name}: §a§l${Keyboard.getKeyName(it.keyBind)}")
        }
    }
}