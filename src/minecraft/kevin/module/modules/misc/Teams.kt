package kevin.module.modules.misc

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.event.WorldEvent
import kevin.hud.element.elements.Notification
import kevin.module.*
import kevin.utils.BlockUtils.getBlock
import kevin.utils.timers.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos
import java.util.concurrent.CopyOnWriteArrayList

class Teams : Module("Teams","Prevents Killaura from attacking team mates.", category = ModuleCategory.MISC) {
    private val scoreboardValue = BooleanValue("ScoreboardTeam", true)
    private val colorValue = BooleanValue("Color", true)
    private val gommeSWValue = BooleanValue("GommeSW", false)

    val bedCheckValue = BooleanValue("BedCheck",true)
    private val bedCheckWaitTime = IntegerValue("BedCheckWaitTime",1000,500,5000)
    private val bedCheckXRange = IntegerValue("BedCheckXRange",50,10,128)
    private val bedCheckYRange = IntegerValue("BedCheckYRange",50,10,128)
    private val bedCheckZRange = IntegerValue("BedCheckZRange",50,10,128)
    private val bedCheckState = TextValue("BedCheckState","")
    var teamBed = CopyOnWriteArrayList<BlockPos>()
    private var needCheck = true
    private val waitTimer = MSTimer()
    private var thread:Thread? = null

    fun isInYourTeam(entity: EntityLivingBase): Boolean {
        val thePlayer = mc.player ?: return false

        if (scoreboardValue.get() && thePlayer.team != null && entity.team != null &&
            thePlayer.team!!.isSameTeam(entity.team!!))
            return true

        val displayName = thePlayer.displayName

        if (gommeSWValue.get() && displayName != null && entity.displayName != null) {
            val targetName = entity.displayName!!.formattedText.replace("§r", "")
            val clientName = displayName.formattedText.replace("§r", "")
            if (targetName.startsWith("T") && clientName.startsWith("T"))
                if (targetName[1].isDigit() && clientName[1].isDigit())
                    return targetName[1] == clientName[1]
        }

        if (colorValue.get() && displayName != null && entity.displayName != null) {
            val targetName = entity.displayName!!.formattedText.replace("§r", "")
            val clientName = displayName.formattedText.replace("§r", "")
            return targetName.startsWith("§${clientName[1]}")
        }

        return false
    }
    @EventTarget
    fun onWorld(event: WorldEvent){
        if (event.worldClient==null) return
        if (!bedCheckValue.get()){
            bedCheckState.set("Bed check is disable.")
            teamBed.clear()
            needCheck = false
            return
        }
        if (mc.isIntegratedServerRunning) {
            bedCheckState.set("Integrated server running.")
            teamBed.clear()
            needCheck = false
            return
        }
        teamBed.clear()
        needCheck = true
        waitTimer.reset()
    }
    @EventTarget
    fun onUpdate(event: UpdateEvent){
        if (needCheck&&waitTimer.hasTimePassed(bedCheckWaitTime.get().toLong())&&bedCheckValue.get()&&(thread == null || !thread!!.isAlive)){

            thread = Thread({
                val bedList = ArrayList<BlockPos>()
                val player = mc.player
                val px = player.posX.toInt()
                val py = player.posY.toInt()
                val pz = player.posZ.toInt()
                for (x in -bedCheckXRange.get() until bedCheckXRange.get()) {
                    for (z in -bedCheckZRange.get() until bedCheckZRange.get()){
                        for (y in bedCheckYRange.get() downTo -bedCheckYRange.get()+1){
                            val blockPos = BlockPos(px+x,py+y,pz+z)
                            val block = getBlock(blockPos)
                            if (block == Blocks.BED) bedList.add(blockPos)
                        }
                    }
                }
                waitTimer.reset()
                teamBed.clear()
                bedList.sortBy { mc.player.getDistance(it.x.toDouble(),it.y.toDouble(),it.z.toDouble()) }
                if (bedList.isNotEmpty()){
                    teamBed.add(bedList.first())
                    val x = bedList.first().x
                    val y = bedList.first().y
                    val z = bedList.first().z
                    val blockPosList = arrayListOf(
                        BlockPos(x+1,y,z),
                        BlockPos(x-1,y,z),
                        BlockPos(x,y,z+1),
                        BlockPos(x,y,z-1)
                    )
                    teamBed.addAll(blockPosList.filter { getBlock(it) == Blocks.BED })
                }
                if (teamBed.isEmpty()){
                    bedCheckState.set("No bed fond.")
                    //KevinClient.hud.addNotification(Notification("No bed fond."),"Bed Checker")
                } else {
                    val pos = teamBed.first()
                    bedCheckState.set("Fond team bed at X:${pos.x} Y:${pos.y} Z:${pos.z}.")
                    KevinClient.hud.addNotification(Notification("Fond team bed at X:${pos.x} Y:${pos.y} Z:${pos.z}."),"Bed Checker")
                    needCheck = false
                }
            },"BedCheckerThread")

            thread!!.start()
        }
    }
}