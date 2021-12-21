package kevin.command.commands

import kevin.KevinClient
import kevin.command.Command
import kevin.utils.ChatUtils

class HelpCommand : Command("Help","h") {
    override fun exec(args: Array<String>) {
        val prefix = KevinClient.commandManager.prefix
        ChatUtils.message("§b<Help>")
        ChatUtils.message("§a${prefix}t/toggle <ModuleName> <on/off> §9Enable/Disable module.")
        ChatUtils.message("§a${prefix}h/help §9Show this message.")
        ChatUtils.message("§a${prefix}binds §9Show binds.")
        ChatUtils.message("§a${prefix}bind <Module> <Key> §9Bind Module To a Key.")
        ChatUtils.message("§a${prefix}binds clear §9Clear binds.")
        ChatUtils.message("§a${prefix}login <Name> <Password?> §9Login.")
        ChatUtils.message("§a${prefix}say §9Say.")
        ChatUtils.message("§a${prefix}modulestate §9Show module state.")
        ChatUtils.message("§a${prefix}<ModuleName> <Option> <Value> §9Set module option value.")
        ChatUtils.message("§a${prefix}config <save/load> <Name> §9Load/Save config.")
        ChatUtils.message("§a${prefix}skin <Set/Clear/List/Reload/Mode> <Value> §9Change your skin.")
        ChatUtils.message("§a${prefix}hide <ModuleName> §9Hide a module.")
        ChatUtils.message("§a${prefix}AutoDisableSet <ModuleName> <add/remove> <World/SetBack/All> §9Add/Remove a module to AutoDisable List.")
        ChatUtils.message("§a${prefix}reloadScripts §9Reload Scripts.")
        ChatUtils.message("§a${prefix}reloadScript §9Reload Scripts.")
        ChatUtils.message("§a${prefix}Admin <Add/Remove> <Name> Add admin name to detect list.")
    }
}