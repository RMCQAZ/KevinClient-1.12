package kevin.module.modules.movement

import kevin.KevinClient
import kevin.event.EventState
import kevin.event.EventTarget
import kevin.event.MotionEvent
import kevin.event.SlowDownEvent
import kevin.module.*
import kevin.module.modules.combat.KillAura
import kevin.utils.MovementUtils
import net.minecraft.item.*
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos

class NoSlow : Module("NoSlow", "Cancels slowness effects caused by soulsand and using items.", category = ModuleCategory.MOVEMENT) {

    private val blockForwardMultiplier = FloatValue("BlockForwardMultiplier", 1.0F, 0.2F, 1.0F)
    private val blockStrafeMultiplier = FloatValue("BlockStrafeMultiplier", 1.0F, 0.2F, 1.0F)

    private val consumeForwardMultiplier = FloatValue("ConsumeForwardMultiplier", 1.0F, 0.2F, 1.0F)
    private val consumeStrafeMultiplier = FloatValue("ConsumeStrafeMultiplier", 1.0F, 0.2F, 1.0F)

    private val bowForwardMultiplier = FloatValue("BowForwardMultiplier", 1.0F, 0.2F, 1.0F)
    private val bowStrafeMultiplier = FloatValue("BowStrafeMultiplier", 1.0F, 0.2F, 1.0F)

    private val packetMode = ListValue("PacketMode", arrayOf("None","AntiCheat","AAC5"),"None")

    val soulsandValue = BooleanValue("Soulsand", true)
    val liquidPushValue = BooleanValue("LiquidPush", true)

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.player ?: return
        val heldItem = thePlayer.heldItemMainhand ?: return

        if ((heldItem.item) !is ItemSword || !MovementUtils.isMoving)
            return

        val aura = KevinClient.moduleManager.getModule("KillAura") as KillAura
        //if (!thePlayer.isBlocking && !aura.blockingStatus)
            return

        when(packetMode.get()){
            "AntiCheat" -> {
                when (event.eventState) {
                    EventState.PRE -> {
                        val digging = CPacketPlayerDigging(CPacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos(0, 0, 0), EnumFacing.DOWN)
                        mc.connection!!.sendPacket(digging)
                    }
                    EventState.POST -> {
                        val blockPlace = CPacketPlayerTryUseItemOnBlock(BlockPos(-1, -1, -1), EnumFacing.DOWN, EnumHand.MAIN_HAND, 0.0F, 0.0F, 0.0F)
                        mc.connection!!.sendPacket(blockPlace)
                    }
                }
            }
            "AAC5" -> {
                if (event.eventState == EventState.POST && (mc.player.isHandActive || /*mc.player.isBlocking ||*/ aura.blockingStatus)) {
                    mc.connection!!.sendPacket(CPacketPlayerTryUseItemOnBlock(BlockPos(-1, -1, -1), EnumFacing.DOWN, EnumHand.MAIN_HAND, 0f, 0f, 0f))
                }
            }
        }
    }

    override val tag: String
        get() = packetMode.get()

    @EventTarget
    fun onSlowDown(event: SlowDownEvent) {
        val heldItem = if(mc.player!!.heldItemMainhand.isEmpty) mc.player!!.heldItemOffhand.item else mc.player!!.heldItemMainhand.item

        event.forward = getMultiplier(heldItem, true)
        event.strafe = getMultiplier(heldItem, false)
    }

    private fun getMultiplier(item: Item?, isForward: Boolean): Float {
        return when {
            (item)is ItemFood || (item)is ItemPotion || (item)is ItemBucketMilk -> {
                if (isForward) this.consumeForwardMultiplier.get() else this.consumeStrafeMultiplier.get()
            }
            (item)is ItemSword || (item)is ItemShield -> {
                if (isForward) this.blockForwardMultiplier.get() else this.blockStrafeMultiplier.get()
            }
            (item)is ItemBow -> {
                if (isForward) this.bowForwardMultiplier.get() else this.bowStrafeMultiplier.get()
            }
            else -> 0.2F
        }
    }
}