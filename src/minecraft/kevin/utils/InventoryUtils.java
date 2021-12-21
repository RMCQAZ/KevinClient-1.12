package kevin.utils;

import kevin.utils.timers.MSTimer;
import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.List;

public final class InventoryUtils extends Mc {

    public static final MSTimer CLICK_TIMER = new MSTimer();
    public static final List<Block> BLOCK_BLACKLIST = Arrays.asList(
            Blocks.CHEST, Blocks.ENDER_CHEST, Blocks.TRAPPED_CHEST, Blocks.ANVIL, Blocks.SAND, Blocks.WEB, Blocks.TORCH,
            Blocks.CRAFTING_TABLE, Blocks.FURNACE, Blocks.WATERLILY, Blocks.DISPENSER, Blocks.STONE_PRESSURE_PLATE, Blocks.WOODEN_PRESSURE_PLATE,
            Blocks.NOTEBLOCK, Blocks.DROPPER, Blocks.TNT, Blocks.STANDING_BANNER,Blocks.WALL_BANNER, Blocks.REDSTONE_TORCH
    );

    public static int findItem(final int startSlot, final int endSlot, final Item item) {
        for (int i = startSlot; i < endSlot; i++) {
            final ItemStack stack = getMc().player.inventoryContainer.getSlot(i).getStack();

            if (!stack.isEmpty() && stack.getItem().equals(item))
                return i;
        }

        return -1;
    }

    public static boolean hasSpaceHotbar() {
        for (int i = 36; i < 45; i++) {
            final ItemStack stack = getMc().player.inventory.getStackInSlot(i);

            if (stack.isEmpty())
                return true;
        }

        return false;
    }

    public static int findAutoBlockBlock() {
        for (int i = 36; i < 45; i++) {
            final ItemStack itemStack = getMc().player.inventoryContainer.getSlot(i).getStack();

            if (!itemStack.isEmpty() && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
                final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
                final Block block = itemBlock.getBlock();

                if (block.isFullCube(block.getDefaultState()) && !BLOCK_BLACKLIST.contains(block)
                        && !(block instanceof BlockBush))
                    return i;
            }
        }

        for (int i = 36; i < 45; i++) {
            final ItemStack itemStack = getMc().player.inventoryContainer.getSlot(i).getStack();

            if (!itemStack.isEmpty() && itemStack.getItem() instanceof ItemBlock && itemStack.stackSize > 0) {
                final ItemBlock itemBlock = (ItemBlock) itemStack.getItem();
                final Block block = itemBlock.getBlock();

                if (!BLOCK_BLACKLIST.contains(block) && !(block instanceof BlockBush))
                    return i;
            }
        }

        return -1;
    }
}