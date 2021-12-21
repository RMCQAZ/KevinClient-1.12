package kevin.module.modules.movement

import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.module.Module
import kevin.module.ModuleCategory
import net.minecraft.client.settings.GameSettings

class AutoWalk : Module("AutoWalk","Automatic walk.", category = ModuleCategory.MOVEMENT) {
    @EventTarget
    fun onUpdate(event: UpdateEvent){
        mc.gameSettings.keyBindForward.pressed = true
        mc.player!!.movementInput.moveForward = 0.001F
    }

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindForward))
            mc.gameSettings.keyBindForward.pressed = false
    }
}