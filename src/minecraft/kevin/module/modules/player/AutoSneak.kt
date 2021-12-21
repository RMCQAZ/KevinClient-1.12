package kevin.module.modules.player

import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.module.Module
import kevin.module.ModuleCategory
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks
import net.minecraft.util.math.BlockPos

class AutoSneak : Module("AutoSneak", description = "Automatically sneak at the edge of the block.", category = ModuleCategory.PLAYER) {
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.player ?: return
        if (mc.player.onGround)
            mc.gameSettings.keyBindSneak.pressed = mc.world!!.getBlockState(BlockPos(thePlayer.posX, thePlayer.posY - 1.0, thePlayer.posZ)).block == Blocks.AIR
    }

    override fun onDisable() {
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak))
            mc.gameSettings.keyBindSneak.pressed = false
    }
}