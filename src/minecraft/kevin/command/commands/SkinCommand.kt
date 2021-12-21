package kevin.command.commands

import kevin.command.Command
import kevin.skin.SkinManager
import kevin.utils.ChatUtils

class SkinCommand : Command("Skin") {
    override fun exec(args: Array<String>) {
        when(args.size){
            1 -> {
                usageMessage()
            }
            2 -> {
                val v = args[1]
                when{
                    v.equals("Set",true) -> {
                        chatSyntax("§6skin §b$v §c<SkinName/None>")
                    }
                    v.equals("Mode",true) -> {
                        chatSyntax("§6skin §b$v §c<Default/Slim>")
                    }
                    v.equals("List",true) -> {
                        SkinManager.list()
                    }
                    v.equals("Clear",true) -> {
                        clearSkin()
                    }
                    v.equals("Reload",true) -> {
                        SkinManager.load()
                        ChatUtils.messageWithPrefix("§9Reloaded")
                    }
                    else -> {
                        usageMessage()
                    }
                }
            }
            else -> {
                val v = args[1]
                val list = args.toMutableList()
                repeat(2) { list.removeFirst() }
                var v2 = ""
                var c = 0
                list.forEach {
                    c += 1
                    v2 += if (c==list.size){
                        it
                    } else {
                        "$it "
                    }
                }
                when{
                    v.equals("Set",true) -> {
                        if (v2.equals("None",true)) {
                            clearSkin()
                            return
                        }
                        val state = SkinManager.set(v2)
                        if (state==0){
                            ChatUtils.messageWithPrefix("§9Skin was set to §b$v2.")
                        } else {
                            ChatUtils.messageWithPrefix("§cNo skin called §b$v2.")
                        }
                    }
                    v.equals("Mode",true) -> {
                        when{
                            v2.equals("Default",true) -> {
                                SkinManager.setMode(SkinManager.SkinMode.Default)
                                ChatUtils.messageWithPrefix("§9Skin mode was set to §b$v2.")
                            }
                            v2.equals("Slim",true) -> {
                                SkinManager.setMode(SkinManager.SkinMode.Slim)
                                ChatUtils.messageWithPrefix("§9Skin mode was set to §b$v2.")
                            }
                            else -> {
                                chatSyntax("§6skin §b$v §c<Default/Slim>")
                            }
                        }
                    }
                    v.equals("Clear",true) -> {
                        clearSkin()
                    }
                    v.equals("List",true) -> {
                        SkinManager.list()
                    }
                    v.equals("Reload",true) -> {
                        SkinManager.load()
                        ChatUtils.messageWithPrefix("§9Reloaded")
                    }
                    else -> {
                        usageMessage()
                    }
                }
            }
        }
    }
    private fun clearSkin(){
        SkinManager.nowSkin = null
        SkinManager.save()
        ChatUtils.messageWithPrefix("§9Skin was set to §bNone.")
    }
    private fun usageMessage() = chatSyntax("skin <Set/Clear/List/Reload/Mode>")
}