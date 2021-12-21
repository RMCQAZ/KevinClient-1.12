package kevin.module.modules.misc

import kevin.event.EventTarget
import kevin.event.UpdateEvent
import kevin.file.FileManager
import kevin.module.BooleanValue
import kevin.module.Module
import net.minecraft.entity.item.EntityMinecartChest
import net.minecraft.tileentity.*
import net.minecraft.util.math.BlockPos
import java.text.SimpleDateFormat

class ChestFinder : Module("ChestFinder","Find chests and save position.") {
    private val chestValue = BooleanValue("Chest", false)
    private val enderChestValue = BooleanValue("EnderChest", false)
    private val furnaceValue = BooleanValue("Furnace", false)
    private val dispenserValue = BooleanValue("Dispenser", false)
    private val hopperValue = BooleanValue("Hopper", false)
    private val minecartChest = BooleanValue("MinecartChest",false)
    private val shulkerBoxValue = BooleanValue("ShulkerBox", true)

    private val foundStorages = HashSet<BlockPos>()

    private val file = FileManager.findChestsFile

    override fun onEnable() {
        if (!file.exists())
            file.createNewFile()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent){
        for (tileEntity in mc.world!!.loadedTileEntityList){
            val pos = tileEntity.pos
            if (pos in foundStorages) continue
            when(tileEntity){
                is TileEntityChest -> if(chestValue.get()) write("Chest",pos)
                is TileEntityEnderChest -> if (enderChestValue.get()) write("EnderChest",pos)
                is TileEntityFurnace -> if (furnaceValue.get()) write("Furnace",pos)
                is TileEntityDispenser -> if (dispenserValue.get()) write("Dispenser",pos)
                is TileEntityHopper -> if (hopperValue.get()) write("Hopper",pos)
                is TileEntityShulkerBox -> if (shulkerBoxValue.get()) write("ShulkerBox",pos)
            }
            foundStorages.add(pos)
        }
        if (!minecartChest.get()) return
        for (entityMinecartChest in mc.world!!.loadedEntityList.filterIsInstance<EntityMinecartChest>()){
            val pos = entityMinecartChest.position
            if (pos in foundStorages) continue
            write("MinecartChest",entityMinecartChest.position)
            foundStorages.add(pos)
        }
    }
    private fun write(type: String,pos: BlockPos) = file.appendText("[${SimpleDateFormat("HH:mm:ss").format(System.currentTimeMillis())}] Found a <$type> at X:${pos.x},Y:${pos.y},Z:${pos.z}\n")
}