package kevin.module.modules.movement

import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.module.BooleanValue
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.utils.MovementUtils
import kevin.utils.Rotation
import kevin.utils.RotationUtils
import net.minecraft.init.MobEffects
import org.lwjgl.input.Keyboard

class Sprint : Module("Sprint","Automatically sprints all the time.", Keyboard.KEY_NONE,ModuleCategory.MOVEMENT) {

    val allDirectionsValue = BooleanValue("AllDirections", true)
    private val blindnessValue = BooleanValue("Blindness", true)
    val foodValue = BooleanValue("Food", true)

    val checkServerSide: BooleanValue = BooleanValue("CheckServerSide", false)
    val checkServerSideGround: BooleanValue = BooleanValue("CheckServerSideOnlyGround", false)

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (!MovementUtils.isMoving || mc.player.isSneaking ||
            blindnessValue.get() && mc.player
                .isPotionActive(MobEffects.BLINDNESS) ||
            foodValue.get() && !(mc.player.foodStats.foodLevel > 6.0f || mc.player.capabilities.allowFlying)
            || (checkServerSide.get() && (mc.player.onGround || !checkServerSideGround.get())
                    && !allDirectionsValue.get() && RotationUtils.targetRotation != null && RotationUtils.getRotationDifference(
                Rotation(mc.player.rotationYaw, mc.player.rotationPitch)
            ) > 30)) {
            mc.player.isSprinting = false
            return
        }
        if (allDirectionsValue.get() || mc.player.movementInput.moveForward >= 0.8f) mc.player.isSprinting = true
    }
}