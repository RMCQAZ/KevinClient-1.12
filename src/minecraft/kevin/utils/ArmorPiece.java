package kevin.utils;

import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;

public class ArmorPiece {
    private final ItemStack itemStack;
    private final int slot;

    public ArmorPiece(ItemStack itemStack, int slot) {
        this.itemStack = itemStack;
        this.slot = slot;
    }

    public int getArmorType() {
        return ((ItemArmor) itemStack.getItem()).armorType.getIndex();
    }

    public int getSlot() {
        return slot;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }
}
