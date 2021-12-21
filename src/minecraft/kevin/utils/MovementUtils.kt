package kevin.utils

import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.MobEffects
import java.lang.Math.*

object MovementUtils : Mc() {
    val speed: Float
        get() = kotlin.math.sqrt(mc.player!!.motionX * mc.player!!.motionX + mc.player!!.motionZ * mc.player!!.motionZ)
            .toFloat()

    @JvmStatic
    val isMoving: Boolean
        get() = mc.player != null && (mc.player!!.movementInput.moveForward != 0f || mc.player!!.movementInput.moveStrafe != 0f)

    fun hasMotion(): Boolean {
        return mc.player!!.motionX != 0.0 && mc.player!!.motionZ != 0.0 && mc.player!!.motionY != 0.0
    }

    fun calcMoveYaw(yawIn: Float = mc.player.rotationYaw, moveForward: Float = roundedForward, moveString: Float = roundedStrafing): Double {
        var strafe = 90 * moveString
        strafe *= if (moveForward != 0F) moveForward * 0.5F else 1F

        var yaw = yawIn - strafe
        yaw -= if (moveForward < 0F) 180 else 0

        return Math.toRadians(yaw.toDouble())
    }

    private inline val roundedForward get() = getRoundedMovementInput(mc.player.movementInput.moveForward)
    private inline val roundedStrafing get() = getRoundedMovementInput(mc.player.movementInput.moveStrafe)

    private fun getRoundedMovementInput(input: Float) = when {
        input > 0f -> 1f
        input < 0f -> -1f
        else -> 0f
    }

    inline fun EntityLivingBase.applySpeedPotionEffects(speed: Double): Double {
        return this.getActivePotionEffect(MobEffects.SPEED)?.let {
            speed * this.speedEffectMultiplier
        } ?: speed
    }

    fun EntityLivingBase.applyJumpBoostPotionEffects(motion: Double): Double {
        return this.getActivePotionEffect(MobEffects.JUMP_BOOST)?.let {
            motion + (it.amplifier + 1.0) * 0.2
        } ?: motion
    }

    inline val EntityLivingBase.speedEffectMultiplier: Double
        get() = this.getActivePotionEffect(MobEffects.SPEED)?.let {
            1.0 + (it.amplifier + 1.0) * 0.2
        } ?: 1.0

    @JvmStatic
    @JvmOverloads
    fun strafe(speed: Float = this.speed) {
        if (!isMoving) return
        val yaw = direction
        val thePlayer = mc.player!!
        thePlayer.motionX = -kotlin.math.sin(yaw) * speed
        thePlayer.motionZ = kotlin.math.cos(yaw) * speed
    }

    @JvmStatic
    fun forward(length: Double) {
        val thePlayer = mc.player!!
        val yaw = toRadians(thePlayer.rotationYaw.toDouble())
        thePlayer.setPosition(thePlayer.posX + -sin(yaw) * length, thePlayer.posY, thePlayer.posZ + cos(yaw) * length)
    }

    @JvmStatic
    val direction: Double
        get() {
            val thePlayer = mc.player!!
            var rotationYaw = thePlayer.rotationYaw
            if (thePlayer.moveForward < 0f) rotationYaw += 180f
            var forward = 1f
            if (thePlayer.moveForward < 0f) forward = -0.5f else if (thePlayer.moveForward > 0f) forward = 0.5f
            if (thePlayer.moveStrafing > 0f) rotationYaw -= 90f * forward
            if (thePlayer.moveStrafing < 0f) rotationYaw += 90f * forward
            return toRadians(rotationYaw.toDouble())
        }
}