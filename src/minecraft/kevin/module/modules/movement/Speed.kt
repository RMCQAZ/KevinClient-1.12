package kevin.module.modules.movement

import kevin.event.*
import kevin.module.*
import kevin.module.modules.movement.speeds.SpeedMode
import kevin.module.modules.movement.speeds.aac.AAC5Fast
import kevin.module.modules.movement.speeds.aac.AAC5Long
import kevin.module.modules.movement.speeds.other.AutoJump
import kevin.module.modules.movement.speeds.other.Troll
import kevin.module.modules.movement.speeds.other.YPort
import kevin.module.modules.movement.speeds.verus.VerusHop
import kevin.module.modules.movement.speeds.verus.VerusYPort
import kevin.utils.MovementUtils
import net.minecraft.network.play.server.SPacketEntityVelocity

class Speed : Module("Speed","Allows you to move faster.", category = ModuleCategory.MOVEMENT) {
    private val speeds = arrayListOf(
        AAC5Long,
        AAC5Fast,
        Troll,
        YPort,
        AutoJump,
        VerusYPort,
        VerusHop
    )

    private val names: Array<String>
    init {
        val arrayList = arrayListOf<String>()
        speeds.forEach { arrayList.add(it.modeName) }
        names = arrayList.toTypedArray()
    }

    private val mode = ListValue("Mode",names,names.first())
    private val keepSprint = BooleanValue("KeepSprint",false)
    private val antiKnockback = BooleanValue("AntiKnockBack",false)
    private val antiKnockbackLong = FloatValue("AntiKnockBackLong",0F,0.00F,1.00F)
    private val antiKnockbackHigh = FloatValue("AntiKnockBackHigh",1F,0.00F,1.00F)

    override val tag: String
        get() = mode.get()

    private val nowMode: SpeedMode
    get() = speeds.find { mode equal it.modeName }!!

    override fun onDisable() {
        mc.timer.timerSpeed = 1F
        mc.player.speedInAir = 0.02F
        nowMode.onDisable()
    }
    override fun onEnable() = nowMode.onEnable()
    @EventTarget fun onPlayerMove(event: PlayerMoveEvent) = nowMode.onPlayerMove(event)
    @EventTarget fun onPostTick(event: PostTickEvent) = nowMode.onPostTick()
    @EventTarget fun onPlayerTravel(event: PlayerTravelEvent) = nowMode.onPlayerTravel()
    @EventTarget fun onMove(event: MoveEvent) = nowMode.onMove(event)
    @EventTarget fun onUpdate(event: UpdateEvent) = nowMode.onUpdate(event)
    @EventTarget fun onMotion(event: MotionEvent) {
        if (mc.player.isSneaking || event.eventState != EventState.PRE) return
        if (MovementUtils.isMoving && keepSprint.get()) mc.player.isSprinting = true
        nowMode.onPreMotion()
    }
    @EventTarget
    fun onPacket(event: PacketEvent){
        nowMode.onPacket(event)
        if (event.packet is SPacketEntityVelocity && antiKnockback.get()) {
            val thePlayer = mc.player ?: return
            val packet = event.packet
            if ((mc.world?.getEntityByID(packet.entityID) ?: return) != thePlayer) return
            val horizontal = antiKnockbackLong.get()
            val vertical = antiKnockbackHigh.get()
            if (horizontal == 0F && vertical == 0F) event.cancelEvent()
            packet.motionX = (packet.motionX * horizontal).toInt()
            packet.motionY = (packet.motionY * vertical).toInt()
            packet.motionZ = (packet.motionZ * horizontal).toInt()
        }
    }
    override val values: List<Value<*>>
    get(){
        val valueList = arrayListOf<Value<*>>()
        valueList.addAll(super.values)
        speeds.forEach { valueList.addAll(it.values) }
        return valueList.toList()
    }
}