package kevin.module.modules.misc

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.event.WorldEvent
import kevin.hud.element.elements.Notification
import kevin.module.BooleanValue
import kevin.module.ListValue
import kevin.module.Module
import kevin.utils.ChatUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSword

class KillerDetector : Module("KillerDetector","Detect who has a sword in his hand.") {
    var killer: EntityPlayer? = null
    private val messageMode = ListValue("MessageMode", arrayOf("Notification","Chat"),"Chat")
    private val autoSay = BooleanValue("AutoSay",true)

    @EventTarget
    fun onWorld(event: WorldEvent){
        killer = null
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if (killer == null) for (e in mc.world.loadedEntityList){
            if (e !is EntityPlayer||e == mc.player) continue
            if (e.inventory.getCurrentItem().item is ItemSword){
                killer = e
                when(messageMode.get()){
                    "Notification" -> KevinClient.hud.addNotification(Notification("Killer is ${e.name}!"),"KillerDetector")
                    "Chat" -> ChatUtils.messageWithPrefix("[KillerDetector] §cKiller is ${e.name}!")
                }
                if (autoSay.get()) mc.player.sendChatMessage("is ${e.name}")
                break
            }
        }
    }
}