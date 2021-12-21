package kevin.module.modules.combat;

import kevin.event.EventTarget;
import kevin.event.Render3DEvent;
import kevin.module.BooleanValue;
import kevin.module.IntegerValue;
import kevin.module.Module;
import kevin.module.ModuleCategory;
import kevin.utils.*;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.inventory.ClickType;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemElytra;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.util.EnumHand;
import org.lwjgl.input.Keyboard;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AutoArmor extends Module {

    public static final ArmorComparator ARMOR_COMPARATOR = new ArmorComparator();
    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 100, 0, 400) {

        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxDelayValue.get();

            if (maxDelay < newValue) set(maxDelay);
        }
    };
    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 200, 0, 400) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int minDelay = minDelayValue.get();

            if (minDelay > newValue) set(minDelay);
        }
    };
    private final BooleanValue invOpenValue = new BooleanValue("InvOpen", false);
    private final BooleanValue simulateInventory = new BooleanValue("SimulateInventory", true);
    private final BooleanValue noMoveValue = new BooleanValue("NoMove", false);
    private final IntegerValue itemDelayValue = new IntegerValue("ItemDelay", 0, 0, 5000);
    private final BooleanValue hotbarValue = new BooleanValue("Hotbar", true);
    private final BooleanValue elytra = new BooleanValue("Elytra",true);

    private long delay;

    private boolean locked = false;
    private boolean isElytra = false;

    public AutoArmor() {
        super("AutoArmor", "Automatically equips the best armor in your inventory.", Keyboard.KEY_NONE, ModuleCategory.COMBAT);
    }

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if (!InventoryUtils.CLICK_TIMER.hasTimePassed(delay) || getMc().player == null ||
                (getMc().player.openContainer != null && getMc().player.openContainer.windowId != 0))
            return;

        // Find best armor
        final Map<Integer, List<ArmorPiece>> armorPieces = IntStream.range(0, 36)
                .filter(i -> {
                    final ItemStack itemStack = getMc().player.inventory.getStackInSlot(i);

                    return !itemStack.isEmpty() && itemStack.getItem() instanceof ItemArmor
                            && (i < 9 || System.currentTimeMillis() - itemStack.itemDelay >= itemDelayValue.get());
                })
                .mapToObj(i -> new ArmorPiece(getMc().player.inventory.getStackInSlot(i), i))
                .collect(Collectors.groupingBy(ArmorPiece::getArmorType));

        final ArmorPiece[] bestArmor = new ArmorPiece[4];

        for (final Map.Entry<Integer, List<ArmorPiece>> armorEntry : armorPieces.entrySet()) {
            bestArmor[armorEntry.getKey()] = armorEntry.getValue().stream()
                    .max(ARMOR_COMPARATOR).orElse(null);
        }

        // Swap armor
        for (int i = 0; i < 4; i++) {
            ArmorPiece armorPiece = bestArmor[i];

            int armorSlot = i;

            final ArmorPiece oldArmor = new ArmorPiece(getMc().player.inventory.armorItemInSlot(armorSlot), -1);

            if (elytra.get()&&(oldArmor.getItemStack().getItem() instanceof ItemElytra||isElytra)){
                if (!oldArmor.getItemStack().isEmpty()&&ItemElytra.isUsable(oldArmor.getItemStack()))
                    continue;
                else {
                    final int[] s = IntStream.range(0, 36).filter(v -> {
                        final ItemStack itemStack = getMc().player.inventory.getStackInSlot(v);
                        return !itemStack.isEmpty() && itemStack.getItem() instanceof ItemElytra
                                && ItemElytra.isUsable(itemStack);
                    }).toArray();
                    isElytra = !isElytra;
                    if (s.length != 0) {
                        armorPiece = new ArmorPiece(getMc().player.inventory.getStackInSlot(s[0]),s[0]);
                    }
                }
            }

            if (armorPiece == null)
                continue;

            if (ItemUtils.isStackEmpty(oldArmor.getItemStack()) || !(oldArmor.getItemStack().getItem() instanceof ItemArmor)||
                    ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0) {
                if (!ItemUtils.isStackEmpty(oldArmor.getItemStack()) && move(8 - armorSlot, true)) {
                    locked = true;
                    return;
                }

                if (ItemUtils.isStackEmpty(getMc().player.inventory.armorItemInSlot(armorSlot)) && move(armorPiece.getSlot(), false)) {
                    locked = true;
                    return;
                }
            }
        }

        locked = false;
    }

    public boolean isLocked() {
        return this.getState() && locked;
    }

    private boolean move(int item, boolean isArmorSlot) {
        if (!isArmorSlot && item < 9 && hotbarValue.get() && !(getMc().currentScreen instanceof GuiInventory)) {
            getMc().getConnection().sendPacket(new CPacketHeldItemChange(item));
            getMc().getConnection().sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
            getMc().getConnection().sendPacket(new CPacketHeldItemChange(getMc().player.inventory.currentItem));

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            return true;
        } else if (!(noMoveValue.get() && MovementUtils.isMoving()) && (!invOpenValue.get() || getMc().currentScreen instanceof GuiInventory) && item != -1) {
            final boolean openInventory = simulateInventory.get() && !(getMc().currentScreen instanceof GuiInventory);

            if (openInventory)
                getMc().getConnection().sendPacket(new CPacketEntityAction(getMc().player, CPacketEntityAction.Action.OPEN_INVENTORY));

            boolean full = isArmorSlot;

            if (full) {
                for (ItemStack iItemStack : getMc().player.inventory.mainInventory) {
                    if (ItemUtils.isStackEmpty(iItemStack)) {
                        full = false;
                        break;
                    }
                }
            }

            if (full) {
                getMc().playerController.windowClick(getMc().player.inventoryContainer.windowId, item, 1, ClickType.THROW, getMc().player);
            } else {
                getMc().playerController.windowClick(getMc().player.inventoryContainer.windowId, (isArmorSlot ? item : (item < 9 ? item + 36 : item)), 0, ClickType.QUICK_MOVE, getMc().player);
            }

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (openInventory)
                getMc().getConnection().sendPacket(new CPacketCloseWindow());

            return true;
        }

        return false;
    }
}
