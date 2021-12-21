package kevin.command.commands

import kevin.command.Command
import kevin.module.modules.misc.AdminDetector

class AdminDetectorCommand : Command("Admin") {
    override fun exec(args: Array<String>) {
        if (args.size<3){
            usageMessage()
            return
        }
        val name = args[2]
        val names = AdminDetector.adminNamesFile.readLines()
        when{
            args[1].equals("Add",true) -> {
                if (name in names) {
                    chat("§cName is already in the list!")
                    return
                }
                AdminDetector.adminNamesFile.appendText("$name\n")
                chat("§aName successfully added to the list!")
            }
            args[1].equals("Remove",true) -> {
                if (name !in names){
                    chat("§cName is not in the list!")
                    return
                }
                AdminDetector.adminNamesFile.writeText("")
                names.forEach {
                    if (it!=name&&it.isNotEmpty()){
                        AdminDetector.adminNamesFile.appendText("$it\n")
                    }
                }
                chat("§aName successfully removed from the list!")
            }
            else -> usageMessage()
        }
    }
    private fun usageMessage() = chatSyntax("Admin <Add/Remove> <Name>")
}