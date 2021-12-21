package kevin.module.modules.render

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.Render3DEvent
import kevin.event.UpdateEvent
import kevin.module.*
import kevin.module.modules.misc.Teams
import kevin.utils.BlockUtils.getBlock
import kevin.utils.BlockUtils.getBlockName
import kevin.utils.ColorUtils.rainbow
import kevin.utils.RenderUtils
import kevin.utils.timers.MSTimer
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import java.awt.Color
import java.util.ArrayList

class BlockESP : Module("BlockESP", "Allows you to see a selected block through walls.", category = ModuleCategory.RENDER) {
    private val modeValue = ListValue("Mode", arrayOf("Box", "2D"), "Box")
    private val blockValue = BlockValue("Block", 168)
    private val radiusValue = IntegerValue("Radius", 40, 5, 120)
    private val blockLimitValue = IntegerValue("BlockLimit", 256, 0, 2056)
    private val colorRedValue = IntegerValue("R", 255, 0, 255)
    private val colorGreenValue = IntegerValue("G", 179, 0, 255)
    private val colorBlueValue = IntegerValue("B", 72, 0, 255)
    private val colorRainbow = BooleanValue("Rainbow", false)
    private val searchTimer = MSTimer()
    private val posList: MutableList<BlockPos> = ArrayList()
    private var thread: Thread? = null

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (searchTimer.hasTimePassed(1000L) && (thread == null || !thread!!.isAlive)) {
            val radius = radiusValue.get()
            val selectedBlock = Block.getBlockById(blockValue.get())

            if (selectedBlock == null || selectedBlock == Blocks.AIR)
                return

            thread = Thread({
                val blockList: MutableList<BlockPos> = ArrayList()

                for (x in -radius until radius) {
                    for (y in radius downTo -radius + 1) {
                        for (z in -radius until radius) {
                            val thePlayer = mc.player!!

                            val xPos = thePlayer.posX.toInt() + x
                            val yPos = thePlayer.posY.toInt() + y
                            val zPos = thePlayer.posZ.toInt() + z

                            val blockPos = BlockPos(xPos, yPos, zPos)
                            val block = getBlock(blockPos)

                            if (block == selectedBlock && blockList.size < blockLimitValue.get()) blockList.add(blockPos)
                        }
                    }
                }
                searchTimer.reset()

                synchronized(posList) {
                    posList.clear()
                    posList.addAll(blockList)
                }
            }, "BlockESP-BlockFinder")

            thread!!.start()
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent?) {
        synchronized(posList) {
            val teams = KevinClient.moduleManager.getModule("Teams") as Teams
            for (blockPos in posList) {
                val color = if (teams.bedCheckValue.get()&&blockPos in teams.teamBed) Color.green else if (colorRainbow.get()) rainbow() else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())
                when (modeValue.get().toLowerCase()) {
                    "box" -> RenderUtils.drawBlockBox(blockPos, color, true)
                    "2d" -> RenderUtils.draw2D(blockPos, color.rgb, Color.BLACK.rgb)
                }
            }
        }
    }

    override val tag: String
        get() = getBlockName(blockValue.get())
}