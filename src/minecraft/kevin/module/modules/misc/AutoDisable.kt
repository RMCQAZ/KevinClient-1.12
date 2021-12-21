package kevin.module.modules.misc

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.WorldEvent
import kevin.file.FileManager
import kevin.hud.element.elements.Notification
import kevin.module.BooleanValue
import kevin.module.Module
import kevin.utils.ChatUtils
import kevin.utils.timers.MSTimer
import net.minecraft.network.play.server.SPacketPlayerPosLook
import java.util.*

object AutoDisable : Module("AutoDisable","Auto disable modules.(Use Command .AutoDisable <ModuleName> <add/remove> <World/SetBack/All>)") {
    fun add(module: Module,mode: String){
        module.autoDisable = true to mode.lowercase(Locale.getDefault())
        ChatUtils.messageWithPrefix("§aModule successfully added to list.")
        if (!this.state) ChatUtils.messageWithPrefix("§eDon't forget to open AutoDisable module!")
        FileManager.saveConfig(FileManager.modulesConfig)
    }
    fun remove(module: Module) {
        module.autoDisable = false to ""
        ChatUtils.messageWithPrefix("§aModule successfully removed from list.")
        FileManager.saveConfig(FileManager.modulesConfig)
    }
    private val timer = MSTimer()
    private val notification = BooleanValue("Notification",true)
    @EventTarget fun onWorld(event: WorldEvent){
        KevinClient.moduleManager.modules
            .filter { it.autoDisable.first&&(it.autoDisable.second=="world"||it.autoDisable.second=="all")&&it.state }
            .forEach {
                it.state = false
                if (notification.get()) KevinClient.hud.addNotification(Notification("Auto disabled module ${it.name}."),"AutoDisable")
            }
        timer.reset()
    }
    @EventTarget fun onPacket(event: PacketEvent) {
        if (event.packet !is SPacketPlayerPosLook || !timer.hasTimePassed(3000)) return
        KevinClient.moduleManager.modules
            .filter { it.autoDisable.first&&(it.autoDisable.second=="setback"||it.autoDisable.second=="all")&&it.state }
            .forEach {
                it.state = false
                if (notification.get()) KevinClient.hud.addNotification(Notification("Auto disabled module ${it.name}."),"AutoDisable")
            }
    }
}