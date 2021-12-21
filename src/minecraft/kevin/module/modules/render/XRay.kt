package kevin.module.modules.render

import kevin.module.ListValue
import kevin.module.Module
import kevin.module.ModuleCategory
import net.minecraft.init.Blocks

class XRay : Module(name = "XRay", description = "Allows you to see through walls.", category = ModuleCategory.RENDER) {
    override fun onToggle(state: Boolean) {
        mc.renderGlobal.loadRenderers()
    }
    val mode = ListValue("Mode", arrayOf("Simple","Translucent"),"Simple")
    override val tag: String
        get() = mode.get()
    val xrayBlocks = mutableListOf(
        Blocks.COAL_ORE,
        Blocks.IRON_ORE,
        Blocks.GOLD_ORE,
        Blocks.REDSTONE_ORE,
        Blocks.LAPIS_ORE,
        Blocks.DIAMOND_ORE,
        Blocks.EMERALD_ORE,
        Blocks.QUARTZ_ORE,
        Blocks.CLAY,
        Blocks.GLOWSTONE,
        Blocks.CRAFTING_TABLE,
        Blocks.TORCH,
        Blocks.LADDER,
        Blocks.TNT,
        Blocks.COAL_BLOCK,
        Blocks.IRON_BLOCK,
        Blocks.GOLD_BLOCK,
        Blocks.DIAMOND_BLOCK,
        Blocks.EMERALD_BLOCK,
        Blocks.REDSTONE_BLOCK,
        Blocks.LAPIS_BLOCK,
        Blocks.FIRE,
        Blocks.MOSSY_COBBLESTONE,
        Blocks.MOB_SPAWNER,
        Blocks.END_PORTAL_FRAME,
        Blocks.ENCHANTING_TABLE,
        Blocks.BOOKSHELF,
        Blocks.COMMAND_BLOCK,
        Blocks.LAVA,
        Blocks.FLOWING_LAVA,
        Blocks.WATER,
        Blocks.FLOWING_WATER,
        Blocks.FURNACE,
        Blocks.LIT_FURNACE
    )
}