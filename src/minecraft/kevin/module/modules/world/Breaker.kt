package kevin.module.modules.world

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.event.UpdateEvent
import kevin.module.*
import kevin.module.modules.combat.KillAura
import kevin.module.modules.misc.Teams
import kevin.module.modules.player.AutoTool
import kevin.utils.BlockUtils.getBlock
import kevin.utils.BlockUtils.getBlockName
import kevin.utils.BlockUtils.getCenterDistance
import kevin.utils.BlockUtils.isFullBlock
import kevin.utils.RenderUtils
import kevin.utils.RotationUtils
import kevin.utils.timers.MSTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.CPacketPlayerDigging
import net.minecraft.util.EnumActionResult
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumHand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import java.awt.Color

class Breaker : Module("Breaker",description = "Destroys selected blocks around you.", category = ModuleCategory.WORLD) {

    /**
     * SETTINGS
     */

    private val blockValue = BlockValue("Block", 26)
    private val throughWallsValue = ListValue("ThroughWalls", arrayOf("None", "Raycast", "Around"), "None")
    private val rangeValue = FloatValue("Range", 5F, 1F, 7F)
    private val actionValue = ListValue("Action", arrayOf("Destroy", "Use"), "Destroy")
    private val instantValue = BooleanValue("Instant", false)
    private val switchValue = IntegerValue("SwitchDelay", 250, 0, 1000)
    private val swingValue = BooleanValue("Swing", true)
    private val rotationsValue = BooleanValue("Rotations", true)
    private val surroundingsValue = BooleanValue("Surroundings", true)
    private val noHitValue = BooleanValue("NoHit", false)
    private val bypassValue = BooleanValue("Bypass",false)


    /**
     * VALUES
     */

    override fun onDisable() {
        pos = null
        oldPos = null
    }

    private var pos: BlockPos? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private val switchTimer = MSTimer()
    private var isRealBlock = false
    var currentDamage = 0F

    @EventTarget
    fun onUpdate(event: UpdateEvent) {

        //if (event.eventState == UpdateState.OnUpdate) return

        val thePlayer = mc.player ?: return

        if (noHitValue.get()) {
            val killAura = KevinClient.moduleManager.getModule("KillAura") as KillAura

            if (killAura.state && killAura.target != null)
                return
        }

        val targetId = blockValue.get()

        if (pos == null || Block.getIdFromBlock(getBlock(pos!!)!!) != targetId ||
            getCenterDistance(pos!!) > rangeValue.get())
            pos = find(targetId)

        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
            return
        }

        // BedCheck
        val teams = KevinClient.moduleManager.getModule("Teams") as Teams
        if (Block.getBlockById(targetId)==Blocks.BED&&teams.bedCheckValue.get()&&pos in teams.teamBed){
            pos = null
            currentDamage = 0F
            return
        }

        var currentPos = pos ?: return
        var rotations = RotationUtils.faceBlock(currentPos) ?: return

        // Surroundings
        var surroundings = false

        if (surroundingsValue.get()) {
            val eyes = thePlayer.getPositionEyes(1F)
            val blockPos = mc.world!!.rayTraceBlocks(eyes, rotations.vec, false,
                false, true)?.blockPos

            if (blockPos != null && (blockPos) !is BlockAir) {
                if (currentPos.x != blockPos.x || currentPos.y != blockPos.y || currentPos.z != blockPos.z)
                    surroundings = true

                pos = blockPos
                currentPos = pos ?: return
                rotations = RotationUtils.faceBlock(currentPos) ?: return
            }
        }

        // Reset switch timer when position changed
        if (oldPos != null && oldPos != currentPos) {
            currentDamage = 0F
            switchTimer.reset()
        }

        oldPos = currentPos

        if (!switchTimer.hasTimePassed(switchValue.get().toLong()))
            return

        // Block hit delay
        if (blockHitDelay > 0) {
            blockHitDelay--
            return
        }

        // Face block
        if (rotationsValue.get())
            RotationUtils.setTargetRotation(rotations.rotation)

