package kevin.module.modules.misc

import joptsimple.internal.Strings
import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.file.FileManager
import kevin.hud.element.elements.Notification
import kevin.module.*
import kevin.utils.ChatUtils
import kevin.utils.timers.TickTimer
import net.minecraft.network.play.client.CPacketTabComplete
import net.minecraft.network.play.server.SPacketTabComplete
import java.io.File

object AdminDetector : Module("AdminDetector","Detect server admins.") {
    val adminNamesFile:File by lazy {
        if (!FileManager.adminNamesFile.exists()) FileManager.adminNamesFile.createNewFile()
        FileManager.adminNamesFile
    }

    private val modeValue = ListValue("Mode", arrayOf("Tab"),"Tab")
    private val tabCommand = TextValue("TabCommand","/tell")
    private val waitTicks = IntegerValue("WaitTick",100,0,200)
    private val notificationMode = ListValue("NotificationMode", arrayOf("Chat","Notification"),"Chat")
    private val noNotFindNotification = BooleanValue("NoNotFindNotification",true)

    private val timer = TickTimer()
    private var waiting = false

    @EventTarget fun onUpdate(event: UpdateEvent){
        timer.update()
        if (!timer.hasTimePassed(waitTicks.get()+1)) return
        when(modeValue.get()){
            "Tab" -> {
                mc.connection!!.sendPacket(CPacketTabComplete("${tabCommand.get()} ",null,false))
                waiting = true
                timer.reset()
            }
        }
    }
    @EventTarget fun onPacket(event: PacketEvent){
        val packet = event.packet
        when(modeValue.get()){
            "Tab" -> {
                if (!waiting) return
                if (packet is SPacketTabComplete){
                    val players = packet.matches
                    val admins = adminNamesFile.readLines().toMutableList()
                    admins.removeAll { it.isEmpty() }
                    val findAdmins = arrayListOf<String>()
                    players.forEach {
                        if (it in admins) findAdmins.add(it)
                    }
                    n(findAdmins)
                    waiting = false
                    event.cancelEvent()
                }
            }
        }
    }

    private fun n(findAdmins:ArrayList<String>){
        if (findAdmins.isEmpty()) {
            if (!noNotFindNotification.get()) when(notificationMode.get()){
                "Chat" -> ChatUtils.messageWithPrefix("[AdminDetector] No admin find.")
                "Notification" -> KevinClient.hud.addNotification(Notification("No admin find."),"Admin Detector")
            }
            return
        }
        when(notificationMode.get()){
            "Chat" -> ChatUtils.messageWithPrefix("[AdminDetector] Warning: find ${findAdmins.size} admin(s)![§c${Strings.join(findAdmins.toArray(arrayOfNulls<String>(0)), "§7, §c")}]")
            "Notification" -> KevinClient.hud.addNotification(Notification("Warning: find ${findAdmins.size} admin(s)![§c${Strings.join(findAdmins.toArray(arrayOfNulls<String>(0)), "§7, §c")}]"),"Admin Detector")
        }
    }
}