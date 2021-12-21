package kevin.module.modules.world

import kevin.event.EventTarget
import kevin.event.PacketEvent
import kevin.event.UpdateEvent
import kevin.module.*
import net.minecraft.network.play.server.SPacketChangeGameState
import net.minecraft.network.play.server.SPacketTimeUpdate

object World : Module("World","Change the world!",category = ModuleCategory.WORLD) {
    private val timeModeValue = ListValue("TimeMode", arrayOf("Normal","Custom","Set"), "Normal")
    private val weatherModeValue = ListValue("WeatherMode", arrayOf("Normal","Sun","Rain","Thunder"), "Normal")
    private val customWorldTimeValue = IntegerValue("CustomTime", 1000, 0, 24000)
    private val changeWorldTimeSpeedValue = IntegerValue("ChangeWorldTimeSpeed", 150, 10, 500)
    private val weatherStrengthValue = FloatValue("WeatherStrength", 1f, 0f, 1f)

    var i = 0L

    override fun onDisable() {
        i = 0
    }

    @EventTarget
    fun onUpdate(event : UpdateEvent) {
        when (timeModeValue.get()) {
            "Custom" -> {
                if (i < 24000)
                    i += changeWorldTimeSpeedValue.get()
                else
                    i = 0
                mc.world.worldTime = i
            }
            "Set" -> {
                mc.world.worldTime = customWorldTimeValue.get().toLong()
            }
        }

        when (weatherModeValue.get()){
            "Sun" -> {
                mc.world.setRainStrength(0f)
                mc.world.setThunderStrength(0f)
            }
            "Rain" -> {
                mc.world.setRainStrength(weatherStrengthValue.get())
                mc.world.setThunderStrength(0f)
            }
            "Thunder" -> {
                mc.world.setRainStrength(weatherStrengthValue.get())
                mc.world.setThunderStrength(weatherStrengthValue.get())
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent){
        val packet=event.packet

        if(!(timeModeValue equal "Normal")&&packet is SPacketTimeUpdate){
            event.cancelEvent()
        }

        if(!(weatherModeValue equal "Normal")&&packet is SPacketChangeGameState){
            if(packet.gameState in 7..8) event.cancelEvent()
        }
    }
}