        when {
            // Destory block
            actionValue.get().equals("destroy", true) || surroundings || !isRealBlock -> {
                // Auto Tool
                val autoTool = KevinClient.moduleManager.getModule("AutoTool") as AutoTool
                if (autoTool.state)
                    autoTool.switchSlot(currentPos)

                // Break block
                if (instantValue.get()) {
                    // CivBreak style block breaking
                    mc.connection!!.sendPacket(
                        CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK,
                        currentPos, EnumFacing.DOWN)
                    )

                    if (swingValue.get())
                        thePlayer.swingArm(EnumHand.MAIN_HAND)

                    mc.connection!!.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                        currentPos, EnumFacing.DOWN))
                    currentDamage = 0F
                    return
                }

                // Minecraft block breaking
                val block = getBlock(currentPos) ?: return

                if (currentDamage == 0F) {
                    mc.connection!!.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.START_DESTROY_BLOCK,
                        currentPos, EnumFacing.DOWN))

                    if (thePlayer.capabilities.isCreativeMode ||
                        block.getPlayerRelativeBlockHardness(mc.world.getBlockState(currentPos),thePlayer, mc.world!!, pos!!) >= 1.0F) {
                        if (swingValue.get())
                            thePlayer.swingArm(EnumHand.MAIN_HAND)
                        mc.playerController.onPlayerDestroyBlock(pos!!)

                        currentDamage = 0F
                        pos = null
                        return
                    }
                }

                if (swingValue.get())
                    thePlayer.swingArm(EnumHand.MAIN_HAND)

                currentDamage += block.getPlayerRelativeBlockHardness(mc.world.getBlockState(currentPos),thePlayer, mc.world!!, currentPos)
                mc.world!!.sendBlockBreakProgress(thePlayer.entityId, currentPos, (currentDamage * 10F).toInt() - 1)

                if (currentDamage >= 1F) {
                    mc.connection!!.sendPacket(CPacketPlayerDigging(CPacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                        currentPos, EnumFacing.DOWN))
                    mc.playerController.onPlayerDestroyBlock(currentPos)
                    blockHitDelay = 4
                    currentDamage = 0F
                    pos = null
                }
            }

            // Use block
            actionValue.get().equals("use", true) -> if (mc.playerController.processRightClickBlock(
                    thePlayer, mc.world!!, pos!!, EnumFacing.DOWN,
                    Vec3d(currentPos.x.toDouble(), currentPos.y.toDouble(), currentPos.z.toDouble()),EnumHand.MAIN_HAND) == EnumActionResult.SUCCESS) {
                if (swingValue.get())
                    thePlayer.swingArm(EnumHand.MAIN_HAND)

                blockHitDelay = 4
                currentDamage = 0F
                pos = null
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        RenderUtils.drawBlockBox(pos ?: return, Color.RED, true)
    }

    /**
     * Find new target block by [targetID]
     */
    /*private fun find(targetID: Int) =
        searchBlocks(rangeValue.get().toInt() + 1).filter {
                    Block.getIdFromBlock(it.value) == targetID && getCenterDistance(it.key) <= rangeValue.get()
                            && (isHitable(it.key) || surroundingsValue.get())
                }.minBy { getCenterDistance(it.key) }?.key*/

    //Removed triple iteration of blocks to improve speed
    /**
     * Find new target block by [targetID]
     */
    private fun find(targetID: Int): BlockPos? {
        val thePlayer = mc.player ?: return null

        val radius = rangeValue.get().toInt() + 1

        var nearestBlockDistance = Double.MAX_VALUE
        var nearestBlock: BlockPos? = null

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = BlockPos(thePlayer.posX.toInt() + x, thePlayer.posY.toInt() + y,
                        thePlayer.posZ.toInt() + z)
                    val block = getBlock(blockPos) ?: continue

                    if (Block.getIdFromBlock(block) != targetID) continue

                    val distance = getCenterDistance(blockPos)
                    if (distance > rangeValue.get()) continue
                    if (nearestBlockDistance < distance) continue
                    if (!isHitable(blockPos) && !surroundingsValue.get()) continue

                    nearestBlockDistance = distance
                    nearestBlock = blockPos
                }
            }
        }

        isRealBlock=true
        if(bypassValue.get()){
            val upBlock = nearestBlock?.up() ?: return nearestBlock
            if(getBlock(upBlock)!=Blocks.AIR){
                isRealBlock=false
                return upBlock
            }
        }

        return nearestBlock
    }

    /**
     * Check if block is hitable (or allowed to hit through walls)
     */
    private fun isHitable(blockPos: BlockPos): Boolean {
        val thePlayer = mc.player ?: return false

        return when (throughWallsValue.get().toLowerCase()) {
            "raycast" -> {
                val eyesPos = Vec3d(thePlayer.posX, thePlayer.entityBoundingBox.minY +
                        thePlayer.eyeHeight, thePlayer.posZ)
                val movingObjectPosition = mc.world!!.rayTraceBlocks(eyesPos,
                    Vec3d(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5), false,
                    true, false)

                movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
            }
            "around" -> !isFullBlock(blockPos.down()) || !isFullBlock(blockPos.up()) || !isFullBlock(blockPos.north())
                    || !isFullBlock(blockPos.east()) || !isFullBlock(blockPos.south()) || !isFullBlock(blockPos.west())
            else -> true
        }
    }

    override val tag: String
        get() = getBlockName(blockValue.get())
}