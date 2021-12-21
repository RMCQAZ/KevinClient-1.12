package kevin.command

import kevin.command.commands.*
import kevin.module.modules.misc.AdminDetector
import kevin.module.modules.misc.AutoDisable
import kevin.script.ScriptManager
import kevin.utils.ChatUtils

class CommandManager {
    var prefix = "."
    val commands = HashSet<Command>()

    init {
        registerCommand(
            ToggleCommand(),
            HelpCommand(),
            BindCommand(),
            BindsCommand(),
            SayCommand(),
            LoginCommand(),
            StateCommand(),
            SkinCommand(),
            ConfigCommand(),
            HideCommand(),
            AutoDisableCommand(),
            AdminDetectorCommand(),
            ScriptManager
        )
    }

    fun execCommand(command: String): Boolean{
        if (!command.startsWith(prefix)) return true
        val commandNoPrefix = command.removePrefix(prefix)
        if (commandNoPrefix.isEmpty()||commandNoPrefix.isBlank()){
            commandNotFond()
        } else {
            val args = commandNoPrefix.split(" ").toMutableList()
            val cmd = getCommand(args.first())
            if (cmd != null){
                cmd.exec(args.toTypedArray())
            } else commandNotFond()
        }
        return false
    }

    fun registerCommand(vararg commands: Command) = this.commands.addAll(commands)

    private fun getCommand(key: String): Command? {
        commands.forEach{ command ->
            if (command.commandName.equals(key,ignoreCase = true)) return command
            command.alias.forEach{ s ->
                if (s.equals(key,ignoreCase = true)) return command
            }
        }
        return null
    }
    private fun commandNotFond() = ChatUtils.messageWithPrefix("§l§4Command Not Found! Use .help for help.")
}