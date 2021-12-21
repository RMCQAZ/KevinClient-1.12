package kevin.utils

import net.minecraft.client.Minecraft

open class Mc {
     companion object {
        @JvmStatic
        val mc: Minecraft = Minecraft.getMinecraft()
    }
}