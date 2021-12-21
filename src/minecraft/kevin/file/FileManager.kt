package kevin.file

import com.google.gson.GsonBuilder
import kevin.KevinClient
import kevin.utils.ChatUtils
import kevin.utils.Mc
import net.minecraft.client.Minecraft
import java.io.File

object FileManager : Mc() {
    @JvmStatic
    val PRETTY_GSON = GsonBuilder().setPrettyPrinting().create()
    private val dir = File(mc.mcDataDir,"Kevin")
    val via = File(dir,"Via")
    val spammerDir = File(dir, "SpammerMessages")
    val capesDir = File(dir, "Capes")
    val skinsDir = File(dir, "Skins")
    val serverIconsDir = File(dir, "ServerIcons")
    val configsDir = File(dir, "Configs")
    val killMessages = File(dir, "KillMessages")
    val playerModels = File(dir, "PlayerModels")
    val scripts = File(dir, "Scripts")
    val modulesConfig: FileConfig = ModulesConfig(File(dir, "modules.json"))
    val hudConfig: FileConfig = HudConfig(File(dir, "hud.json"))
    val altsFile = File(dir, "accounts.json")
    val adminNamesFile = File(dir, "AdminNames.txt")
    val findChestsFile = File(dir,"FindChests.txt")
    fun load(){
        if (dir.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created Dir.")
        if (via.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created ViaVersion Dir.")
        if (spammerDir.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created Spammers Dir.")
        if (capesDir.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created Capes Dir.")
        if (skinsDir.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created Skins Dir.")
        if (serverIconsDir.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created ServerIcons Dir.")
        if (configsDir.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created Configs Dir.")
        if (killMessages.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created KillMessages Dir.")
        if (playerModels.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created PlayerModels Dir.")
        if (scripts.mkdir())
            Minecraft.LOGGER.info("${KevinClient.logPrefix} Created Scripts Dir.")
    }
    fun saveConfig(config: FileConfig) {
        saveConfig(config, false)
    }
    private fun saveConfig(config: FileConfig, ignoreStarting: Boolean) {
        if (!ignoreStarting && KevinClient.isStarting) return
        try {
            if (!config.hasConfig()) config.createConfig()
            config.saveConfig()
        } catch (t: Throwable) {
            ChatUtils.messageWithPrefix("§cSaveConfig Error: $t")
        }
    }
    fun loadConfigs(vararg configs: FileConfig) {
        for (fileConfig in configs) loadConfig(fileConfig)
    }
    fun loadConfig(config: FileConfig) {
        if (!config.hasConfig()) {
            saveConfig(config, true)
            return
        }
        try {
            config.loadConfig()
        } catch (t: Throwable) {
            t.printStackTrace()
        }
    }
    fun saveAllConfigs() {
        for (field in javaClass.declaredFields) {
            if (field.type == FileConfig::class.java) {
                try {
                    if (!field.isAccessible) field.isAccessible = true
                    val fileConfig: FileConfig = field[this] as FileConfig
                    saveConfig(fileConfig)
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
    }
}