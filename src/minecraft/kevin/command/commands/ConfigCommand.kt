package kevin.command.commands

import kevin.command.Command
import kevin.file.ConfigManager
import kevin.utils.ChatUtils

class ConfigCommand : Command("config") {
    override fun exec(args: Array<String>) {
        if (args.size == 1){
            usageMessage()
            return
        }
        when(args.size){
            2 -> {
                when{
                    args[1].equals("Save",true) || args[1].equals("Load",true) -> {
                        chatSyntax("§6config §b${args[1]} §c<ConfigName>")
                    }
                    else -> usageMessage()
                }
            }
            else -> {
                var name = ""
                val list = args.toMutableList()
                repeat(2) { list.removeFirst() }
                var c = 0
                list.forEach {
                    c+=1
                    name += if (c == list.size){
                        it
                    }else "$it "
                }
                when{
                    args[1].equals("Save",true) -> {
                        try {
                            ConfigManager.saveConfig(name)
                            ChatUtils.messageWithPrefix("§aSuccessfully saved config §b$name.")
                        }catch (e: Exception){
                            ChatUtils.messageWithPrefix("§cError: $e")
                        }
                    }
                    args[1].equals("Load",true) -> {
                        try {
                            when(ConfigManager.loadConfig(name)){
                                0 -> {
                                    ChatUtils.messageWithPrefix("§aSuccessfully loaded config §b$name.")
                                }
                                1 -> {
                                    ChatUtils.messageWithPrefix("§eWarning: §eThe §eModules §econfig §efile §eis §emissing.§eSuccessfully §eloaded §eHUD §econfig §b$name.")
                                }
                                2 -> {
                                    ChatUtils.messageWithPrefix("§eWarning: §eThe §eHUD §econfig §efile §eis §emissing.§eSuccessfully §eloaded §eModules §econfig §b$name.")
                                }
                                3 -> {
                                    ChatUtils.messageWithPrefix("§cFailed to load config §b$name.§cFile not found.")
                                }
                            }
                        }catch (e: Exception){
                            ChatUtils.messageWithPrefix("§cError: $e")
                        }
                    }
                    else -> usageMessage()
                }
            }
        }
    }
    private fun usageMessage() = chatSyntax("config <Save/Load> <ConfigName>")
}