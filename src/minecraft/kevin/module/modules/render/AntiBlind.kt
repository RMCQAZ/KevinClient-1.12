package kevin.module.modules.render

import kevin.module.BooleanValue
import kevin.module.FloatValue
import kevin.module.Module
import kevin.module.ModuleCategory

class AntiBlind : Module(name = "AntiBlind", description = "Cancels blindness effects.", category = ModuleCategory.RENDER) {
    val confusionEffect = BooleanValue("Confusion", true)
    val pumpkinEffect = BooleanValue("Pumpkin", true)
    val fireEffect = FloatValue("Fire", 0.9F,0F,1F)
}