package kevin.module.modules.render

import kevin.KevinClient
import kevin.module.Module
import kevin.module.ModuleCategory
import org.lwjgl.input.Keyboard

class CapeManager : Module("CapeManager","Cape manager.",Keyboard.KEY_RMENU,category = ModuleCategory.RENDER) {
    override fun onEnable() {
        mc.displayGuiScreen(KevinClient.capeManager)
        state = false
    }
}