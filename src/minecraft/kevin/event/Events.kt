package kevin.event

import net.minecraft.block.Block
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.inventory.ClickType
import net.minecraft.network.Packet
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.AxisAlignedBB
import net.minecraft.util.math.BlockPos

class AttackEvent(val targetEntity: Entity?) : Event()

class BlockBBEvent(blockPos: BlockPos, val block: Block, var boundingBox: AxisAlignedBB?) : Event() {
    val x = blockPos.x
    val y = blockPos.y
    val z = blockPos.z
}

class ClickBlockEvent(val clickedBlock: BlockPos?, val WEnumFacing: EnumFacing?) : Event()

class ClientShutdownEvent : Event()

data class EntityMovementEvent(val movedEntity: Entity) : Event()

class JumpEvent(var motion: Float) : CancellableEvent()

class KeyEvent(val key: Int) : Event()

class MotionEvent(val eventState: EventState) : Event()

class SlowDownEvent(var strafe: Float, var forward: Float) : Event()

class StrafeEvent(val strafe: Float, val forward: Float, val friction: Float) : CancellableEvent()

class MoveEvent(var x: Double, var y: Double, var z: Double) : CancellableEvent() {
    var isSafeWalk = false

    fun zero() {
        x = 0.0
        y = 0.0
        z = 0.0
    }

    fun zeroXZ() {
        x = 0.0
        z = 0.0
    }
}

class PacketEvent(val packet: Packet<*>) : CancellableEvent()

class PushOutEvent : CancellableEvent()

class Render2DEvent(val partialTicks: Float) : Event()

class Render3DEvent(val partialTicks: Float) : Event()

class RenderEntityEvent(val entity: Entity, val x: Double, val y: Double, val z: Double, val entityYaw: Float,
                        val partialTicks: Float) : Event()

class ScreenEvent(val guiScreen: GuiScreen?) : Event()

class StepEvent(var stepHeight: Float) : Event()

class StepConfirmEvent : Event()

class TextEvent(var text: String?) : Event()

class TickEvent : Event()

class PostTickEvent : Event()

class UpdateEvent : Event()

class WorldEvent(val worldClient: WorldClient?) : Event()

class ClickWindowEvent(val windowId: Int, val slotId: Int, val mouseButtonClicked: Int, val mode: ClickType) : CancellableEvent()

class EntityKilledEvent(val targetEntity: EntityLivingBase) : Event()

class PlayerTravelEvent : CancellableEvent()

class PlayerMoveEvent(val eventState: EventState,private val player: EntityPlayerSP? = null) : Event() {
    private val prevX = player?.motionX
    private val prevY = player?.motionY
    private val prevZ = player?.motionZ

    val isModified: Boolean
        get() = x != prevX
                || y != prevY
                || z != prevZ

    var x = Double.NaN
        get() = get(field, player?.motionX)

    var y = Double.NaN
        get() = get(field, player?.motionY)

    var z = Double.NaN
        get() = get(field, player?.motionZ)

    @Suppress("NOTHING_TO_INLINE")
    private inline fun get(x: Double, y: Double?): Double {
        return when {
            !x.isNaN() -> x
            y != null -> y
            else -> .0
        }
    }
    companion object {
        @JvmStatic
        fun PlayerMoveEventPre(player: EntityPlayerSP) = PlayerMoveEvent(EventState.PRE,player)
        @JvmStatic
        fun PlayerMoveEventPost() = PlayerMoveEvent(EventState.POST)
    }
}