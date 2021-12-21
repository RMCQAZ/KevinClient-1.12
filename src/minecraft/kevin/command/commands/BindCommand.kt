package kevin.command.commands

import kevin.KevinClient
import kevin.command.Command
import org.lwjgl.input.Keyboard

class BindCommand : Command("Bind") {
    override fun exec(args: Array<String>) {
        if (args.size < 3) {
            chatSyntax("bind <ModuleName> <Key/None>")
            return
        }
        val module = KevinClient.moduleManager.getModule(args[1])
        if (module == null) {
            chat("§9Module §c§l" + args[1] + "§9 not found.")
            return
        }
        val key = Keyboard.getKeyIndex(args[2].toUpperCase())
        module.keyBind = key
        chat("§9Bound module §b§l${module.name}§9 to key §a§l${Keyboard.getKeyName(key)}§3.")
        playEdit()
        return
    }
}