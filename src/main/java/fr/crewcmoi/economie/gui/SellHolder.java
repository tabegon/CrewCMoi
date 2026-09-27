package fr.crewcmoi.economie.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class SellHolder implements InventoryHolder {

    public static final int SIZE = 54;

    public static final int[] ITEM_SLOTS = buildItemSlots();

    public static final int SELL_SLOT = 47;
    public static final int CONFIRM_SLOT = 33;

    public static final int CANCEL_CONFIRM_SLOT = 29;
    public static final int NPC_SLOT = 24;

    private Inventory inventory;

    
    
    private boolean confirming = false;
    private List<ItemStack> pendingItems;

    private static int[] buildItemSlots() {
        // Slots où le joueur peut déposer les objets à vendre :
        // 9-12, 18-21, 27-30, 36-39.
        return new int[] {
                0, 1, 2, 3,
                9, 10, 11, 12,
                18, 19, 20, 21,
                27, 28, 29, 30,
                36, 37, 38, 39
        };
    }

    public static boolean isItemSlot(int slot) {
        for (int itemSlot : ITEM_SLOTS) {
            if (itemSlot == slot) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean isConfirming() {
        return confirming;
    }

    public void setConfirming(boolean confirming) {
        this.confirming = confirming;
    }

    public List<ItemStack> getPendingItems() {
        return pendingItems;
    }

    public void setPendingItems(List<ItemStack> pendingItems) {
        this.pendingItems = pendingItems;
    }
}
