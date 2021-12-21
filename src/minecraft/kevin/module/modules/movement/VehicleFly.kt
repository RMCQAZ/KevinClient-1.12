package kevin.module.modules.movement

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.module.*
import kevin.utils.MovementUtils
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemBoat
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.SPacketMoveVehicle
import net.minecraft.network.play.server.SPacketPlayerPosLook
import net.minecraft.util.EnumHand
import net.minecraft.util.math.Vec3d
import kotlin.math.cos
import kotlin.math.sin


class VehicleFly : Module("VehicleFly","Fly with your vehicle.",category = ModuleCategory.MOVEMENT) {

    private val speedVertical = FloatValue("Vertical", 3f, 0.1f,10f)
    private val speedHorizontal = FloatValue("Horizontal", 3f, 0.1f,10f)
    private val noClip = BooleanValue("NoClip",true)
    private val slowDown = BooleanValue("SlowDown",false)
    private val placeBypass = BooleanValue("PlaceBypass",false)
    private val bypass = BooleanValue("Bypass",false)
    private val packet = IntegerValue("Packet",0,0,5)
    private val interact = IntegerValue("Interact",0,0,20)

    private var teleportID = 0
    private var packetCounter = 0

    override val tag: String
        get() = "H:${speedHorizontal.get()} V:${speedVertical.get()}"

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        val vehicle = mc.player.ridingEntity ?: return
        vehicle.setNoGravity(true)
        vehicle.onGround = false
        if (noClip.get()) vehicle.noClip = true
        vehicle.motionY = when {
            mc.gameSettings.keyBindJump.isKeyDown -> speedVertical.get().toDouble() / 2.0
            mc.gameSettings.keyBindSprint.isKeyDown -> -(speedVertical.get().toDouble() / 2.0)
            else -> 0.0
        }
        vehicle.rotationYaw = mc.player.rotationYaw
        if (vehicle is EntityBoat) vehicle.deltaRotation = 0F
        val speed = directionSpeed(speedHorizontal.get() / 2.0)
        vehicle.motionX = if (MovementUtils.isMoving) speed[0] else 0.0
        vehicle.motionZ = if (MovementUtils.isMoving) speed[1] else 0.0
        if (slowDown.get()) {
            if (mc.gameSettings.keyBindJump.isKeyDown) {
                if (mc.player.ticksExisted % 8 < 2) {
                    vehicle.motionY = -0.04
                }
            } else if (mc.player.ticksExisted % 8 < 4) {
                vehicle.motionY = -0.08
            }
        }
        handlePackets(vehicle.motionX, vehicle.motionY, vehicle.motionZ)
    }
    @EventTarget
    fun onPacket(event: PacketEvent){
        if (mc.world != null && mc.player != null && mc.player.getRidingEntity() != null) {
            if (bypass.get() && event.packet is CPacketInput && !mc.gameSettings.keyBindSneak.isKeyDown && !mc.player.getRidingEntity()!!.onGround) {
                ++packetCounter
                if (packetCounter == 3) {
                    NCPPacketTrick()
                }
            }
            if (bypass.get() && event.packet is SPacketPlayerPosLook || event.packet is SPacketMoveVehicle) {
                event.cancelEvent()
            }
        }
        if (event.packet is CPacketVehicleMove && mc.player.isRiding && interact.get() != 0 && mc.player.ticksExisted % interact.get() == 0) {
            mc.playerController.interactWithEntity(
                mc.player as EntityPlayer,
                mc.player.ridingEntity,
                EnumHand.OFF_HAND
            )
        }
        if (placeBypass.get() && event.packet is CPacketPlayerTryUseItemOnBlock && (mc.player.heldItemMainhand.item is ItemBoat || mc.player.heldItemOffhand.item is ItemBoat)
        ){
            event.cancelEvent()
        }
        if (event.packet is SPacketPlayerPosLook) {
            teleportID = event.packet.teleportId
        }
    }
    private fun directionSpeed(speed: Double): DoubleArray {
        var forward: Float = mc.player.movementInput.moveForward
        var side: Float = mc.player.movementInput.moveStrafe
        var yaw: Float = mc.player.prevRotationYaw + (mc.player.rotationYaw - mc.player.prevRotationYaw) * mc.renderPartialTicks
        if (forward != 0.0f) {
            if (side > 0.0f) {
                yaw += (if (forward > 0.0f) -45 else 45).toFloat()
            } else if (side < 0.0f) {
                yaw += (if (forward > 0.0f) 45 else -45).toFloat()
            }
            side = 0.0f
            if (forward > 0.0f) {
                forward = 1.0f
            } else if (forward < 0.0f) {
                forward = -1.0f
            }
        }
        val sin = sin(Math.toRadians((yaw + 90.0f).toDouble()))
        val cos = cos(Math.toRadians((yaw + 90.0f).toDouble()))
        val posX = forward.toDouble() * speed * cos + side.toDouble() * speed * sin
        val posZ = forward.toDouble() * speed * sin - side.toDouble() * speed * cos
        return doubleArrayOf(posX, posZ)
    }
    private fun handlePackets(x: Double, y: Double, z: Double) {
        if (packet.get()!=0&&mc.player.getRidingEntity() != null) {
            val vec = Vec3d(x, y, z)
            val position = mc.player.getRidingEntity()!!.positionVector.add(vec)
            mc.player.getRidingEntity()!!.setPosition(position.x, position.y, position.z)
            mc.player.connection.sendPacket(CPacketVehicleMove(mc.player.getRidingEntity()!!))
            for (i in 0 until this.packet.get()) {
                mc.player.connection.sendPacket(CPacketConfirmTeleport(teleportID++))
            }
        }
    }
    private fun NCPPacketTrick() {
        packetCounter = 0
        mc.player.getRidingEntity()!!.dismountRidingEntity()
        val l_Entity = mc.world.loadedEntityList.stream().toArray()
            .filterIsInstance<EntityBoat>()
            .minByOrNull { mc.player.getDistanceToEntity(it) }
        if (l_Entity != null) {
            mc.playerController.interactWithEntity(mc.player, l_Entity, EnumHand.MAIN_HAND)
        }
    }
}