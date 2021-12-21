package kevin.module.modules.combat

import kevin.KevinClient
import kevin.event.AttackEvent
import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.IntegerValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.ItemUtils
import net.minecraft.init.Enchantments
import net.minecraft.inventory.EntityEquipmentSlot
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketUseEntity

class AutoWeapon : Module("AutoWeapon", "Automatically selects the best weapon in your hotbar.", category = ModuleCategory.COMBAT) {
    private val silentValue = BooleanValue("SpoofItem", false)
    private val ticksValue = IntegerValue("SpoofTicks", 10, 1, 20)
    private val swordsFirst = BooleanValue("SwordsFirst",false)
    private var attackEnemy = false

    private var spoofedSlot = 0

    @EventTarget
    fun onAttack(event: AttackEvent) {
        attackEnemy = true
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (event.packet !is CPacketUseEntity || KevinClient.moduleManager.getModule("CrystalAura")!!.state)
            return

        val thePlayer = mc.player ?: return

        val packet = event.packet

        if (packet.action == CPacketUseEntity.Action.ATTACK
            && attackEnemy) {
            attackEnemy = false

            // Find best weapon in hotbar (#Kotlin Style)
            var (slot, _) = (0..8)
                .map { Pair(it, thePlayer.inventory.getStackInSlot(it)) }
                .filter { !it.second.isEmpty && (it.second.item is ItemSword || it.second.item is ItemTool) }
                .maxByOrNull {
                    it.second.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)["generic.attackDamage"].first().amount + 1.25 * ItemUtils.getEnchantment(
                        it.second,
                        Enchantments.SHARPNESS
                    )
                } ?: return

            if (swordsFirst.get()){
                val bestSword = (0..8)
                    .map { Pair(it, thePlayer.inventory.getStackInSlot(it)) }
                    .filter { !it.second.isEmpty && it.second.item is ItemSword }
                    .maxByOrNull {
                        it.second.getAttributeModifiers(EntityEquipmentSlot.MAINHAND)["generic.attackDamage"].first().amount + 1.25 * ItemUtils.getEnchantment(
                            it.second,
                            Enchantments.SHARPNESS
                        )
                    }
                if (bestSword!=null){
                    slot = bestSword.first
                }
            }

            if (slot == thePlayer.inventory.currentItem) // If in hand no need to swap
                return

            // Switch to best weapon
            if (silentValue.get()) {
                mc.connection!!.sendPacket(CPacketHeldItemChange(slot))
                spoofedSlot = ticksValue.get()
            } else {
                thePlayer.inventory.currentItem = slot
                mc.playerController.updateController()
            }

            // Resend attack packet
            mc.connection!!.sendPacket(packet)
            event.cancelEvent()
        }
    }

    @EventTarget
    fun onUpdate(update: UpdateEvent) {
        // Switch back to old item after some time
        if (spoofedSlot > 0) {
            if (spoofedSlot == 1)
                mc.connection!!.sendPacket(CPacketHeldItemChange(mc.player!!.inventory.currentItem))
            spoofedSlot--
        }
    }
}