package kevin.module.modules.combat

import kevin.event.*
import kevin.module.FloatValue
import kevin.module.IntegerValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.module.modules.combat.CrystalAura.canPlaceCrystal
import kevin.utils.*
import kevin.utils.timers.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.inventory.Slot
import net.minecraft.network.play.client.CPacketHeldItemChange
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.network.play.client.CPacketPlayerTryUseItemOnBlock
import net.minecraft.network.play.client.CPacketUseEntity
import net.minecraft.network.play.server.SPacketSpawnObject
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.awt.Color

class OCBreak : Module("OCBreak", "Place a obsidian and a crystal on target head and break it.", category = ModuleCategory.COMBAT) {
    private val range = FloatValue("Range", 5F, 1F, 6F)

    private var spoofing = false
    private var slot = -1
    private var targetPos: BlockPos? = null
    private var stat = 0
    private var crystalID = -11451419
    private var first = true

    private val bp = arrayOf(
        BlockPos(0,-1,0) to EnumFacing.UP,
        BlockPos(-1,0,0) to EnumFacing.EAST,
        BlockPos(0,0,-1) to EnumFacing.SOUTH,
        BlockPos(0,0,1) to EnumFacing.NORTH,
        BlockPos(1,0,0) to EnumFacing.WEST,
        BlockPos(0,1,0) to EnumFacing.DOWN,
    )

    override fun onDisable() {
        reset()
        targetPos = null
        stat = 0
        crystalID = -11451419
        first = true
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (targetPos!=null) {
            RenderUtils.drawBlockBox(targetPos, Color.RED, true)
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val target = getTarget()
        if (target == null) {
            reset()
            targetPos = null
            stat = 0
            first = true
            return
        }
        targetPos = BlockPos(target.posX.fastFloor(), target.posY.fastFloor()+2, target.posZ.fastFloor())
        val crystal = mc.player.hotbarSlots.firstItem(Items.END_CRYSTAL)
        val obsidianSlot = mc.player.hotbarSlots.firstBlock(Blocks.OBSIDIAN)
        if (crystal == null || obsidianSlot == null) {
            reset()
            targetPos = null
            stat = 0
            first = true
            return
        }
        when(stat) {
            0 -> {
                if (mc.world.isAirBlock(targetPos)) {
                    if(isCrystalAlreadyExists(targetPos!!)) {
                        stat = 1
                    } else if (!placeBlock(obsidianSlot)) {
                        reset()
                        targetPos = null
                        stat = 0
                        return
                    } else if (!canPlaceCrystal(targetPos!!)) {
                        reset()
                        targetPos = null
                        stat = 0
                        return
                    } else {
                        placeCrystal(crystal)
                        stat = 1
                    }
                } else {
                    if(isCrystalAlreadyExists(targetPos!!)) {
                        stat = 1
                    } else if (!canPlaceCrystal(targetPos!!)) {
                        reset()
                        targetPos = null
                        stat = 0
                        return
                    } else {
                        placeCrystal(crystal)
                        stat = 1
                    }
                }
            }
            1 -> {
                if (mc.world.isAirBlock(targetPos)) {
                    breakCrystal()
                    reset()
                    stat = 0
                } else breakTarget()
            }
        }
    }

    private fun isCrystalAlreadyExists(blockPos: BlockPos): Boolean {
        val boost = blockPos.add(0, 1, 0)
        val boost2 = blockPos.add(0, 2, 0)
        val first = mc.world.getEntitiesWithinAABB(
            EntityEnderCrystal::class.java, AxisAlignedBB(boost)
        )
        val second = mc.world.getEntitiesWithinAABB(
            EntityEnderCrystal::class.java, AxisAlignedBB(boost2)
        )
        if (first.isNotEmpty()) {
            crystalID = first.first().entityId
            return true
        }
        if (second.isNotEmpty()) {
            crystalID = second.first().entityId
            return true
        }
        return false
    }

    private fun breakTarget() {
        var bestSpeed = 1F
        var bestSlot = -1
        val blockState = mc.world!!.getBlockState(targetPos!!)
        for (i in 0..8) {
            val item = mc.player!!.inventory.getStackInSlot(i) ?: continue
            val speed = item.getStrVsBlock(blockState)
            if (speed > bestSpeed) {
                bestSpeed = speed
                bestSlot = i
            }
        }
        if (bestSlot != -1 && bestSlot != mc.player.inventory.currentItem && !spoofing) {
            spoofing = true
            slot = mc.player.inventory.currentItem
            mc.player.connection.sendPacket(CPacketHeldItemChange(bestSlot))
        }
        if (first) {
            first = false
            mc.connection!!.sendPacket(
                CPacketPlayerDigging(
                    CPacketPlayerDigging.Action.START_DESTROY_BLOCK,
                    targetPos!!,
                    EnumFacing.DOWN
                )
            )
        } else mc.connection!!.sendPacket(
            CPacketPlayerDigging(
                CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                targetPos!!,
                EnumFacing.DOWN
            )
        )
    }

    private fun placeCrystal(slot: Slot) {
        switchHotbar(slot.hotbarSlot) {
            mc.player.connection.sendPacket(CPacketPlayerTryUseItemOnBlock(
                targetPos, EnumFacing.DOWN, EnumHand.MAIN_HAND, 0f, 0f, 0f
            ))
        }
    }

    private fun placeBlock(slot: Slot): Boolean {
        for (b in bp){
            val p = targetPos!!.add(b.first)
            if (!mc.world.getBlockState(p).material.isReplaceable){
                switchHotbar(slot.hotbarSlot) {
                    val facing = b.second
                    val dirVec = Vec3d(facing.directionVec)
                    val posVec = Vec3d(targetPos!!).addVector(0.5, 0.5, 0.5)
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
                return true
            }
        }
        return false
    }

    private fun breakCrystal() {
        mc.player.connection.sendPacket(CPacketUseEntity(crystalID))
        crystalID = -114514
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is SPacketSpawnObject &&
            packet.type == 51) {
            val crystal = EntityEnderCrystal(mc.world, packet.x, packet.y, packet.z)
            if (mc.player.getDistanceToEntity(crystal) > range.get()) return
            if (targetPos!=null&&crystal.getDistance(targetPos!!.x.toDouble(),targetPos!!.y.toDouble(),targetPos!!.z.toDouble()) > 2) return
            crystalID = packet.entityID
        }
    }

    private fun getTarget(): Entity? =
        mc.world.loadedEntityList
            .filter { EntityUtils.isSelected(it, true)
                    && it.getDistanceToEntity(mc.player) <= range.get()
                    && (canPlace(it)
                    ||(!mc.world.isAirBlock(BlockPos(it.posX.fastFloor(), it.posY.fastFloor()+2, it.posZ.fastFloor()))
                    && mc.world.getBlockState(BlockPos(it.posX.fastFloor(), it.posY.fastFloor()+2, it.posZ.fastFloor())).block != Blocks.BEDROCK)) }
            .minByOrNull { it.getDistanceToEntity(mc.player) }

    private fun canPlace(entity: Entity): Boolean {
        val pos = BlockPos(entity.posX.fastFloor(), entity.posY.fastFloor()+2, entity.posZ.fastFloor())
        for (p in bp) {
            val blockState = mc.world.getBlockState(pos.add(p.first))
            if (!blockState.material.isReplaceable)
                return true
        }
        return false
    }

    private fun reset() {
        if (spoofing) {
            mc.player.connection.sendPacket(CPacketHeldItemChange(slot))
            spoofing = false
        }
    }
}