package kevin.module

import kevin.command.Command
import kevin.utils.BlockUtils
import kevin.utils.StringUtils
import net.minecraft.block.Block
import net.minecraft.item.Item

class ModuleCommand(private val module: Module, val values: List<Value<*>>) : Command(module.name.lowercase()) {
    override fun exec(args: Array<String>) {
        val valueNames = values
            .joinToString(separator = "/") { it.name.toLowerCase() }

        val moduleName = module.name.toLowerCase()

        if (args.size < 2) {
            chatSyntax(if (values.size == 1) "$moduleName $valueNames <value>" else "$moduleName <$valueNames>")
            return
        }

        val value = module.getValue(args[1])

        if (value == null) {
            chatSyntax("$moduleName <$valueNames>")
            return
        }

        if (args.size < 3 && value is BooleanValue) {
            val newValue = !value.get()
            value.set(newValue)
            chat("§7${module.name} §8${args[1]}§7 was toggled ${if (newValue) "§8on§7" else "§8off§7" + "."}")
            playEdit()
        } else {
            if (args.size < 3) {
                if (value is IntegerValue || value is FloatValue || value is DoubleValue || value is TextValue || value is BooleanValue)
                    chatSyntax("$moduleName ${args[1].toLowerCase()} <value>")
                else if (value is ListValue)
                    chatSyntax("$moduleName ${args[1].toLowerCase()} <${value.values.joinToString(separator = "/").toLowerCase()}>")
                return
            }

            try {
                when (value) {
                    is BooleanValue -> {
                        val newValue = try {
                            args[2].toBoolean()
                        } catch (e : Exception){
                            chat("§8${args[2]}§7 cannot be converted to boolean!")
                            return
                        }
                        value.set(newValue)
                        chat("§7${module.name} §8${args[1]}§7 was toggled ${if (newValue) "§8on§7" else "§8off§7" + "."}")
                        playEdit()
                    }
                    is BlockValue -> {
                        val id: Int = try {
                            args[2].toInt()
                        } catch (exception: NumberFormatException) {
                            val tmpId = Block.getBlockFromName(args[2])?.let { Block.getIdFromBlock(it) }

                            if (tmpId == null || tmpId <= 0) {
                                chat("§7Block §8${args[2]}§7 does not exist!")
                                return
                            }

                            tmpId
                        }

                        value.set(id)
                        chat("§7${module.name} §8${args[1].toLowerCase()}§7 was set to §8${BlockUtils.getBlockName(id)}§7.")
                        playEdit()
                        return
                    }
                    is IntegerValue -> value.set(args[2].toInt())
                    is FloatValue -> value.set(args[2].toFloat())
                    is DoubleValue -> value.set(args[2].toFloat())
                    is ListValue -> {
                        if (!value.contains(args[2])) {
                            chatSyntax("$moduleName ${args[1].toLowerCase()} <${value.values.joinToString(separator = "/").toLowerCase()}>")
                            return
                        }

                        value.set(args[2])
                    }
                    is TextValue -> value.set(StringUtils.toCompleteString(args, 2))
                }

                chat("§7${module.name} §8${args[1]}§7 was set to §8${value.get()}§7.")
                playEdit()
            } catch (e: NumberFormatException) {
                chat("§8${args[2]}§7 cannot be converted to number!")
            }
        }
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty()) return emptyList()

        return when (args.size) {
            1 -> values
                .filter { it.name.startsWith(args[0], true) }
                .map { it.name.toLowerCase() }
            2 -> {
                when(module.getValue(args[0])) {
                    is BlockValue -> {
                        return Item.REGISTRY.keys
                            .map { it.resourcePath.toLowerCase() }
                            .filter { it.startsWith(args[1], true) }
                    }
                    is ListValue -> {
                        values.forEach { value ->
                            if (!value.name.equals(args[0], true))
                                return@forEach
                            if (value is ListValue)
                                return value.values.filter { it.startsWith(args[1], true) }
                        }
                        return emptyList()
                    }
                    else -> emptyList()
                }
            }
            else -> emptyList()
        }
    }
}