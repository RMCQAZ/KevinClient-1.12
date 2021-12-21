package kevin.command.commands

import kevin.KevinClient
import kevin.command.Command
import kevin.utils.ChatUtils

class StateCommand : Command("ModuleState") {
    override fun exec(args: Array<String>) {
        ChatUtils.messageWithPrefix("§9Modules State")
        KevinClient.moduleManager.modules.forEach {
            if (it.name!="Targets") ChatUtils.messageWithPrefix("§6${it.name} §9State: ${if (it.state) "§aOn" else "§cOff"}")
        }
    }
}