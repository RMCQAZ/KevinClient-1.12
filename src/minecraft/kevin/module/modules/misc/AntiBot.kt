package kevin.module.modules.misc

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.TextEvent
import kevin.event.UpdateEvent
import kevin.hud.element.elements.Notification
import kevin.module.BooleanValue
import kevin.module.ListValue
import kevin.module.Module
import kevin.module.ModuleCategory
import net.minecraft.entity.Entity
import net.minecraft.init.Items
import net.minecraft.item.ItemArmor
import net.minecraft.network.play.server.SPacketChat

class AntiBot : Module("AntiBot","Prevents KillAura from attacking AntiCheat bots.", category = ModuleCategory.MISC) {
    private val modeValue = ListValue("Mode", arrayOf("ZQAT-ArmorAndColor(BW)","NoColorArmor","UnusualArmor"),"ZQAT-ArmorAndColor(BW)")

    //Helmet
    private val allowDiamondHelmet = BooleanValue("AllowDiamondHelmet",true)
    private val allowGoldenHelmet = BooleanValue("AllowGoldenHelmet",true)
    private val allowIronHelmet = BooleanValue("AllowIronHelmet",true)
    private val allowChainHelmet = BooleanValue("AllowChainHelmet",true)
    private val allowLeatherHelmet = BooleanValue("AllowLeatherHelmet",true)
    private val allowNoHelmet = BooleanValue("AllowNoHelmet",true)

    //Chestplate
    private val allowDiamondChestplate = BooleanValue("AllowDiamondChestplate",true)
    private val allowGoldenChestplate = BooleanValue("AllowGoldenChestplate",true)
    private val allowIronChestplate = BooleanValue("AllowIronChestplate",true)
    private val allowChainChestplate = BooleanValue("AllowChainChestplate",true)
    private val allowLeatherChestplate = BooleanValue("AllowLeatherChestplate",true)
    private val allowNoChestplate = BooleanValue("AllowNoChestplate",true)

    //Leggings
    private val allowDiamondLeggings = BooleanValue("AllowDiamondLeggings",true)
    private val allowGoldenLeggings = BooleanValue("AllowGoldenLeggings",true)
    private val allowIronLeggings = BooleanValue("AllowIronLeggings",true)
    private val allowChainLeggings = BooleanValue("AllowChainLeggings",true)
    private val allowLeatherLeggings = BooleanValue("AllowLeatherLeggings",true)
    private val allowNoLeggings = BooleanValue("AllowNoLeggings",true)

    //Boots
    private val allowDiamondBoots = BooleanValue("AllowDiamondBoots",true)
    private val allowGoldenBoots = BooleanValue("AllowGoldenBoots",true)
    private val allowIronBoots = BooleanValue("AllowIronBoots",true)
    private val allowChainBoots = BooleanValue("AllowChainBoots",true)
    private val allowLeatherBoots = BooleanValue("AllowLeatherBoots",true)
    private val allowNoBoots = BooleanValue("AllowNoBoots",true)

    private val removeNoColorLeatherArmor = BooleanValue("NoColorLeatherArmor",true)

    override val tag: String
        get() = modeValue.get()

    private var gameStarted = false

    override fun onEnable() {
        gameStarted = false
    }

    @EventTarget
    fun onPacket(event: PacketEvent){
        val packet = event.packet
        when{
            modeValue equal "ZQAT-ArmorAndColor(BW)" -> {
                if (packet is SPacketChat){
                    if (packet.chatComponent.formattedText.contains("获得胜利",true)){
                        gameStarted = false
                        KevinClient.hud.addNotification(Notification("GameEnd!"),"Antibot")
                    }

                    if (packet.chatComponent.formattedText.contains("游戏开始",true)){
                        gameStarted = true
                        KevinClient.hud.addNotification(Notification("GameStart!"),"Antibot")
                    }
                }
            }
        }
    }

