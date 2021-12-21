package kevin.module.modules.player

import kevin.KevinClient
import kevin.event.*
import kevin.module.*
import kevin.module.modules.combat.AutoArmor
import kevin.utils.*
import net.minecraft.block.BlockBush
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.enchantment.Enchantment
import net.minecraft.init.Blocks
import net.minecraft.init.Enchantments
import net.minecraft.init.Items
import net.minecraft.inventory.ClickType
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.*
import net.minecraft.network.play.client.CPacketClientStatus
import net.minecraft.network.play.client.CPacketCloseWindow
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock

class InventoryCleaner : Module(name = "InventoryCleaner", description = "Automatically throws away useless items.", category = ModuleCategory.PLAYER) {
    private val maxDelayValue: IntegerValue = object : IntegerValue("MaxDelay", 600, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val minCPS = minDelayValue.get()
            if (minCPS > newValue) set(minCPS)
        }
    }

    private val minDelayValue: IntegerValue = object : IntegerValue("MinDelay", 400, 0, 1000) {
        override fun onChanged(oldValue: Int, newValue: Int) {
            val maxDelay = maxDelayValue.get()
            if (maxDelay < newValue) set(maxDelay)
        }
    }

    private val invOpenValue = BooleanValue("InvOpen", false)
    private val simulateInventory = BooleanValue("SimulateInventory", true)
    private val noMoveValue = BooleanValue("NoMove", false)
    private val ignoreVehiclesValue = BooleanValue("IgnoreVehicles", false)
    private val hotbarValue = BooleanValue("Hotbar", true)
    private val randomSlotValue = BooleanValue("RandomSlot", false)
    private val sortValue = BooleanValue("Sort", true)
    private val keepDamagedElytras = BooleanValue("keepDamagedElytras",true)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)
    private val expValue = BooleanValue("EXPB", true)
    private val keepAllArmors = BooleanValue("KeepAllArmors", false)

    private val maxBlocks = IntegerValue("MaxBlocks",4,1,36)
    private val maxBuckets = IntegerValue("MaxBuckets",2,0,36)
    private val maxThrowableItems = IntegerValue("MaxThrowableItems",0,0,36)

    private val items = arrayOf("None", "Ignore", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water", "Gapple", "Pearl", "Throwable","Crystal","EXP")
    private val sortSlot1Value = ListValue("SortSlot-1", items, "Sword")
    private val sortSlot2Value = ListValue("SortSlot-2", items, "Bow")
    private val sortSlot3Value = ListValue("SortSlot-3", items, "Pickaxe")
    private val sortSlot4Value = ListValue("SortSlot-4", items, "Axe")
    private val sortSlot5Value = ListValue("SortSlot-5", items, "None")
    private val sortSlot6Value = ListValue("SortSlot-6", items, "None")
    private val sortSlot7Value = ListValue("SortSlot-7", items, "Food")
    private val sortSlot8Value = ListValue("SortSlot-8", items, "Block")
    private val sortSlot9Value = ListValue("SortSlot-9", items, "Block")


    private var delay = 0L

    @EventTarget
    fun onUpdate(event: UpdateEvent) {

        //if (event.eventState == UpdateState.OnUpdate) return

        val thePlayer = mc.player ?: return

        if (!InventoryUtils.CLICK_TIMER.hasTimePassed(delay) ||
             mc.currentScreen !is GuiInventory && invOpenValue.get() ||
            noMoveValue.get() && MovementUtils.isMoving ||
            thePlayer.openContainer != null && thePlayer.openContainer!!.windowId != 0
            || (KevinClient.moduleManager.getModule("AutoArmor") as AutoArmor).isLocked)
            return

        if (sortValue.get())
            sortHotbar()

        while (InventoryUtils.CLICK_TIMER.hasTimePassed(delay)) {

            val garbageItems = items(9, if (hotbarValue.get()) 45 else 36)
                .filter {
                    !isUseful(it.value, it.key)
                            ||(it.value.item is ItemBlock&&blocks > maxBlocks.get())
                            ||(it.value.item is ItemBucket&&buckets > maxBuckets.get())
                }
                .keys
                .toMutableList()

            // Shuffle items
            if (randomSlotValue.get())
                garbageItems.shuffle()

            val garbageItem = garbageItems.firstOrNull() ?: break

            // Drop all useless items
            val openInventory = mc.currentScreen !is GuiInventory && simulateInventory.get()

            if (openInventory)
                mc.connection!!.sendPacket(CPacketEntityAction(mc.player,CPacketEntityAction.Action.OPEN_INVENTORY))

            mc.playerController.windowClick(thePlayer.openContainer!!.windowId, garbageItem, 1, ClickType.THROW, thePlayer)

            if (openInventory)
                mc.connection!!.sendPacket(CPacketCloseWindow())

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
        }
    }

    //blocks
    private val blocks: Int
    get() {
        var blocks = 0
        mc.player.inventory.mainInventory
            .filter { !it.isEmpty&&it.item is ItemBlock }
            .forEach { _ -> blocks+=1 }
        return blocks
    }
    //buckets
    private val buckets: Int
    get() {
        var buckets = 0
        mc.player.inventory.mainInventory
            .filter { !it.isEmpty&&it.item is ItemBucket }
            .forEach { _ -> buckets+=1 }
        return buckets
    }
    //Throwable items
    private val throwables: Int
    get() {
        var throwables = 0
        mc.player.inventory.mainInventory
            .filter { !it.isEmpty&&(it.item is ItemSnowball||it.item is ItemEgg) }
            .forEach { _ -> throwables+=1 }
        return throwables
    }

    fun isUseful(itemStack: ItemStack, slot: Int): Boolean {
        return try {
            val item = itemStack.item

            //if (item is ItemBoat) return true

            if (item is ItemSword || item is ItemTool) {
                val thePlayer = mc.player ?: return true

                if (slot >= 36 && findBetterItem(slot - 36, thePlayer.inventory.getStackInSlot(slot - 36)) == slot - 36)
                    return true

                for (i in 0..8) {
                    if (type(i).equals("sword", true) && item is ItemSword
                        || type(i).equals("pickaxe", true) && item is ItemPickaxe
                        || type(i).equals("axe", true) && item is ItemAxe) {
                        if (findBetterItem(i, thePlayer.inventory.getStackInSlot(i)) == null) {
                            return true
                        }
                    }
                }

                val damage = (itemStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)["generic.attackDamage"].firstOrNull()?.amount
                    ?: 0.0) + 1.25 * ItemUtils.getEnchantment(itemStack, Enchantments.SHARPNESS)

                items(0, 45).none { (_, stack) ->
                    stack != itemStack && stack.javaClass == itemStack.javaClass
                            && damage < (stack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)["generic.attackDamage"].firstOrNull()?.amount
                        ?: 0.0) + 1.25 * ItemUtils.getEnchantment(stack, Enchantments.SHARPNESS)
                }
            } else if (item is ItemBow) {
                val currPower = ItemUtils.getEnchantment(itemStack, Enchantments.POWER)

                items().none { (_, stack) ->
                    itemStack != stack && stack.item is ItemBow &&
                            currPower < ItemUtils.getEnchantment(stack, Enchantments.POWER)
                }
            } else if (item is ItemArmor) {
                if (keepAllArmors.get()) return true

                val currArmor = ArmorPiece(itemStack, slot)

                items().none { (slot, stack) ->
                    if (stack != itemStack && stack.item is ItemArmor) {
                        val armor = ArmorPiece(stack, slot)

                        if (armor.armorType != currArmor.armorType)
                            false
                        else
                            AutoArmor.ARMOR_COMPARATOR.compare(currArmor, armor) <= 0
                    } else
                        false
                }
            } else if (itemStack.unlocalizedName == "item.compass") {
                items(0, 45).none { (_, stack) -> itemStack != stack && stack.unlocalizedName == "item.compass" }
            } else item is ItemFood || itemStack.unlocalizedName == "item.arrow" || item == Items.TOTEM_OF_UNDYING || (item is ItemElytra &&(ItemElytra.isUsable(itemStack)||keepDamagedElytras.get())) ||
                    ((item is ItemBlock && item.block !is BlockBush)&&(blocks<=maxBlocks.get()||!this.state)) || item is ItemEndCrystal || item is ItemShulkerBox ||
                    item is ItemBed || itemStack.unlocalizedName == "item.diamond" || itemStack.unlocalizedName == "item.ingotIron" || item is ItemSpectralArrow || item is ItemTippedArrow ||
                    item is ItemPotion || item is ItemEnderPearl || item is ItemEnchantedBook || (item is ItemBucket&&(buckets<=maxBuckets.get()||!this.state)) || itemStack.unlocalizedName == "item.stick" ||
                    ignoreVehiclesValue.get() && (item is ItemBoat || item is ItemMinecart) || ((item is ItemSnowball||item is ItemEgg)&&(throwables<=maxThrowableItems.get()||!this.state)) ||
                    (expValue.get() && item is ItemExpBottle)
        } catch (ex: Exception) {
            true
        }
    }

    private fun sortHotbar() {
        for (index in 0..8) {
            val thePlayer = mc.player ?: return

            val bestItem = findBetterItem(index, thePlayer.inventory.getStackInSlot(index)) ?: continue

            if (bestItem != index) {
                val openInventory = mc.currentScreen !is GuiInventory && simulateInventory.get()

                if (openInventory)
                    mc.connection!!.sendPacket(CPacketEntityAction(mc.player,CPacketEntityAction.Action.OPEN_INVENTORY))

                mc.playerController.windowClick(0, if (bestItem < 9) bestItem + 36 else bestItem, index,
                    ClickType.SWAP, thePlayer)

                if (openInventory)
                    mc.connection!!.sendPacket(CPacketCloseWindow())

                delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
                break
            }
        }
    }

    private fun findBetterItem(targetSlot: Int, slotStack: ItemStack?): Int? {
        val type = type(targetSlot)

        val thePlayer = mc.player ?: return null

        when (type.toLowerCase()) {
            "sword", "pickaxe", "axe" -> {
                val currentTypeChecker: ((Item?) -> Boolean) = when {
                    type.equals("Sword", ignoreCase = true) -> { item: Item? -> item is ItemSword }
                    type.equals("Pickaxe", ignoreCase = true) -> { obj: Item? -> obj is ItemPickaxe }
                    type.equals("Axe", ignoreCase = true) -> { obj: Item? -> obj is ItemAxe }
                    else -> return null
                }

                var bestWeapon = if (currentTypeChecker(slotStack?.item))
                    targetSlot
                else -1

                thePlayer.inventory.mainInventory.forEachIndexed { index, itemStack ->
                    if (!itemStack.isEmpty && currentTypeChecker(itemStack.item) && !type(index).equals(type, ignoreCase = true)) {
                        if (bestWeapon == -1) {
                            bestWeapon = index
                        } else {
                            val currDamage = (itemStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)["generic.attackDamage"].firstOrNull()?.amount
                                ?: 0.0) + 1.25 * ItemUtils.getEnchantment(itemStack, Enchantments.SHARPNESS)

                            val bestStack = thePlayer.inventory.getStackInSlot(bestWeapon)
                                ?: return@forEachIndexed
                            val bestDamage = (bestStack.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)["generic.attackDamage"].firstOrNull()?.amount
                                ?: 0.0) + 1.25 * ItemUtils.getEnchantment(bestStack, Enchantments.SHARPNESS)

                            if (bestDamage < currDamage)
                                bestWeapon = index
                        }
                    }
                }

                return if (bestWeapon != -1 || bestWeapon == targetSlot) bestWeapon else null
            }

            "bow" -> {
                var bestBow = if (slotStack?.item is ItemBow) targetSlot else -1
                var bestPower = if (bestBow != -1)
                    ItemUtils.getEnchantment(slotStack, Enchantments.POWER)
                else
                    0

                thePlayer.inventory.mainInventory.forEachIndexed { index, itemStack ->
                    if (itemStack?.item is ItemBow && !type(index).equals(type, ignoreCase = true)) {
                        if (bestBow == -1) {
                            bestBow = index
                        } else {
                            val power = ItemUtils.getEnchantment(itemStack, Enchantments.POWER)

                            if (ItemUtils.getEnchantment(itemStack, Enchantments.POWER) > bestPower) {
                                bestBow = index
                                bestPower = power
                            }
                        }
                    }
                }

                return if (bestBow != -1) bestBow else null
            }

            "food" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (!stack.isEmpty) {
                        val item = stack.item

                        if (item is ItemFood && item !is ItemAppleGold && !type(index).equals("Food", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack)

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "block" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (!stack.isEmpty) {
                        val item = stack.item

                        if (item is ItemBlock && !InventoryUtils.BLOCK_BLACKLIST.contains(item.block) &&
                            !type(index).equals("Block", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack)

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "exp" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (!stack.isEmpty) {
                        val item = stack.item

                        if ((item is ItemExpBottle) && !type(index).equals("Exp", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack)

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "crystal" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (!stack.isEmpty) {
                        val item = stack.item

                        if ((item is ItemEndCrystal) && !type(index).equals("Crystal", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack)

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "throwable" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (!stack.isEmpty) {
                        val item = stack.item

                        if ((item is ItemSnowball||item is ItemEgg) && !type(index).equals("Throwable", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack)

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "water" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (!stack.isEmpty) {
                        val item = stack.item

                        if (item is ItemBucket && item.containedBlock == Blocks.FLOWING_WATER && !type(index).equals("Water", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack) || item.containedBlock != Blocks.FLOWING_WATER

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "gapple" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (!stack.isEmpty) {
                        val item = stack.item

                        if (item is ItemAppleGold && !type(index).equals("Gapple", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack) || slotStack?.item !is ItemAppleGold

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }

            "pearl" -> {
                thePlayer.inventory.mainInventory.forEachIndexed { index, stack ->
                    if (!stack.isEmpty) {
                        val item = stack.item

                        if (item is ItemEnderPearl && !type(index).equals("Pearl", ignoreCase = true)) {
                            val replaceCurr = ItemUtils.isStackEmpty(slotStack) || slotStack?.item !is ItemEnderPearl

                            return if (replaceCurr) index else null
                        }
                    }
                }
            }
        }

        return null
    }

    private fun items(start: Int = 0, end: Int = 45): Map<Int, ItemStack> {
        val items = mutableMapOf<Int, ItemStack>()

        for (i in end - 1 downTo start) {
            val itemStack = mc.player?.inventoryContainer?.getSlot(i)?.stack ?: continue

            if (ItemUtils.isStackEmpty(itemStack))
                continue

            if (i in 36..44 && type(i).equals("Ignore", ignoreCase = true))
                continue

            if (System.currentTimeMillis() - (itemStack).itemDelay >= itemDelayValue.get())
                items[i] = itemStack
        }

        return items
    }

    private fun type(targetSlot: Int) = when (targetSlot) {
        0 -> sortSlot1Value.get()
        1 -> sortSlot2Value.get()
        2 -> sortSlot3Value.get()
        3 -> sortSlot4Value.get()
        4 -> sortSlot5Value.get()
        5 -> sortSlot6Value.get()
        6 -> sortSlot7Value.get()
        7 -> sortSlot8Value.get()
        8 -> sortSlot9Value.get()
        else -> ""
    }

    @EventTarget(ignoreCondition = true)
    fun onClick(event: ClickWindowEvent?) {
        InventoryUtils.CLICK_TIMER.reset()
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is CPacketPlayerTryUseItemOnBlock) InventoryUtils.CLICK_TIMER.reset()
    }
}