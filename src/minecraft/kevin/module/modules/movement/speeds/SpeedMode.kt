package kevin.module.modules.movement.speeds

import kevin.KevinClient
import kevin.event.MoveEvent
import kevin.event.PacketEvent
import kevin.event.PlayerMoveEvent
import kevin.event.UpdateEvent
import kevin.module.Value
import kevin.module.modules.movement.Speed
import kevin.utils.ClassUtils
import kevin.utils.Mc

abstract class SpeedMode(val modeName: String): Mc() {
    protected val valuePrefix = "$modeName-"
    protected val speed by lazy { KevinClient.moduleManager.getModule("Speed") as Speed }
    open val values: List<Value<*>>
        get() = ClassUtils.getValues(this.javaClass,this)
    open fun onEnable() {}
    open fun onDisable() {}

    open fun onMove(event: MoveEvent) {}
    open fun onUpdate(event: UpdateEvent) {}
    open fun onPreMotion() {}
    open fun onPacket(event: PacketEvent) {}
    open fun onPostTick() {}
    open fun onPlayerTravel() {}
    open fun onPlayerMove(event: PlayerMoveEvent) {}
}