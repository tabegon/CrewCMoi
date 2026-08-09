package fr.crewcmoi.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.List;

/**
 * Marqueur permettant d'identifier l'inventaire du /sell afin de savoir
 * quels slots sont dédiés à la vente, au bouton de confirmation et au
 * remplissage décoratif.
 */
public class SellHolder implements InventoryHolder {

    // Taille totale de la GUI (3 lignes de 9)
    public static final int SIZE = 27;

    // Slots réservés aux items à vendre (les 2 premières lignes)
    public static final int[] ITEM_SLOTS = buildItemSlots();

    // Slot du bouton "vendre" (état normal) / "confirmer" (état de confirmation)
    public static final int SELL_SLOT = 22;
    public static final int CONFIRM_SLOT = 15;

    // Slot du bouton "annuler" affiché uniquement pendant l'étape de confirmation
    public static final int CANCEL_CONFIRM_SLOT = 11;

    private Inventory inventory;

    // Passe à true lorsque le joueur a cliqué sur "vendre" et doit confirmer la vente.
    // Pendant cette étape, les objets réels sont retirés visuellement de la GUI (conservés
    // dans pendingItems) pour empêcher toute manipulation avant confirmation.
    private boolean confirming = false;
    private List<ItemStack> pendingItems;

    private static int[] buildItemSlots() {
        int[] slots = new int[18];
        for (int i = 0; i < 18; i++) {
            slots[i] = i;
        }
        return slots;
    }

    public static boolean isItemSlot(int slot) {
        return slot >= 0 && slot < 18;
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
