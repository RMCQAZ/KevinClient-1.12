package kevin.module.modules.player

import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.ListValue
import kevin.module.Module
import kevin.module.ModuleCategory
import net.minecraft.client.multiplayer.PlayerControllerMP
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.ContainerPlayer
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemStack
import net.minecraft.util.NonNullList


class AutoTotem : Module("AutoTotem","Automatically places totem in off hand.", category = ModuleCategory.PLAYER) {

    private val noReplace = BooleanValue("NoReplace", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if (noReplace.get()&&!mc.player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND).isEmpty) return

        val itemStackFromSlot2: ItemStack = mc.player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND)
        val mainInventory: NonNullList<*> = mc.player.inventory.mainInventory
        var k = 0
        while (k < mainInventory.size) {
            if (mainInventory[k] !== ItemStack.EMPTY && (itemStackFromSlot2.isEmpty || itemStackFromSlot2.item !== Items.TOTEM_OF_UNDYING) && (mainInventory[k] as ItemStack).item === Items.TOTEM_OF_UNDYING) {
                this.wCGE(k)
                break
            } else {
                ++k
            }
            continue
        }
    }
    private fun wCGE(n: Int) {
        if (mc.player.openContainer is ContainerPlayer) {
            val playerController: PlayerControllerMP = mc.playerController
            val n2 = 0
            val n3 = if (n < 9) {
                n + 36
            } else {
                n
            }
            playerController.windowClick(n2, n3, 0, ClickType.PICKUP, mc.player as EntityPlayer)
            mc.playerController.windowClick(0, 45, 0, ClickType.PICKUP, mc.player as EntityPlayer)
            val playerController2: PlayerControllerMP = mc.playerController
            val n4 = 0
            val n5 = if (n < 9) {
                n + 36
            } else {
                n
            }
            playerController2.windowClick(n4, n5, 0, ClickType.PICKUP, mc.player as EntityPlayer)
        }
    }
}

