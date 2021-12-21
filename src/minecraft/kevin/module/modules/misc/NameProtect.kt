package kevin.module.modules.misc

import kevin.KevinClient
import kevin.event.EventTarget
import kevin.event.TextEvent
import kevin.module.Module
import kevin.module.ModuleCategory
import kevin.module.TextValue
import kevin.utils.ColorUtils.translateAlternateColorCodes
import kevin.utils.StringUtils

class NameProtect : Module(name = "NameProtect", description = "Changes playernames clientside.", category = ModuleCategory.MISC) {

    private val fakeNameValue = TextValue("FakeName", "&cKevinUser")

    @EventTarget
    fun onText(event: TextEvent) {
        val thePlayer = mc.player

        if (thePlayer == null || event.text!!.startsWith("§l§7[§l§9${KevinClient.clientName}§l§7]"))
            return

        event.text = StringUtils.replace(event.text, thePlayer.name, translateAlternateColorCodes(fakeNameValue.get()) + "§f")
    }
}