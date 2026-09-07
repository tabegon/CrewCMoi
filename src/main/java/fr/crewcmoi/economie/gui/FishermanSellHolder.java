package fr.crewcmoi.economie.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * GUI de vente du NPC pêcheur (marqueur distinct de SellHolder/le /sell classique
 * puisque les prix et les objets acceptés y sont différents : voir FishermanGuiManager).
 */
public class FishermanSellHolder implements InventoryHolder {

    // Taille totale de la GUI (6 lignes de 9 = un double coffre)
    public static final int SIZE = 54;

    // Slots réservés aux items à vendre (les 4 premières lignes)
    public static final int[] ITEM_SLOTS = buildItemSlots();

    // Slot du bouton "vendre" (état normal) / "confirmer" (état de confirmation)
    public static final int SELL_SLOT = 40;
    public static final int CONFIRM_SLOT = 33;

    // Slot du bouton "annuler" affiché uniquement pendant l'étape de confirmation
    public static final int CANCEL_CONFIRM_SLOT = 29;

    // Slot du bouton "retour" vers le menu principal du pêcheur
    public static final int BACK_SLOT = 45;

    private Inventory inventory;

    private boolean confirming = false;
    private List<ItemStack> pendingItems;

    private static int[] buildItemSlots() {
        int[] slots = new int[36];
        for (int i = 0; i < 36; i++) {
            slots[i] = i;
        }
        return slots;
    }

    public static boolean isItemSlot(int slot) {
        return slot >= 0 && slot < 36;
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
