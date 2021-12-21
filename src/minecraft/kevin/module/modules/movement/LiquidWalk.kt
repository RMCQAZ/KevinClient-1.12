package kevin.module.modules.movement

import kevin.event.*
import kevin.module.*
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.BlockUtils.collideBlock
import kevin.utils.BlockUtils.getBlock
import net.minecraft.block.BlockLiquid
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

class LiquidWalk : Module(name = "LiquidWalk", description = "Allows you to walk on water.", category = ModuleCategory.MOVEMENT) {
    val modeValue = ListValue("Mode", arrayOf("Vanilla", "NCP", "AAC", "AAC3.3.11", "AACFly", "Spartan", "Dolphin"), "NCP")
    private val noJumpValue = BooleanValue("NoJump", false)
    private val aacFlyValue = FloatValue("AACFlyMotion", 0.5f, 0.1f, 1f)

    private var nextTick = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player

        if (thePlayer == null || thePlayer.isSneaking) return

        when (modeValue.get().toLowerCase()) {
            "ncp", "vanilla" -> if (collideBlock(thePlayer.entityBoundingBox) { it is BlockLiquid } && thePlayer.isInsideOfMaterial(Material.AIR) && !thePlayer.isSneaking) thePlayer.motionY = 0.08
            "aac" -> {
                val blockPos = thePlayer.position.down()
                if (!thePlayer.onGround && getBlock(blockPos) == Blocks.WATER || thePlayer.isInWater) {
                    if (!thePlayer.isSprinting) {
                        thePlayer.motionX *= 0.99999
                        thePlayer.motionY *= 0.0
                        thePlayer.motionZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((thePlayer.posY - (thePlayer.posY - 1).toInt()).toInt() / 8f).toDouble()
                    } else {
                        thePlayer.motionX *= 0.99999
                        thePlayer.motionY *= 0.0
                        thePlayer.motionZ *= 0.99999
                        if (thePlayer.isCollidedHorizontally) thePlayer.motionY = ((thePlayer.posY - (thePlayer.posY - 1).toInt()).toInt() / 8f).toDouble()
                    }
                    if (thePlayer.fallDistance >= 4) thePlayer.motionY = -0.004 else if (thePlayer.isInWater) thePlayer.motionY = 0.09
                }
                if (thePlayer.hurtTime != 0) thePlayer.onGround = false
            }
            "spartan" -> if (thePlayer.isInWater) {
                if (thePlayer.isCollidedHorizontally) {
                    thePlayer.motionY += 0.15
                    return
                }
                val block = getBlock(BlockPos(thePlayer.posX, thePlayer.posY + 1, thePlayer.posZ))
                val blockUp = getBlock(BlockPos(thePlayer.posX, thePlayer.posY + 1.1, thePlayer.posZ))

                if (blockUp is BlockLiquid) {
                    thePlayer.motionY = 0.1
                } else if (block is BlockLiquid) {
                    thePlayer.motionY = 0.0
                }

                thePlayer.onGround = true
                thePlayer.motionX *= 1.085
                thePlayer.motionZ *= 1.085
            }
            "aac3.3.11" -> if (thePlayer.isInWater) {
                thePlayer.motionX *= 1.17
                thePlayer.motionZ *= 1.17
                if (thePlayer.isCollidedHorizontally) thePlayer.motionY = 0.24 else if (mc.world!!.getBlockState(BlockPos(thePlayer.posX, thePlayer.posY + 1.0, thePlayer.posZ)).block != Blocks.AIR) thePlayer.motionY += 0.04
            }
            "dolphin" -> if (thePlayer.isInWater) thePlayer.motionY += 0.03999999910593033
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if ("aacfly" == modeValue.get().toLowerCase() && mc.player!!.isInWater) {
            event.y = aacFlyValue.get().toDouble()
            mc.player!!.motionY = aacFlyValue.get().toDouble()
        }
    }

    @EventTarget
    fun onBlockBB(event: BlockBBEvent) {
        if (mc.player == null)
            return

        if (event.block is BlockLiquid && !collideBlock(mc.player.entityBoundingBox) { it is BlockLiquid } && !mc.player.isSneaking) {
            when (modeValue.get().toLowerCase()) {
                "ncp", "vanilla" -> event.boundingBox = AxisAlignedBB(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.toDouble(), event.y + 1.toDouble(), event.z + 1.toDouble())
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val thePlayer = mc.player

        if (thePlayer == null || !modeValue.get().equals("NCP", ignoreCase = true))
            return

        if (event.packet is CPacketPlayer) {
            val packetPlayer = event.packet
            if (collideBlock(AxisAlignedBB(thePlayer.entityBoundingBox.maxX, thePlayer.entityBoundingBox.maxY, thePlayer.entityBoundingBox.maxZ, thePlayer.entityBoundingBox.minX, thePlayer.entityBoundingBox.minY - 0.01, thePlayer.entityBoundingBox.minZ)) { it is BlockLiquid }) {
                nextTick = !nextTick
                if (nextTick) packetPlayer.y -= 0.001
            }
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        val thePlayer = mc.player ?: return

        val block = getBlock(BlockPos(thePlayer.posX, thePlayer.posY - 0.01, thePlayer.posZ))

        if (noJumpValue.get() && block is BlockLiquid)
            event.cancelEvent()
    }

    override val tag: String
        get() = modeValue.get()
}