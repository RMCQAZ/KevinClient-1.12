package kevin

import kevin.cape.CapeManager
import kevin.command.CommandManager
import kevin.event.ClientShutdownEvent
import kevin.event.EventManager
import kevin.file.ConfigManager
import kevin.file.FileManager
import kevin.file.ImageManager
import kevin.hud.HUD.Companion.createDefault
import kevin.module.ModuleManager
import kevin.module.modules.render.ClickGui
import kevin.module.modules.render.Renderer
import kevin.script.ScriptManager
import kevin.skin.SkinManager
import kevin.utils.CombatManager
import kevin.via.ViaVersion
import net.minecraft.client.Minecraft

object KevinClient {
    @JvmStatic
    val version = "V1.0"
    @JvmStatic
    val clientName = "Kevin"
    @JvmStatic
    val logPrefix = "[$clientName]"

    lateinit var commandManager: CommandManager
    lateinit var moduleManager: ModuleManager
    lateinit var capeManager: CapeManager
    lateinit var clickGUI: ClickGui.ClickGUI
    lateinit var newClickGui: ClickGui.NewClickGui
    lateinit var hud: kevin.hud.HUD
    lateinit var combatManager: CombatManager

    var isStarting = true

    fun start(){
        Minecraft.LOGGER.info("$logPrefix Starting...")
        val time = System.currentTimeMillis()
        Minecraft.LOGGER.info("$logPrefix loading FileManager...")
        FileManager.load()
        Minecraft.LOGGER.info("$logPrefix FileManager loaded,starting ViaVersion...")
        ViaVersion.start()
        Minecraft.LOGGER.info("$logPrefix ViaVersion started,loading Renderer...")
        Renderer.load()
        Minecraft.LOGGER.info("$logPrefix Renderer loaded,loading CommandManager...")
        commandManager = CommandManager()
        Minecraft.LOGGER.info("$logPrefix CommandManager loaded,loading ModuleManager...")
        moduleManager = ModuleManager()
        moduleManager.registerModules()
        Minecraft.LOGGER.info("$logPrefix ModuleManager loaded,loading ScriptManager...")
        ScriptManager.load()
        Minecraft.LOGGER.info("$logPrefix ScriptManager loaded,loading ModulesConfig...")
        FileManager.loadConfig(FileManager.modulesConfig)
        Minecraft.LOGGER.info("$logPrefix ModulesConfig loaded,loading HUD...")
        hud = createDefault()
        Minecraft.LOGGER.info("$logPrefix HUD loaded,loading HUDConfig...")
        FileManager.loadConfig(FileManager.hudConfig)
        Minecraft.LOGGER.info("$logPrefix HUDConfig loaded,loading ClickGui...")
        clickGUI = ClickGui.ClickGUI()
        newClickGui = ClickGui.NewClickGui()
        Minecraft.LOGGER.info("$logPrefix ClickGui loaded,loading CapeManager...")
        capeManager = CapeManager()
        capeManager.load()
        Minecraft.LOGGER.info("$logPrefix CapeManager loaded,loading SkinManager...")
        SkinManager.load()
        Minecraft.LOGGER.info("$logPrefix SkinManager loaded,loading ImageManager...")
        ImageManager.load()
        Minecraft.LOGGER.info("$logPrefix ImageManager loaded,loading ConfigManager...")
        ConfigManager.load()
        Minecraft.LOGGER.info("$logPrefix ConfigManager loaded,loading CombatManager...")
        combatManager = CombatManager()
        Minecraft.LOGGER.info("$logPrefix CombatManager loaded.")
        Minecraft.LOGGER.info("$logPrefix Initialization completed,${System.currentTimeMillis()-time} MS.")
        isStarting = false
    }

    fun stop(){
        val time = System.currentTimeMillis()
        Minecraft.LOGGER.info("$logPrefix Stopping...")
        EventManager.callEvent(ClientShutdownEvent())
        Minecraft.LOGGER.info("$logPrefix Saving Configs...")
        FileManager.saveAllConfigs()
        Minecraft.LOGGER.info("$logPrefix Configs saved,saving CapeManager...")
        capeManager.save()
        Minecraft.LOGGER.info("$logPrefix CapeManager saved,saving SkinManager...")
        SkinManager.save()
        Minecraft.LOGGER.info("$logPrefix SkinManager saved,saving ImageManager...")
        ImageManager.save()
        Minecraft.LOGGER.info("$logPrefix ImageManager saved.")
        Minecraft.LOGGER.info("$logPrefix Stop completed,${System.currentTimeMillis()-time} MS.")
    }
}