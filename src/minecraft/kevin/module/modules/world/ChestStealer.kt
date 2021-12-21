package kevin.module.modules.world

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.Render3DEvent
import kevin.module.BooleanValue
import kevin.module.IntegerValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.module.modules.player.InventoryCleaner
import kevin.utils.ItemUtils
import kevin.utils.TimeUtils
import kevin.utils.timers.MSTimer
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.Slot
import net.minecraft.item.Item
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.SPacketWindowItems
import net.minecraft.util.ResourceLocation
import kotlin.random.Random

class ChestStealer : Module("ChestStealer", description = "Automatically steals all items from a chest.", category = ModuleCategory.WORLD) {

    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 200, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = minDelayValue.get()
            if (i > newValue)
                set(i)

            nextDelay = TimeUtils.randomDelay(minDelayValue.get(), get())
        }
    }

    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 150, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = maxDelayValue.get()

            if (i < newValue)
                set(i)

            nextDelay = TimeUtils.randomDelay(get(), maxDelayValue.get())
        }
    }
    private val delayOnFirstValue = BooleanValue("DelayOnFirst", false)

    private val takeRandomizedValue = BooleanValue("TakeRandomized", false)
    private val onlyItemsValue = BooleanValue("OnlyItems", false)
    private val noCompassValue = BooleanValue("NoCompass", false)
    private val autoCloseValue = BooleanValue("AutoClose", true)

    private val autoCloseMaxDelayValue: IntegerValue = object : IntegerValue("AutoCloseMaxDelay", 0, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = autoCloseMinDelayValue.get()
            if (i > newValue) set(i)
            nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), this.get())
        }
    }

    private val autoCloseMinDelayValue: IntegerValue = object : IntegerValue("AutoCloseMinDelay", 0, 0, 400) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val i = autoCloseMaxDelayValue.get()
            if (i < newValue) set(i)
            nextCloseDelay = TimeUtils.randomDelay(this.get(), autoCloseMaxDelayValue.get())
        }
    }

    private val closeOnFullValue = BooleanValue("CloseOnFull", true)
    private val chestTitleValue = BooleanValue("ChestTitle", false)

    var chestItems = mutableListOf<ItemStack?>()

    /**
     * VALUES
     */

    private val delayTimer = MSTimer()
    private var nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())

    private val autoCloseTimer = MSTimer()
    private var nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get())

    private var contentReceived = 0

    val silentValue = BooleanValue("Silent", true)
    val overrideShowInvValue = BooleanValue("OverrideShowInv", true)

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        val thePlayer = mc.player!!

        if (mc.currentScreen !is GuiChest || mc.currentScreen == null) {
            if (delayOnFirstValue.get())
                delayTimer.reset()
            autoCloseTimer.reset()
            return
        }

        if (!delayTimer.hasTimePassed(nextDelay)) {
            autoCloseTimer.reset()
            return
        }

        val screen = mc.currentScreen!! as GuiChest

        // No Compass
        if (noCompassValue.get() && thePlayer.inventory.getCurrentItem()?.item?.unlocalizedName == "item.compass")
            return

        // Chest title
        if (chestTitleValue.get() && (screen.lowerChestInventory == null || !screen.lowerChestInventory.name.contains(ItemStack(
                Item.REGISTRY.getObject(ResourceLocation("minecraft:chest"))!!).displayName)))
            return

        // inventory cleaner
        val inventoryCleaner = KevinClient.moduleManager.getModule("InventoryCleaner") as InventoryCleaner

        // Is empty?
        if (!isEmpty(screen) && (!closeOnFullValue.get() || !fullInventory)) {
            autoCloseTimer.reset()

            chestItems.clear()

            for (slotIndex in 0 until screen.inventoryRows * 9){
                val slot = screen.inventorySlots!!.getSlot(slotIndex)
                val stack = slot.stack
                chestItems.add(stack)
            }

            // Randomized
            if (takeRandomizedValue.get()) {
                do {
                    val items = mutableListOf<Slot>()

                    for (slotIndex in 0 until screen.inventoryRows * 9) {
                        val slot = screen.inventorySlots!!.getSlot(slotIndex)

                        val stack = slot.stack

                        if (!stack.isEmpty && (!onlyItemsValue.get() || stack.item !is ItemBlock) && (!inventoryCleaner.state || inventoryCleaner.isUseful(stack, -1)))
                            items.add(slot)
                    }

                    val randomSlot = Random.nextInt(items.size)
                    val slot = items[randomSlot]

                    move(screen, slot)
                } while (delayTimer.hasTimePassed(nextDelay) && items.isNotEmpty())
                return
            }

            // Non randomized
            for (slotIndex in 0 until screen.inventoryRows * 9) {
                val slot = screen.inventorySlots!!.getSlot(slotIndex)

                val stack = slot.stack

                if (delayTimer.hasTimePassed(nextDelay) && shouldTake(stack, inventoryCleaner)) {
                    move(screen, slot)
                }
            }
        } else if (autoCloseValue.get() && screen.inventorySlots!!.windowId == contentReceived && autoCloseTimer.hasTimePassed(nextCloseDelay)) {
            thePlayer.closeScreen()
            nextCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get())
        }
    }

    @EventTarget
    private fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is SPacketWindowItems) {
            contentReceived = (packet).windowId
        }
    }

    private inline fun shouldTake(stack: ItemStack?, inventoryCleaner: InventoryCleaner): Boolean {
        return stack != null && !ItemUtils.isStackEmpty(stack) && (!onlyItemsValue.get() || stack.item !is ItemBlock) && (!inventoryCleaner.state || inventoryCleaner.isUseful(stack, -1))
    }

    private fun move(screen: GuiChest, slot: Slot) {
        mc.playerController.windowClick(screen.inventorySlots.windowId,slot.slotNumber,0,ClickType.QUICK_MOVE,mc.player)
        delayTimer.reset()
        nextDelay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
    }

    private fun isEmpty(chest: GuiChest): Boolean {
        val inventoryCleaner = KevinClient.moduleManager.getModule("InventoryCleaner") as InventoryCleaner

        for (i in 0 until chest.inventoryRows * 9) {
            val slot = chest.inventorySlots!!.getSlot(i)

            val stack = slot.stack

            if (shouldTake(stack, inventoryCleaner))
                return false
        }

        return true
    }

    private val fullInventory: Boolean
        get() = mc.player?.inventory?.mainInventory?.none(ItemUtils::isStackEmpty) ?: false
}