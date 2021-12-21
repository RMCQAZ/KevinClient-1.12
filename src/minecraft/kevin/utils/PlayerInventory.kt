package kevin.utils

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketHeldItemChange
import java.util.function.Predicate

val Slot.hotbarSlot: Int
    get() = this.slotNumber - 36

val EntityPlayer.hotbarSlots: List<Slot>
    get() = ArrayList<Slot>().apply {
            for (i in 36..44) {
                add(inventoryContainer.inventorySlots[i])
            }
        }

fun <T : Slot> Iterable<T>.firstItem(item: Item, predicate: Predicate<ItemStack>? = null) =
    firstByStack {
        it.item == item && (predicate == null || predicate.test(it))
    }

fun <T : Slot> Iterable<T>.firstBlock(block: Block, predicate: Predicate<ItemStack>? = null) =
    firstByStack { itemStack ->
        itemStack.item.let { it is ItemBlock && it.block == block } && (predicate == null || predicate.test(itemStack))
    }

fun <T : Slot> Iterable<T>.firstByStack(predicate: Predicate<ItemStack>? = null): T? =
    firstOrNull {
        (predicate == null || predicate.test(it.stack))
    }

fun switchHotbar(slot: Int, block: () -> Unit){
    val mc = Minecraft.getMinecraft()
    val oldSlot = mc.player.inventory.currentItem
    mc.player.inventory.currentItem = slot
    mc.player.connection.sendPacket(CPacketHeldItemChange(slot))
    block.invoke()
    mc.player.inventory.currentItem = oldSlot
    mc.player.connection.sendPacket(CPacketHeldItemChange(oldSlot))
}