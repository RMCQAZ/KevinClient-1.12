package kevin.module.modules.world

import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.IntegerValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.*
import kevin.utils.timers.MSTimer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class Surround : Module("Surround", "Surrounds you with obsidian to take less damage", category = ModuleCategory.WORLD) {
    private val placeDelay = IntegerValue("Place delay", 50, 0, 1000)
    private val multiPlace = IntegerValue("Multi Place", 2, 1, 5)
    private val rotation = BooleanValue("Rotation", true)
    private val double = BooleanValue("Double",false)
    private val full = BooleanValue("Full",false)

    private val timer = MSTimer()
    private val pos: List<BlockPos>
    get() {
        val list = if (double.get()) mutableListOf(
            BlockPos(0,-1,0),
            BlockPos(-1,-1,0),
            BlockPos(1,-1,0),
            BlockPos(0,-1,-1),
            BlockPos(0,-1,1),
            BlockPos(-1,0,0),
            BlockPos(1,0,0),
            BlockPos(0,0,-1),
            BlockPos(0,0,1),
            BlockPos(-1,1,0),
            BlockPos(1,1,0),
            BlockPos(0,1,-1),
            BlockPos(0,1,1)
        ) else mutableListOf(
            BlockPos(0,-1,0),
            BlockPos(-1,-1,0),
            BlockPos(1,-1,0),
            BlockPos(0,-1,-1),
            BlockPos(0,-1,1),
            BlockPos(-1,0,0),
            BlockPos(1,0,0),
            BlockPos(0,0,-1),
            BlockPos(0,0,1),
        )
        if (full.get()){
            list.addAll(
                if (double.get())
                    mutableListOf(
                        BlockPos(-1,0,-1),
                        BlockPos(1,0,-1),
                        BlockPos(-1,0,1),
                        BlockPos(1,0,1),
                        BlockPos(-1,1,-1),
                        BlockPos(1,1,-1),
                        BlockPos(-1,1,1),
                        BlockPos(1,1,1)
                    )
                else
                    mutableListOf(
                        BlockPos(-1,0,-1),
                        BlockPos(1,0,-1),
                        BlockPos(-1,0,1),
                        BlockPos(1,0,1)
                    )
            )
        }
        return list
    }
    private val bp = arrayOf(
        BlockPos(0,-1,0) to EnumFacing.UP,
        BlockPos(-1,0,0) to EnumFacing.EAST,
        BlockPos(0,0,-1) to EnumFacing.SOUTH,
        BlockPos(0,0,1) to EnumFacing.NORTH,
        BlockPos(1,0,0) to EnumFacing.WEST,
        BlockPos(0,1,0) to EnumFacing.DOWN,
    )

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!timer.hasTimePassed(placeDelay.get().toLong()) || !mc.player.onGround) return
        timer.reset()
        var placed = 0
        for (p in pos){
            if (placed >= multiPlace.get()) break
            val blockPos = p.add(mc.player.posX.fastFloor(),mc.player.posY.fastFloor(),mc.player.posZ.fastFloor())
            if (!mc.world.getBlockState(blockPos).material.isReplaceable) continue
            if (doPlace(blockPos))
                placed++
        }
    }
    private fun doPlace(blockPos: BlockPos): Boolean {
        var state = true
        for (b in bp){
            val p = blockPos.add(b.first)
            val obsidianSlot = mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
            if (obsidianSlot != null) {
                if (!mc.world.getBlockState(p).material.isReplaceable){
                    switchHotbar(obsidianSlot.hotbarSlot) {
                        val facing = b.second
                        if (rotation.get()) RotationUtils.setTargetRotation(RotationUtils.faceBlock(p).rotation)
                        val dirVec = Vec3d(facing.directionVec)
                        val posVec = Vec3d(blockPos).addVector(0.5, 0.5, 0.5)
                        val hitVec = posVec.add(Vec3d(dirVec.x * 0.5, dirVec.y * 0.5, dirVec.z * 0.5))
                        state = mc.playerController.processRightClickBlock(
                            mc.player,
                            mc.world,
                            p,
                            facing,
                            hitVec,
                            EnumHand.MAIN_HAND
                        ) == EnumActionResult.SUCCESS
                    }
                    break
                }
            } else break
        }
        return state
    }
}