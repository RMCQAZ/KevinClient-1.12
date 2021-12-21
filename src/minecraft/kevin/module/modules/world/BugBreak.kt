package kevin.module.modules.world

import kevin.event.*
import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.BlockUtils
import kevin.utils.RenderUtils
import kevin.utils.RotationUtils
import kevin.utils.switchHotbar
import net.minecraft.block.BlockAir
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketAnimation
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import java.awt.Color

class BugBreak : Module("BugBreak","Break block with packets", category = ModuleCategory.WORLD) {

    private var blockPos: BlockPos? = null
    private var enumFacing: EnumFacing? = null
    private var isSpoofing = false
    private var slot = -1

    private val range = FloatValue("Range", 5F, 1F, 80F)
    private val rotationsValue = BooleanValue("Rotations", true)
    private val visualSwingValue = BooleanValue("VisualSwing", true)
    private val airResetValue = BooleanValue("Air-Reset", true)
    private val rangeResetValue = BooleanValue("Range-Reset", true)
    private val noClick = BooleanValue("No-Click", true)
    private val switchItem = BooleanValue("SwitchItem", true)

    @EventTarget
    fun onBlockClick(event: ClickBlockEvent) {
        if (event.clickedBlock?.let { BlockUtils.getBlock(it) } == Blocks.BEDROCK)
            return

        blockPos = event.clickedBlock ?: return
        enumFacing = event.WEnumFacing ?: return

        doBreak()
    }

    override fun onDisable() {
        reset()
        blockPos = null
        enumFacing = null
    }

    @EventTarget
    fun onUpdate(event: MotionEvent) {
        if (event.eventState == EventState.PRE) reset()

        val pos = blockPos ?: return

        if (airResetValue.get() && BlockUtils.getBlock(pos) is BlockAir ||
            rangeResetValue.get() && BlockUtils.getCenterDistance(pos) > range.get()) {
            blockPos = null
            return
        }

        if (BlockUtils.getBlock(pos) is BlockAir || BlockUtils.getCenterDistance(pos) > range.get())
            return

        when (event.eventState) {
            EventState.PRE -> if (rotationsValue.get())
                    RotationUtils.setTargetRotation((RotationUtils.faceBlock(pos) ?: return).rotation)


            EventState.POST -> {
                if (visualSwingValue.get())
                    mc.player!!.swingArm(EnumHand.MAIN_HAND)
                else
                    mc.connection!!.sendPacket(CPacketAnimation(EnumHand.MAIN_HAND))

                doBreak()

                if (!noClick.get()) mc.playerController.clickBlock(blockPos!!, enumFacing!!)
            }
        }
    }

    private fun reset(){
        if (isSpoofing) {
            isSpoofing = false
            mc.player.connection.sendPacket(CPacketHeldItemChange(slot))
        }
    }

    private fun doBreak() {
        if (switchItem.get()) {
            var bestSpeed = 1F
            var bestSlot = -1
            val blockState = mc.world!!.getBlockState(blockPos!!)
            for (i in 0..8) {
                val item = mc.player!!.inventory.getStackInSlot(i) ?: continue
                val speed = item.getStrVsBlock(blockState)
                if (speed > bestSpeed) {
                    bestSpeed = speed
                    bestSlot = i
                }
            }
            if (bestSlot != -1 && bestSlot != mc.player.inventory.currentItem && !isSpoofing) {
                isSpoofing = true
                slot = mc.player.inventory.currentItem
                mc.player.connection.sendPacket(CPacketHeldItemChange(bestSlot))
            }
        }
        breakPacket()
    }

    private fun breakPacket() =
        mc.connection!!.sendPacket(
            CPacketPlayerDigging(
                CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                blockPos!!,
                enumFacing!!
            )
        )

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        RenderUtils.drawBlockBox(blockPos ?: return, Color.RED, true)
    }
}