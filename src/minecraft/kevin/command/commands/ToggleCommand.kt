package kevin.command.commands

import kevin.KevinClient
import kevin.command.Command

class ToggleCommand : Command("toggle","t") {
    override fun exec(args: Array<String>) {
        if (args.size < 2) {
            chatSyntax("toggle/t §c<ModuleName> §c<on/off>")
            return
        }
        for (module in KevinClient.moduleManager.modules){
            if (module.name.equals(args[1],ignoreCase = true)){
                if (args.size > 2){
                    if (args[2].equals("on",ignoreCase = true)){
                        module.state = true
                        chat("§aEnable §e${module.name} §9Module")
                        return
                    }else if (args[2].equals("off",ignoreCase = true)){
                        module.state = false
                        chat("§cDisable §e${module.name} §9Module")
                        return
                    }else {
                        module.toggle()
                        chat("§9${if (module.state) "§aEnable" else "§cDisable"} §e${module.name} §9Module")
                        return
                    }
                }else{
                    module.toggle()
                    chat("§9${if (module.state) "§aEnable" else "§cDisable"} §e${module.name} §9Module")
                    return
                }
            }
        }
        chat("§cNo module called ${args[1]}")
    }
}