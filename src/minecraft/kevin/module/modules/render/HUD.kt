package kevin.module.modules.render

import kevin.KevinClient
import kevin.event.*
import kevin.hud.designer.GuiHudDesigner
import kevin.module.Module
import kevin.module.ModuleCategory

class HUD : Module("HUD","Toggles visibility of the HUD.",category = ModuleCategory.RENDER) {
    @EventTarget
    fun onRender2D(event: Render2DEvent?) {
        if ((mc.currentScreen) is GuiHudDesigner)
            return

        KevinClient.hud.render(false)
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {

        //if (event!!.eventState == UpdateState.OnUpdate) return

        KevinClient.hud.update()
    }

    @EventTarget
    fun onKey(event: KeyEvent) {
        KevinClient.hud.handleKey('a', event.key)
    }
}