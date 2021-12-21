package kevin.module.modules.movement

import kevin.event.*
import kevin.module.*
import kevin.module.modules.movement.flys.FlyMode
import kevin.module.modules.movement.flys.aac.AAC5
import kevin.module.modules.movement.flys.other.Teleport
import kevin.module.modules.movement.flys.vanilla.Creative
import kevin.module.modules.movement.flys.vanilla.Vanilla
import kevin.module.modules.movement.flys.verus.VerusAuto
import kevin.utils.*
import kevin.utils.timers.MSTimer
import org.lwjgl.input.Keyboard

class Fly : Module("Fly","Allow you fly", Keyboard.KEY_F,ModuleCategory.MOVEMENT) {
    private val flys = arrayListOf(
        Vanilla,
        Creative,
        AAC5,
        Teleport,
        VerusAuto
    )

    private val names: Array<String>
    init {
        val arrayList = arrayListOf<String>()
        flys.forEach { arrayList.add(it.modeName) }
        names = arrayList.toTypedArray()
    }

    private val nowMode: FlyMode
    get() = flys.find { mode equal it.modeName }!!

    val mode = ListValue("Mode",names,names.first())
    val speed = FloatValue("Speed",2F,0.5F,5F)
    val keepAlive = BooleanValue("KeepAlive",false)
    private val resetMotion = BooleanValue("ResetMotion",false)
    private val fakeDamageValue = BooleanValue("FakeDamage", true)

    private var isFlying = false
    val flyTimer = MSTimer()

    override fun onEnable() {
        isFlying = mc.player.capabilities.isFlying
        if(mc.player.onGround&&fakeDamageValue.get()) mc.player.handleStatusUpdate(2)
        nowMode.onEnable()
    }
    override fun onDisable() {
        mc.player.capabilities.isFlying = isFlying&&(mc.playerController.isSpectator||mc.playerController.isInCreativeMode)
        if (resetMotion.get()) {
            mc.player.motionY = 0.0
            mc.player.motionX = 0.0
            mc.player.motionZ = 0.0
        }
        nowMode.onDisable()
    }

    @EventTarget fun onMotion(event: MotionEvent) = nowMode.onMotion(event)
    @EventTarget fun onRender3D(event: Render3DEvent) = nowMode.onRender3D(event)
    @EventTarget fun onWorld(event: WorldEvent) = nowMode.onWorld(event)
    @EventTarget fun onBB(event: BlockBBEvent) = nowMode.onBB(event)
    @EventTarget fun onStep(event: StepEvent) = nowMode.onStep(event)
    @EventTarget fun onJump(event: JumpEvent) = nowMode.onJump(event)
    @EventTarget fun onUpdate(event: UpdateEvent) = nowMode.onUpdate(event)
    @EventTarget fun onPacket(event: PacketEvent) = nowMode.onPacket(event)

    override val values: List<Value<*>>
    get() {
        val valueList = arrayListOf<Value<*>>()
        valueList.addAll(super.values)
        flys.forEach { valueList.addAll(it.values) }
        return valueList.toList()
    }

    override val tag: String
        get() = mode.get()
}