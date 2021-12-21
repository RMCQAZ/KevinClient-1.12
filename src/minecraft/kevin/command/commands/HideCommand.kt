package kevin.command.commands

import kevin.KevinClient
import kevin.command.Command
import kevin.file.FileManager
import kevin.utils.ChatUtils

class HideCommand : Command("hide") {
    override fun exec(args: Array<String>) {
        if (args.size == 1) {
            chatSyntax("hide <ModuleName>")
            return
        }
        KevinClient.moduleManager.modules.filter { it.name.equals(args[1],true) }.forEach {
            it.array = !it.array
            if (it.array)
                ChatUtils.messageWithPrefix("§aModule ${it.name} is unhidden.")
            else
                ChatUtils.messageWithPrefix("§aModule ${it.name} is hidden.")
            FileManager.saveConfig(FileManager.modulesConfig)
            return
        }
        ChatUtils.messageWithPrefix("§cNo module called ${args[1]}.")
    }
}