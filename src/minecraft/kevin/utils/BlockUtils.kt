package kevin.utils

import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import kotlin.math.floor

typealias Collidable = (Block?) -> Boolean

object BlockUtils : Mc() {

    @JvmStatic
    fun floorVec3(vec3: Vec3d) = Vec3d(floor(vec3.x),floor(vec3.y),floor(vec3.z))

    @JvmStatic
    fun getBlock(blockPos: BlockPos): Block? = mc.world?.getBlockState(blockPos)?.block

    @JvmStatic
    inline fun getMaterial(blockPos: BlockPos): Material? {
        val state = getState(blockPos)

        return state?.block?.getMaterial(state)
    }

    @JvmStatic
    inline fun isReplaceable(blockPos: BlockPos) = getMaterial(blockPos)?.isReplaceable ?: false

    @JvmStatic
    inline fun getState(blockPos: BlockPos): IBlockState? = mc.world?.getBlockState(blockPos)

    @JvmStatic
    fun canBeClicked(blockPos: BlockPos) = getBlock(blockPos)?.canCollideCheck(getState(blockPos), false) ?: false &&
            mc.world!!.worldBorder.contains(blockPos)

    @JvmStatic
    fun getBlockName(id: Int): String = Block.getBlockById(id)!!.localizedName

    @JvmStatic
    fun isFullBlock(blockPos: BlockPos): Boolean {
        val axisAlignedBB = getBlock(blockPos)?.getCollisionBoundingBox(getState(blockPos), mc.world!!, blockPos
            ?: return false)
            ?: return false
        return axisAlignedBB.maxX - axisAlignedBB.minX == 1.0 && axisAlignedBB.maxY - axisAlignedBB.minY == 1.0 && axisAlignedBB.maxZ - axisAlignedBB.minZ == 1.0
    }

    @JvmStatic
    fun getCenterDistance(blockPos: BlockPos) =
        mc.player!!.getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

    @JvmStatic
    fun searchBlocks(radius: Int): Map<BlockPos, Block> {
        val blocks = mutableMapOf<BlockPos, Block>()

        val thePlayer = mc.player ?: return blocks

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = BlockPos(thePlayer.posX.toInt() + x, thePlayer.posY.toInt() + y,
                        thePlayer.posZ.toInt() + z)
                    val block = getBlock(blockPos) ?: continue

                    blocks[blockPos] = block
                }
            }
        }

        return blocks
    }

    @JvmStatic
    fun collideBlock(axisAlignedBB: AxisAlignedBB, collide: Collidable): Boolean {
        val thePlayer = mc.player!!

        for (x in floor(thePlayer.entityBoundingBox.minX).toInt() until
                floor(thePlayer.entityBoundingBox.maxX).toInt() + 1L) {
            for (z in floor(thePlayer.entityBoundingBox.minZ).toInt() until
                    floor(thePlayer.entityBoundingBox.maxZ).toInt() + 1) {
                val block = getBlock(BlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble()))

                if (!collide(block))
                    return false
            }
        }

        return true
    }

    @JvmStatic
    fun collideBlockIntersects(axisAlignedBB: AxisAlignedBB, collide: Collidable): Boolean {
        val thePlayer = mc.player!!
        val world = mc.world!!

        for (x in floor(thePlayer.entityBoundingBox.minX).toInt() until
                floor(thePlayer.entityBoundingBox.maxX).toInt() + 1) {
            for (z in floor(thePlayer.entityBoundingBox.minZ).toInt() until
                    floor(thePlayer.entityBoundingBox.maxZ).toInt() + 1) {
                val blockPos = BlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble())
                val block = getBlock(blockPos)

                if (collide(block)) {
                    val boundingBox = getState(blockPos)?.let { block?.getCollisionBoundingBox(it,world, blockPos) }
                        ?: continue

                    if (thePlayer.entityBoundingBox.intersects(boundingBox))
                        return true
                }
            }
        }
        return false
    }

}