package fr.crewcmoi.claims.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marqueur permettant d'identifier l'inventaire de la GUI /claim shop.
 */
public class ClaimShopHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int BUY_SLOT = 13;

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
