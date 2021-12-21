package kevin.module.modules.player

import kevin.KevinClient
import kevin.event.*
import kevin.module.ListValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.BlockUtils.collideBlock
import kevin.utils.timers.TickTimer
import net.minecraft.block.BlockLiquid
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.CPacketPlayer
import net.minecraft.util.*
import net.minecraft.util.math.AxisAlignedBB

class NoFall : Module("NoFall","Prevents you from taking fall damage.", category = ModuleCategory.PLAYER) {
    @JvmField
    val modeValue = ListValue("Mode", arrayOf("SpoofGround", "NoGround", "Packet", "AAC", "LAAC", "AAC3.3.11", "AAC3.3.15", "Spartan", "CubeCraft", "Hypixel", "C03->C04", "Verus"), "SpoofGround")
    private val spartanTimer = TickTimer()
    private var currentState = 0
    private var jumped = false

    @EventTarget fun onBB(event: BlockBBEvent){
        if(mc.player==null) return
        if (modeValue equal "Verus") {
            if (mc.player.fallDistance>2.6&&
                !KevinClient.moduleManager.getModule("FreeCam")!!.state&&
                !KevinClient.moduleManager.getModule("Fly")!!.state&&
                !(collideBlock(mc.player!!.entityBoundingBox, fun(block: Any?) = block is BlockLiquid) || collideBlock(AxisAlignedBB(mc.player!!.entityBoundingBox.maxX, mc.player!!.entityBoundingBox.maxY, mc.player!!.entityBoundingBox.maxZ, mc.player!!.entityBoundingBox.minX, mc.player!!.entityBoundingBox.minY - 0.01, mc.player!!.entityBoundingBox.minZ), fun(block: Any?) = block is BlockLiquid))){
                if (event.block== Blocks.AIR&&
                    event.y < mc.player!!.posY&&
                    mc.player.getDistance(event.x.toDouble(),event.y.toDouble(),event.z.toDouble()) < 1.5) event.boundingBox =
                    AxisAlignedBB(
                        event.x.toDouble(),
                        event.y.toDouble(),
                        event.z.toDouble(),
                        event.x + 1.0,
                        (mc.player.posY/0.125).toInt()*0.125,
                        event.z + 1.0
                    )
            }
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onUpdate(event: UpdateEvent?) {

        //if (event!!.eventState == UpdateState.OnUpdate) return

        if (mc.player!!.onGround)
            jumped = false

        if (mc.player!!.motionY > 0)
            jumped = true

        if (!this.state || KevinClient.moduleManager.getModule("FreeCam")!!.state)
            return

        if (collideBlock(mc.player!!.entityBoundingBox, fun(block: Any?) = block is BlockLiquid) ||
            collideBlock(AxisAlignedBB(mc.player!!.entityBoundingBox.maxX, mc.player!!.entityBoundingBox.maxY, mc.player!!.entityBoundingBox.maxZ, mc.player!!.entityBoundingBox.minX, mc.player!!.entityBoundingBox.minY - 0.01, mc.player!!.entityBoundingBox.minZ), fun(block: Any?) = block is BlockLiquid))
            return

        when (modeValue.get().toLowerCase()) {
            "packet" -> {
                if (mc.player!!.fallDistance > 2f) {
                    mc.connection!!.sendPacket(CPacketPlayer(true))
                }
            }
            "cubecraft" -> if (mc.player!!.fallDistance > 2f) {
                mc.player!!.onGround = false
                mc.player!!.connection.sendPacket(CPacketPlayer(true))
            }
            "aac" -> {
                if (mc.player!!.fallDistance > 2f) {
                    mc.connection!!.sendPacket(CPacketPlayer(true))
                    currentState = 2
                } else if (currentState == 2 && mc.player!!.fallDistance < 2) {
                    mc.player!!.motionY = 0.1
                    currentState = 3
                    return
                }
                when (currentState) {
                    3 -> {
                        mc.player!!.motionY = 0.1
                        currentState = 4
                    }
                    4 -> {
                        mc.player!!.motionY = 0.1
                        currentState = 5
                    }
                    5 -> {
                        mc.player!!.motionY = 0.1
                        currentState = 1
                    }
                }
            }
            "laac" -> if (!jumped && mc.player!!.onGround && !mc.player!!.isOnLadder && !mc.player!!.isInWater
                && !mc.player!!.isInWeb) mc.player!!.motionY = (-6).toDouble()
            "aac3.3.11" -> if (mc.player!!.fallDistance > 2) {
                mc.player!!.motionZ = 0.0
                mc.player!!.motionX = mc.player!!.motionZ
                mc.connection!!.sendPacket(CPacketPlayer.Position(mc.player!!.posX,
                    mc.player!!.posY - 10E-4, mc.player!!.posZ, mc.player!!.onGround))
                mc.connection!!.sendPacket(CPacketPlayer(true))
            }
            "aac3.3.15" -> if (mc.player!!.fallDistance > 2) {
                if (!mc.isIntegratedServerRunning) mc.connection!!.sendPacket(CPacketPlayer.Position(mc.player!!.posX, Double.NaN, mc.player!!.posZ, false))
                mc.player!!.fallDistance = (-9999).toFloat()
            }
            "spartan" -> {
                spartanTimer.update()
                if (mc.player!!.fallDistance > 1.5 && spartanTimer.hasTimePassed(10)) {
                    mc.connection!!.sendPacket(CPacketPlayer.Position(mc.player!!.posX,
                        mc.player!!.posY + 10, mc.player!!.posZ, true))
                    mc.connection!!.sendPacket(CPacketPlayer.Position(mc.player!!.posX,
                        mc.player!!.posY - 10, mc.player!!.posZ, true))
                    spartanTimer.reset()
                }
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val mode = modeValue.get()
        if (packet is CPacketPlayer) {
            if (mode.equals("SpoofGround", ignoreCase = true)) packet.onGround = true
            if (mode.equals("NoGround", ignoreCase = true)) packet.onGround = false
            if (mode.equals("Hypixel", ignoreCase = true)
                && mc.player != null && mc.player!!.fallDistance > 1.5) packet.onGround =
                mc.player!!.ticksExisted % 2 == 0
            if (mode.equals("C03->C04",ignoreCase = true)){
                if (
                    mc.player!!.capabilities.isFlying ||
                    Minecraft.getMinecraft().player.capabilities.disableDamage ||
                    mc.player!!.motionY >= 0.0 ||
                    testPackets.contains(packet) ||
                    testPackets.contains(packet)
                ) return
                if (packet.moving) {
                    if (mc.player!!.fallDistance > 2.0f && isBlockUnder()) {
                        event.cancelEvent()
                        sendPacketNoEvent(
                            CPacketPlayer.Position(
                                packet.x,
                                packet.y,
                                packet.z,
                                packet.onGround
                            )
                        )
                    }
                }
            }
        }
    }

    override fun onDisable() {
        testPackets.clear()
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.player == null) return
        if (collideBlock(mc.player!!.entityBoundingBox, fun(block: Any?) = block is BlockLiquid) || collideBlock(AxisAlignedBB(mc.player!!.entityBoundingBox.maxX, mc.player!!.entityBoundingBox.maxY, mc.player!!.entityBoundingBox.maxZ, mc.player!!.entityBoundingBox.minX, mc.player!!.entityBoundingBox.minY - 0.01, mc.player!!.entityBoundingBox.minZ), fun(block: Any?) = block is BlockLiquid))
            return

        if (modeValue.get().equals("laac", ignoreCase = true)) {
            if (!jumped && !mc.player!!.onGround && !mc.player!!.isOnLadder && !mc.player!!.isInWater && !mc.player!!.isInWeb && mc.player!!.motionY < 0.0) {
                event.x = 0.0
                event.z = 0.0
            }
        }
    }

    private val testPackets = arrayListOf<Packet<INetHandlerPlayServer>>()

    private fun sendPacketNoEvent(packet: Packet<INetHandlerPlayServer>){
        testPackets.add(packet)
        mc.connection!!.sendPacket(packet)
    }

    private fun isBlockUnder(): Boolean{
        for (y in 0..((mc.player?.posY?:return false) + (mc.player?.eyeHeight ?:return false)).toInt()){
            val boundingBox = mc.player!!.entityBoundingBox.offset(0.0,-y.toDouble(),0.0)
            if (mc.world!!.getCollisionBoxes(mc.player!!,boundingBox).isNotEmpty()) return true
        }
        return false
    }

    @EventTarget
    private fun onMotionUpdate(event: MotionEvent) {
        if (modeValue.get() == "C03->C04" && event.eventState == EventState.PRE){
            if (
                mc.player!!.capabilities.isFlying ||
                Minecraft.getMinecraft().player.capabilities.disableDamage ||
                mc.player!!.motionY >= 0.0
            ) return
            if (mc.player!!.fallDistance > 3.0f)
                if (isBlockUnder())
                    sendPacketNoEvent(CPacketPlayer(true))
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onJump(event: JumpEvent?) {
        jumped = true
    }

    override val tag: String
        get() = modeValue.get()
}