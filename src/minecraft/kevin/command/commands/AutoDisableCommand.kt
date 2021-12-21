package kevin.command.commands

import kevin.KevinClient
import kevin.command.Command
import kevin.module.modules.misc.AutoDisable

class AutoDisableCommand : Command("AutoDisableSet") {
    override fun exec(args: Array<String>) {
        if (args.size<3) {
            usageMessage()
            return
        }
        val module = KevinClient.moduleManager.getModule(args[1])
        if (module==null){
            chat("§cNo module called ${args[1]}")
            return
        }
        if (args.size == 3) {
            if (args[2].equals("remove",true)) {
                AutoDisable.remove(module)
            } else if (args[2].equals("add",true))
                addUsageMessage(args)
            else usageMessage()
        } else {
            if (args[2].equals("remove",true)) {
                AutoDisable.remove(module)
            } else if (args[2].equals("add",true)) {
                if (args[3].equals("World",true)||
                    args[3].equals("Setback",true)||
                    args[3].equals("All",true)) AutoDisable.add(module, args[3])
                else addUsageMessage(args)
            } else usageMessage()
        }
    }
    private fun addUsageMessage(args: Array<out String>) = chatSyntax("autodisable §a${args[1]} ${args[2]} §c<World/SetBack/All>")
    private fun usageMessage() = chatSyntax("autodisable <ModuleName> <add/remove> <World/SetBack/All>")
}