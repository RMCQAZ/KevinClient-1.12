package kevin.module.modules.render

import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.event.UpdateEvent
import kevin.module.*
import kevin.utils.RenderUtils
import kevin.utils.timers.MSTimer
import net.minecraft.block.state.IBlockState
import net.minecraft.client.renderer.BufferBuilder
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object HoleESP : Module("HoleESP", "Show safe holes for crystal pvp.", category = ModuleCategory.RENDER) {
    private val obbyHole = BooleanValue("Obby Hole", true)
    private val twoBlocksHole = BooleanValue("2 Blocks Hole", true)
    private val fourBlocksHole = BooleanValue("4 Blocks Hole", true)
    private val trappedHole = BooleanValue("Trapped Hole", true)

    private val bedrockR = IntegerValue("Bedrock R", 31, 0, 255)
    private val bedrockG = IntegerValue("Bedrock G", 255, 0, 255)
    private val bedrockB = IntegerValue("Bedrock B", 31, 0, 255)
    private val bedrockColor
    get() = Color(bedrockR.get(), bedrockG.get(), bedrockB.get())

    private val obbyR = IntegerValue("Obby R", 255, 0, 255)
    private val obbyG = IntegerValue("Obby G", 255, 0, 255)
    private val obbyB = IntegerValue("Obby B", 31, 0, 255)
    private val obbyColor
        get() = Color(obbyR.get(), obbyG.get(), obbyB.get())

    private val twoBlocksR = IntegerValue("2 Blocks R", 255, 0, 255)
    private val twoBlocksG = IntegerValue("2 Blocks G", 127, 0, 255)
    private val twoBlocksB = IntegerValue("2 Blocks B", 31, 0, 255)
    private val twoBlocksColor
        get() = Color(twoBlocksR.get(), twoBlocksG.get(), twoBlocksB.get())

    private val fourBlocksR = IntegerValue("4 Blocks R", 255, 0, 255)
    private val fourBlocksG = IntegerValue("4 Blocks G", 127, 0, 255)
    private val fourBlocksB = IntegerValue("4 Blocks B", 31, 0, 255)
    private val fourBlocksColor
        get() = Color(fourBlocksR.get(), fourBlocksG.get(), fourBlocksB.get())

    private val trappedR = IntegerValue("Trapped R", 255, 0, 255)
    private val trappedG = IntegerValue("Trapped G", 31, 0, 255)
    private val trappedB = IntegerValue("Trapped B", 31, 0, 255)
    private val trappedColor
        get() = Color(trappedR.get(), trappedG.get(), trappedB.get())

    private val renderMode = ListValue("Render Mode", arrayOf("Box", "OtherBox", "2D", "GLOW"), "OtherBox")
    private val aFilled = IntegerValue("Filled Alpha", 63, 0, 255)
    private val aOutline = IntegerValue("Outline Alpha", 255, 0, 255)
    private val glowHeight = FloatValue("Glow Height", 1.0f, 0.25f, 4.0f)
    private val flatOutline = BooleanValue("Flat Outline", true)
    private val lineWidth = FloatValue("Line Width", 2.0f, 1.0f, 8.0f)
    private val range = IntegerValue("Range", 16, 4, 128)
    private val high = IntegerValue("High", 16, 10, 128)

    private val timer = MSTimer()
    private val list = arrayListOf<HoleInfo>()
    private var thread: Thread? = null

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (timer.hasTimePassed(500) && (thread == null || !thread!!.isAlive)) {
            timer.reset()
            updateRenderer()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        synchronized(list) {
            for (it in list) {
                val color = getColor(it) ?: continue
                when(renderMode.get()) {
                    "Box","OtherBox" -> RenderUtils.drawBlockBox(it.origin, color, renderMode equal "Box")
                    "2D" -> RenderUtils.draw2D(it.origin, color.rgb,Color.black.rgb)
                    "GLOW" -> drawGlow(it, color)
                }
            }
        }
    }

    private fun drawGlow(holeInfo: HoleInfo, color: Color) {
        glShadeModel(GL_SMOOTH)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)

        GlStateManager.disableCull()
        drawFilled(holeInfo,color)
        GlStateManager.enableCull()

        glLineWidth(lineWidth.get())
        glEnable(GL_LINE_SMOOTH)

        renderOutline(holeInfo, color)

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
        glDepthMask(true)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_LINE_SMOOTH)
    }

    private fun drawFilled(holeInfo: HoleInfo, color: Color) {
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer

        worldrenderer.begin(GL_QUADS, DefaultVertexFormats.POSITION_COLOR)

        val timer = mc.timer
        val player: EntityPlayer = mc.player

        val posX: Double =
            player.lastTickPosX + (player.posX - player.lastTickPosX) * timer.renderPartialTicks.toDouble()
        val posY: Double =
            player.lastTickPosY + (player.posY - player.lastTickPosY) * timer.renderPartialTicks.toDouble()
        val posZ: Double =
            player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * timer.renderPartialTicks.toDouble()

        val box = holeInfo.boundingBox.offset(-posX, -posY, -posZ)

        worldrenderer.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.minX, box.minY, box.minZ, color.alpha(aFilled.get()))

        // -X
        worldrenderer.putVertex(box.minX, box.minY, box.minZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.minX, box.minY + glowHeight.get(), box.maxZ, color.alpha(0))
        worldrenderer.putVertex(box.minX, box.minY + glowHeight.get(), box.minZ, color.alpha(0))

        // +X
        worldrenderer.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.maxX, box.minY + glowHeight.get(), box.minZ, color.alpha(0))
        worldrenderer.putVertex(box.maxX, box.minY + glowHeight.get(), box.maxZ, color.alpha(0))

        // -Z
        worldrenderer.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.minX, box.minY, box.minZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.minX, box.minY + glowHeight.get(), box.minZ, color.alpha(0))
        worldrenderer.putVertex(box.maxX, box.minY + glowHeight.get(), box.minZ, color.alpha(0))

        // +Z
        worldrenderer.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aFilled.get()))
        worldrenderer.putVertex(box.maxX, box.minY + glowHeight.get(), box.maxZ, color.alpha(0))
        worldrenderer.putVertex(box.minX, box.minY + glowHeight.get(), box.maxZ, color.alpha(0))

        tessellator.draw()
    }

    private fun renderOutline(holeInfo: HoleInfo, color: Color) {
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer

        worldrenderer.begin(GL_LINES, DefaultVertexFormats.POSITION_COLOR)

        val timer = mc.timer
        val player: EntityPlayer = mc.player

        val posX: Double =
            player.lastTickPosX + (player.posX - player.lastTickPosX) * timer.renderPartialTicks.toDouble()
        val posY: Double =
            player.lastTickPosY + (player.posY - player.lastTickPosY) * timer.renderPartialTicks.toDouble()
        val posZ: Double =
            player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * timer.renderPartialTicks.toDouble()

        val box = holeInfo.boundingBox.offset(-posX, -posY, -posZ)

        worldrenderer.putVertex(box.minX, box.minY, box.minZ, color.alpha(aOutline.get()))
        worldrenderer.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aOutline.get()))
        worldrenderer.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aOutline.get()))
        worldrenderer.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aOutline.get()))
        worldrenderer.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aOutline.get()))
        worldrenderer.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aOutline.get()))
        worldrenderer.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aOutline.get()))
        worldrenderer.putVertex(box.minX, box.minY, box.minZ, color.alpha(aOutline.get()))

        if (!flatOutline.get()) {
            worldrenderer.putVertex(box.minX, box.minY, box.minZ, color.alpha(aOutline.get()))
            worldrenderer.putVertex(box.minX, box.minY + glowHeight.get(), box.minZ, color.alpha(0))
            worldrenderer.putVertex(box.maxX, box.minY, box.minZ, color.alpha(aOutline.get()))
            worldrenderer.putVertex(box.maxX, box.minY + glowHeight.get(), box.minZ, color.alpha(0))
            worldrenderer.putVertex(box.maxX, box.minY, box.maxZ, color.alpha(aOutline.get()))
            worldrenderer.putVertex(box.maxX, box.minY + glowHeight.get(), box.maxZ, color.alpha(0))
            worldrenderer.putVertex(box.minX, box.minY, box.maxZ, color.alpha(aOutline.get()))
            worldrenderer.putVertex(box.minX, box.minY + glowHeight.get(), box.maxZ, color.alpha(0))
        }

        tessellator.draw()
    }

    private fun BufferBuilder.putVertex(x: Double, y: Double, z: Double, color: Color) {
        this.pos(x, y, z).color(color.red, color.green, color.blue, color.alpha).endVertex()
    }
    private fun Color.alpha(alpha: Int) = Color(this.red, this.green, this.blue, alpha)
    private fun updateRenderer() {
        thread = Thread({
            val found = mutableListOf<HoleInfo>()
            for (y in -high.get()..high.get()) {
                if (y+mc.player.posY.toInt() !in 0..255) continue
                for (x in -range.get()..range.get()) {
                    for (z in -range.get()..range.get()) {
                        val hole = checkHoleM(mc.player.position.add(x,y,z))
                        if (hole.isHole)
                            found.add(hole)
                    }
                }
            }
            synchronized(list) {
                list.clear()
                list.addAll(found)
            }
        },"HoleESP-Finder")
        thread!!.start()
    }

    private fun getColor(holeInfo: HoleInfo) =
        if (holeInfo.isTrapped) {
            trappedColor.takeIf { trappedHole.get() }
        } else {
            when (holeInfo.type) {
                HoleType.NONE -> null
                HoleType.BEDROCK -> bedrockColor
                HoleType.OBBY -> obbyColor.takeIf { obbyHole.get() }
                HoleType.TWO -> twoBlocksColor.takeIf { twoBlocksHole.get() }
                HoleType.FOUR -> fourBlocksColor.takeIf { fourBlocksHole.get() }
            }
        }
    private fun checkHoleM(pos: BlockPos): HoleInfo {
        if (pos.y !in 1..255 || !mc.world.isAirBlock(pos)) return HoleInfo.empty(pos.toImmutable())
        val mutablePos = BlockPos.MutableBlockPos(pos)
        return checkHole1(pos, mutablePos)
            ?: checkHole2(pos, mutablePos)
            ?: checkHole4(pos, mutablePos)
            ?: HoleInfo.empty(pos.toImmutable())
    }
    private fun checkHole1(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        if (!checkAir(holeOffsetCheck1, pos, mutablePos)) return null
        val type = checkSurroundPos(pos, mutablePos, surroundOffset1, HoleType.BEDROCK, HoleType.OBBY)
        return if (type == HoleType.NONE) {
            null
        } else {
            val holePosArray = holeOffset1.offset(pos)
            var trapped = false
            var fullyTrapped = true
            for (holePos in holePosArray) {
                if (mc.world.isAirBlock(mutablePos.setPos(holePos.x, holePos.y + 2, holePos.z))) {
                    fullyTrapped = false
                } else {
                    trapped = true
                }
            }
            HoleInfo(
                pos.toImmutable(),
                pos.toVec3d(0.5,
                    0.0, 0.5
                ),
                AxisAlignedBB(pos),
                holePosArray,
                surroundOffset1.offset(pos),
                type,
                trapped,
                fullyTrapped
            )
        }
    }

    private fun checkHole2(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        var x = true
        if (!mc.world.isAirBlock(mutablePos.setPos(pos.x + 1, pos.y, pos.z))) {
            if (!mc.world.isAirBlock(mutablePos.setPos(pos.x, pos.y, pos.z + 1))) return null
            else x = false
        }
        val checkArray = if (x) holeOffsetCheck2X else holeOffsetCheck2Z
        if (!checkAir(checkArray, pos, mutablePos)) return null
        val surroundOffset = if (x) surroundOffset2X else surroundOffset2Z
        val holeOffset = if (x) holeOffset2X else holeOffset2Z
        val centerX = if (x) 1.0 else 0.5
        val centerZ = if (x) 0.5 else 1.0

        val type = checkSurroundPos(pos, mutablePos, surroundOffset, HoleType.TWO, HoleType.TWO)
        return if (type == HoleType.NONE) {
            null
        } else {
            val holePosArray = holeOffset.offset(pos)
            var trapped = false
            var fullyTrapped = true
            for (holePos in holePosArray) {
                if (mc.world.isAirBlock(mutablePos.setPos(holePos.x, holePos.y + 2, holePos.z))) {
                    fullyTrapped = false
                } else {
                    trapped = true
                }
            }
            HoleInfo(
                pos.toImmutable(),
                pos.toVec3d(centerX,
                    0.0, centerZ
                ),
                if (x) {
                    AxisAlignedBB(
                        pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                        pos.x + 2.0, pos.y + 1.0, pos.z + 1.0
                    )
                } else {
                    AxisAlignedBB(
                        pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                        pos.x + 1.0, pos.y + 1.0, pos.z + 2.0
                    )
                },
                holePosArray,
                surroundOffset.offset(pos),
                type,
                trapped,
                fullyTrapped
            )
        }
    }

    private fun checkHole4(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos): HoleInfo? {
        if (!checkAir(holeOffsetCheck4, pos, mutablePos)) return null
        val type = checkSurroundPos(pos, mutablePos, surroundOffset4, HoleType.FOUR, HoleType.FOUR)
        return if (type == HoleType.NONE) {
            null
        } else {
            val holePosArray = holeOffset4.offset(pos)

            var trapped = false
            var fullyTrapped = true

            for (holePos in holePosArray) {
                if (mc.world.isAirBlock(mutablePos.setPos(holePos.x, holePos.y + 2, holePos.z))) {
                    fullyTrapped = false
                } else {
                    trapped = true
                }
            }
            HoleInfo(
                pos.toImmutable(),
                pos.toVec3d(1.0,
                    0.0, 1.0
                ),
                AxisAlignedBB(
                    pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(),
                    pos.x + 2.0, pos.y + 1.0, pos.z + 2.0
                ),
                holePosArray,
                surroundOffset4.offset(pos),
                type,
                trapped,
                fullyTrapped
            )
        }
    }
    private fun BlockPos.toVec3d(x: Double, y: Double, z: Double) =
        Vec3d(this.add(x, y, z))
    private fun checkSurroundPos(pos: BlockPos, mutablePos: BlockPos.MutableBlockPos, surroundOffset: Array<BlockPos>, expectType: HoleType, obbyType: HoleType): HoleType {
        var type = expectType

        for (offset in surroundOffset) {
            val blockState = mc.world.getBlockState(mutablePos.setPos(pos.x + offset.x, pos.y + offset.y, pos.z + offset.z))
            when {
                blockState.block == Blocks.BEDROCK -> continue
                blockState.block != Blocks.AIR && isResistant(blockState) -> type = obbyType
                else -> return HoleType.NONE
            }
        }

        return type
    }
    @Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "DEPRECATION")
    private fun isResistant(blockState: IBlockState) =
        !blockState.material.isLiquid && blockState.block.getExplosionResistance(null) >= 19.7
    private fun checkAir(array: Array<BlockPos>, pos: BlockPos, mutablePos: BlockPos.MutableBlockPos) =
        array.all {
            mc.world.isAirBlock(mutablePos.setPos(pos.x + it.x, pos.y + it.y, pos.z + it.z))
        }

    private fun Array<BlockPos>.offset(pos: BlockPos) =
        Array(this.size) {
            pos.add(this[it])
        }
    private val holeOffset1 = arrayOf(
        BlockPos(0, 0, 0),
    )
    private val holeOffsetCheck1 = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 1, 0)
    )
    private val surroundOffset1 = arrayOf(
        BlockPos(0, -1, 0), // Down
        BlockPos(0, 0, -1), // North
        BlockPos(1, 0, 0),  // East
        BlockPos(0, 0, 1),  // South
        BlockPos(-1, 0, 0)  // West
    )
    private val holeOffset2X = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(1, 0, 0),
    )
    private val holeOffsetCheck2X = arrayOf(
        *holeOffset2X,
        BlockPos(0, 1, 0),
        BlockPos(1, 1, 0),
    )
    private val holeOffset2Z = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 0, 1),
    )
    private val holeOffsetCheck2Z = arrayOf(
        *holeOffset2Z,
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
    )
    private val surroundOffset2X = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(1, -1, 0),
        BlockPos(-1, 0, 0),
        BlockPos(0, 0, -1),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, -1),
        BlockPos(1, 0, 1),
        BlockPos(2, 0, 0)
    )
    private val surroundOffset2Z = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(0, -1, 1),
        BlockPos(0, 0, -1),
        BlockPos(-1, 0, 0),
        BlockPos(1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(1, 0, 1),
        BlockPos(0, 0, 2)
    )
    private val holeOffset4 = arrayOf(
        BlockPos(0, 0, 0),
        BlockPos(0, 0, 1),
        BlockPos(1, 0, 0),
        BlockPos(1, 0, 1)
    )
    private val holeOffsetCheck4 = arrayOf(
        *holeOffset4,
        BlockPos(0, 1, 0),
        BlockPos(0, 1, 1),
        BlockPos(1, 1, 0),
        BlockPos(1, 1, 1)
    )
    private val surroundOffset4 = arrayOf(
        BlockPos(0, -1, 0),
        BlockPos(0, -1, 1),
        BlockPos(1, -1, 0),
        BlockPos(1, -1, 1),
        BlockPos(-1, 0, 0),
        BlockPos(-1, 0, 1),
        BlockPos(0, 0, -1),
        BlockPos(1, 0, -1),
        BlockPos(0, 0, 2),
        BlockPos(1, 0, 2),
        BlockPos(2, 0, 0),
        BlockPos(2, 0, 1)
    )
    private enum class HoleType(val size: Int) {
        NONE(0),
        OBBY(1),
        BEDROCK(1),
        TWO(2),
        FOUR(4)
    }
    private class HoleInfo(
        val origin: BlockPos,
        val center: Vec3d,
        val boundingBox: AxisAlignedBB,
        val holePos: Array<BlockPos>,
        val surroundPos: Array<BlockPos>,
        val type: HoleType,
        val isTrapped: Boolean,
        val isFullyTrapped: Boolean
    ) {
        val isHole = type != HoleType.NONE
        val isSafe = type == HoleType.BEDROCK
        val isTwo = type == HoleType.TWO
        val isFour = type == HoleType.FOUR
        fun canEnter(world: World, pos: BlockPos): Boolean {
            val headPosY = pos.y + 2
            if (origin.y >= headPosY) return false
            val box = boundingBox.expand(0.0, headPosY - origin.y - 1.0, 0.0)
            return !world.collidesWithAnyBlock(box)
        }
        override fun equals(other: Any?) =
            this === other
                    || other is HoleInfo
                    && origin == other.origin
        override fun hashCode() =
            origin.hashCode()
        companion object {
            fun empty(pos: BlockPos) =
                HoleInfo(
                    pos,
                    Vec3d.ZERO,
                    emptyAxisAlignedBB,
                    emptyBlockPosArray,
                    emptyBlockPosArray,
                    HoleType.NONE,
                    isTrapped = false,
                    isFullyTrapped = false,
                )
            private val emptyAxisAlignedBB = AxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)
            private val emptyBlockPosArray = emptyArray<BlockPos>()
        }
    }
}