package kevin.script

import kevin.KevinClient
import kevin.command.Command
import kevin.file.FileManager
import kevin.module.ModuleCategory
import kevin.module.modules.render.ClickGui
import kevin.utils.Mc
import net.minecraft.client.Minecraft
import org.python.core.Py
import org.python.core.PyDictionary
import org.python.core.PyFunction
import org.python.core.PyObject
import org.python.util.PythonInterpreter
import java.io.File
import java.io.FileInputStream


object ScriptManager : Command("reloadScripts","reloadScript") {
    private val scripts = arrayListOf<Script>()
    override fun exec(args: Array<String>) {
        load()
    }
    fun load(){
        val dir = FileManager.scripts
        if (!dir.exists()) return
        val files = dir.listFiles() ?: return
        Minecraft.LOGGER.info("[ScriptManager] Loading scripts...")
        val time = System.currentTimeMillis()
        scripts.forEach { script ->
            script.registeredModules.forEach {
                if (it in KevinClient.moduleManager.modules) KevinClient.moduleManager.unregisterModule(it)
            }
        }
        scripts.clear()
        files.forEach {
            try {
                val script = Script(it)
                script.initScript()
                scripts += script
            } catch (e: Throwable){
                Minecraft.LOGGER.error("[ScriptManager] Error loading script ${it.name}!",e)
            }
        }
        FileManager.loadConfig(FileManager.modulesConfig)
        Minecraft.LOGGER.info("[ScriptManager] Loaded ${scripts.size} script(s),${System.currentTimeMillis()-time}ms.")
        KevinClient.clickGUI = ClickGui.ClickGUI()
        KevinClient.newClickGui = ClickGui.NewClickGui()
        Minecraft.LOGGER.info("[ScriptManager] Reloaded ClickGui.")
    }

    class Script(private val scriptFile: File) : Mc() {
        lateinit var scriptName: String
        lateinit var scriptVersion: String
        lateinit var scriptAuthors: Set<String>

        val registeredModules = mutableListOf<ScriptModule>()
        val registeredCommands = mutableListOf<Command>()

        private val pythonInterpreter = PythonInterpreter()
        init {
            val interpreter = PythonInterpreter()
            interpreter.set("Script",this)
            interpreter.set("MinecraftLogger",Minecraft.LOGGER)
            interpreter.exec("""
def registerScript(scriptObject):
    Script.scriptName = scriptObject["name"]
    Script.scriptVersion = scriptObject["version"]
    s = set()
    s.add(scriptObject["authors"])
    Script.scriptAuthors = s
    msg="[Script] Loaded script Name:'"+Script.scriptName+"' Version:'"+Script.scriptVersion+"' Authors:"+str(Script.scriptAuthors)+"."
    MinecraftLogger.info(msg)
    return Script
            """.trimIndent())
            pythonInterpreter.set("registerScript", interpreter.get("registerScript",PyFunction::class.java))
            pythonInterpreter.set("mc", mc)
            pythonInterpreter.set("KevinClient", KevinClient)
            pythonInterpreter.set("Setting", Setting)
        }

        fun initScript(){
            pythonInterpreter.execfile(FileInputStream(scriptFile))
        }

        @Suppress("unused")
        fun registerModule(moduleObject: PyObject,pyObject: PyObject) {
            try {
                moduleObject as PyDictionary
                val name = moduleObject["name"].toString()
                val description = try {
                    moduleObject["description"].toString()
                } catch (e: Throwable) {
                    ""
                }
                val category = try {
                    ModuleCategory.valueOf(moduleObject["category"].toString().uppercase())
                } catch (e: Throwable) {
                    ModuleCategory.MISC
                }
                val module = ScriptModule(name, description, category, moduleObject)
                KevinClient.moduleManager.registerModule(module)
                registeredModules += module
                pyObject.__call__(Py.java2py(module))
                Minecraft.LOGGER.info("[Script '${this.scriptName}'] Successfully loaded module '$name'.")
            }catch (e:Throwable){
                Minecraft.LOGGER.error("[Script '${this.scriptName}'] Error loading Module.",e)
            }
        }
    }
}