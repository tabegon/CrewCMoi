package fr.crewcmoi.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

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

    // Slot du bouton de confirmation (dernière ligne, centre)
    public static final int CONFIRM_SLOT = 22;

    private Inventory inventory;

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
}
