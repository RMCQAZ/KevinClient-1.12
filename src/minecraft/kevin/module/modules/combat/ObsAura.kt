package kevin.module.modules.combat

import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.module.FloatValue
import kevin.module.IntegerValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.*
import kevin.utils.timers.MSTimer
import net.minecraft.init.Blocks
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d

class ObsAura : Module("ObsAura" ,"Use Obsidian to trap the enemy.", category = ModuleCategory.COMBAT) {
    private val placeDelay = IntegerValue("Place delay", 50, 0, 1000)
    private val multiPlace = IntegerValue("Multi Place", 2, 1, 5)
    private val range = FloatValue("Range", 5F, 1F, 6F)

    private val timer = MSTimer()
    private val pos = arrayOf(
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
        BlockPos(0,1,1),
        BlockPos(0,2,-1),
        BlockPos(0,2,0),
    )
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
        if (!timer.hasTimePassed(placeDelay.get().toLong())) return
        timer.reset()
        val targets = mc.world.loadedEntityList
            .filter {
                EntityUtils.isSelected(it, true) &&
                it.getDistanceToEntity(mc.player) <= range.get()
            }
        var placed = 0
        for (ent in targets) {
            if (!ent.onGround) continue
            for (p in pos){
                if (placed >= multiPlace.get()) break
                val blockPos = p.add(ent.posX.fastFloor(),ent.posY.fastFloor(),ent.posZ.fastFloor())
                if (!mc.world.getBlockState(blockPos).material.isReplaceable) continue
                doPlace(blockPos)
                placed++
            }
        }
    }
    private fun doPlace(blockPos: BlockPos) {
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
                        mc.playerController.processRightClickBlock(
                            mc.player,
                            mc.world,
                            p,
                            facing,
                            hitVec,
                            EnumHand.MAIN_HAND
                        )
                    }
                    break
                }
            } else break
        }
    }
}