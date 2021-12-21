package kevin.module.modules.combat

import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.*
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketEntityAction
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class Burrow : Module("Burrow", "Put a obsidian on your body.", category = ModuleCategory.COMBAT) {
    private val bp = arrayOf(
        BlockPos(0,-1,0) to EnumFacing.UP,
        BlockPos(-1,0,0) to EnumFacing.EAST,
        BlockPos(0,0,-1) to EnumFacing.SOUTH,
        BlockPos(0,0,1) to EnumFacing.NORTH,
        BlockPos(1,0,0) to EnumFacing.WEST,
        BlockPos(0,1,0) to EnumFacing.DOWN,
    )
    override fun onEnable() {
        val blockPos = BlockPos(mc.player.posX.fastFloor(), mc.player.posY.fastFloor(), mc.player.posZ.fastFloor())
        for (b in bp){
            val p = blockPos.add(b.first)
            val obsidianSlot = mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
            if (obsidianSlot != null) {
                if (!mc.world.getBlockState(p).material.isReplaceable){
                    switchHotbar(obsidianSlot.hotbarSlot) {
                        val facing = b.second
                        val dirVec = Vec3d(facing.directionVec)
                        val posVec = Vec3d(blockPos).addVector(0.5, 0.5, 0.5)
                        val hitVec = posVec.add(Vec3d(dirVec.x * 0.5, dirVec.y * 0.5, dirVec.z * 0.5))
                        doUpPlace(p, facing, hitVec)
                    }
                    break
                }
            } else {
                ChatUtils.messageWithPrefix("§cNo obsidian in your hotbar!")
                break
            }
        }
        state = false
    }
    private fun doUpPlace(p: BlockPos, facing: EnumFacing, hitVec: Vec3d) {
        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.41999808688698, mc.player.posZ, false))
        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.7500019, mc.player.posZ, false))
        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY + 0.9999962, mc.player.posZ, false))
        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.17000380178814, mc.player.posZ, false))
        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.17001330178815, mc.player.posZ, false))
        val sneaking = mc.player.isSneaking
        if (!sneaking)
            mc.player.connection.sendPacket(CPacketEntityAction(mc.player, CPacketEntityAction.Action.START_SNEAKING))
        /*
        mc.player.setPosition(mc.player.posX, mc.player.posY + 1.0, mc.player.posZ)
        mc.playerController.processRightClickBlock(
            mc.player,
            mc.world,
            p,
            facing,
            hitVec,
            EnumHand.MAIN_HAND
        )
        mc.player.setPosition(mc.player.posX,mc.player.posY - 1.0,mc.player.posZ)
        */
        mc.player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(p, facing, EnumHand.MAIN_HAND, 0.5f, 1.0f, 0.5f))
        if (!sneaking)
            mc.player.connection.sendPacket(CPacketEntityAction(mc.player, CPacketEntityAction.Action.STOP_SNEAKING))
        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY + 1.2426308013947485, mc.player.posZ, false))
        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY + 3.3400880035762786, mc.player.posZ, false))
        mc.player.connection.sendPacket(CPacketPlayer.Position(mc.player.posX, mc.player.posY - 1.0, mc.player.posZ, false))
    }
}