    @EventTarget
    fun onText(event: TextEvent){
        val text = event.text
        when{
            modeValue equal "ZQAT-ArmorAndColor(BW)" -> {
                if (text != null && text.contains("游戏结束",true) && gameStarted && !text.contains("后",true)){
                    gameStarted = false
                    KevinClient.hud.addNotification(Notification("GameEnd!"),"Antibot")
                }
            }
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        when{
            modeValue equal "ZQAT-ArmorAndColor(BW)" -> {
                if (!gameStarted) return
                for (entity in mc.world.playerEntities){
                    if (entity == mc.player) continue
                    var isBot = false
                    val armorInventory = entity.inventory.armorInventory
                    if (!armorInventory[2].isEmpty
                        && !armorInventory[3].isEmpty
                        && armorInventory[2].item is ItemArmor
                        && armorInventory[3].item is ItemArmor
                    ){
                        if ((armorInventory[2].item as ItemArmor).armorMaterial != ItemArmor.ArmorMaterial.LEATHER) isBot = true
                        if ((armorInventory[3].item as ItemArmor).armorMaterial != ItemArmor.ArmorMaterial.LEATHER) isBot = true
                    }
                    if (!armorInventory[0].isEmpty
                        && !armorInventory[1].isEmpty
                        && armorInventory[0].item is ItemArmor
                        && armorInventory[1].item is ItemArmor
                    ){
                        if ((armorInventory[0].item as ItemArmor).armorMaterial != (armorInventory[1].item as ItemArmor).armorMaterial) isBot = true
                    }
                    if (((!armorInventory[0].isEmpty && armorInventory[0].item is ItemArmor) && armorInventory[1].isEmpty)
                        || ((!armorInventory[1].isEmpty && armorInventory[1].item is ItemArmor) && armorInventory[0].isEmpty)
                        || ((!armorInventory[2].isEmpty && armorInventory[2].item is ItemArmor) && armorInventory[3].isEmpty)
                        || ((!armorInventory[3].isEmpty && armorInventory[3].item is ItemArmor) && armorInventory[2].isEmpty)
                    ) isBot = true

                    for (a in armorInventory) {
                        if ((a != null
                                    && a.item is ItemArmor
                                    && ((a.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER
                                    && a.tagCompound == null
                                    ))
                        ) isBot = true
                    }

                    if (isBot) {
                        removeBot(entity)
                    }
                }
            }
            modeValue equal "NoColorArmor" -> {
                for (player in mc.world.playerEntities){
                    if (player == mc.player) continue
                    var isBot = false
                    val armorInventory = player.inventory.armorInventory
                    for (armor in armorInventory) {
                        if (armor == null || armor.item == null) continue
                        val itemArmor: ItemArmor
                        try {
                            itemArmor = armor.item as ItemArmor
                        } catch (e: Exception){
                            continue
                        }
                        if (itemArmor.armorMaterial == ItemArmor.ArmorMaterial.LEATHER){
                            if (!armor.hasTagCompound()) isBot = true
                        }
                    }
                    if (isBot) {
                        removeBot(player)
                    }
                }
            }
            modeValue equal "UnusualArmor" -> {
                val playerList = mc.world.playerEntities.toList()
                for (player in playerList) {
                    if (player == mc.player) continue
                    var isBot = false
                    val armorInventory = player.inventory.armorInventory
                    val boots = armorInventory[0]
                    val leggings = armorInventory[1]
                    val chestPlate = armorInventory[2]
                    val helmet = armorInventory[3]
                    if (
                        //NoArmor
                        (boots.isEmpty&&!allowNoBoots.get())
                        ||((leggings.isEmpty)&&!allowNoLeggings.get())
                        ||((chestPlate.isEmpty)&&!allowNoChestplate.get())
                        ||((helmet.isEmpty)&&!allowNoHelmet.get())
                        //Diamond
                        ||((!helmet.isEmpty&&helmet.item is ItemArmor)&&(helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.DIAMOND&&!allowDiamondHelmet.get())
                        ||((!chestPlate.isEmpty&&chestPlate.item is ItemArmor)&&(chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.DIAMOND&&!allowDiamondChestplate.get())
                        ||((!leggings.isEmpty&&leggings.item is ItemArmor)&&(leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.DIAMOND&&!allowDiamondLeggings.get())
                        ||((!boots.isEmpty&&boots.item is ItemArmor)&&(boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.DIAMOND&&!allowDiamondBoots.get())
                        //Golden
                        ||((!helmet.isEmpty&&helmet.item is ItemArmor)&&(helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.GOLD&&!allowGoldenHelmet.get())
                        ||((!chestPlate.isEmpty&&chestPlate.item is ItemArmor)&&(chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.GOLD&&!allowGoldenChestplate.get())
                        ||((!leggings.isEmpty&&leggings.item is ItemArmor)&&(leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.GOLD&&!allowGoldenLeggings.get())
                        ||((!boots.isEmpty&&boots.item is ItemArmor)&&(boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.GOLD&&!allowGoldenBoots.get())
                        //Iron
                        ||((!helmet.isEmpty&&helmet.item is ItemArmor)&&(helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.IRON&&!allowIronHelmet.get())
                        ||((!chestPlate.isEmpty&&chestPlate.item is ItemArmor)&&(chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.IRON&&!allowIronChestplate.get())
                        ||((!leggings.isEmpty&&leggings.item is ItemArmor)&&(leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.IRON&&!allowIronLeggings.get())
                        ||((!boots.isEmpty&&boots.item is ItemArmor)&&(boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.IRON&&!allowIronBoots.get())
                        //Chain
                        ||((!helmet.isEmpty&&helmet.item is ItemArmor)&&(helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.CHAIN&&!allowChainHelmet.get())
                        ||((!chestPlate.isEmpty&&chestPlate.item is ItemArmor)&&(chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.CHAIN&&!allowChainChestplate.get())
                        ||((!leggings.isEmpty&&leggings.item is ItemArmor)&&(leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.CHAIN&&!allowChainLeggings.get())
                        ||((!boots.isEmpty&&boots.item is ItemArmor)&&(boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.CHAIN&&!allowChainBoots.get())
                        //Leather
                        ||((!helmet.isEmpty&&helmet.item is ItemArmor)&&(helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER&&!allowLeatherHelmet.get())
                        ||((!chestPlate.isEmpty&&chestPlate.item is ItemArmor)&&(chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER&&!allowLeatherChestplate.get())
                        ||((!leggings.isEmpty&&leggings.item is ItemArmor)&&(leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER&&!allowLeatherLeggings.get())
                        ||((!boots.isEmpty&&boots.item is ItemArmor)&&(boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER&&!allowLeatherBoots.get())
                        //LeatherNoColor
                        ||((
                              ((!helmet.isEmpty&&helmet.item is ItemArmor)&&(helmet.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER&&!helmet.hasTagCompound())
                            ||((!chestPlate.isEmpty&&chestPlate.item is ItemArmor)&&(chestPlate.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER&&!chestPlate.hasTagCompound())
                            ||((!leggings.isEmpty&&leggings.item is ItemArmor)&&(leggings.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER&&!leggings.hasTagCompound())
                            ||((!boots.isEmpty&&boots.item is ItemArmor)&&(boots.item as ItemArmor).armorMaterial == ItemArmor.ArmorMaterial.LEATHER&&!boots.hasTagCompound())
                                )&&removeNoColorLeatherArmor.get())
                    )isBot = true
                    if (isBot) {
                        removeBot(player)
                    }
                }
            }
        }
    }
    private fun removeBot(bot: Entity){
        mc.world.removeEntityFromWorld(bot.entityId)
        KevinClient.hud.addNotification(Notification("Removed Bot"),"AntiBot")
    }
}