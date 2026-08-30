package fr.crewcmoi.economie.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Marqueur permettant d'identifier l'inventaire du /baltop afin d'empêcher
 * toute interaction (prise d'item, dépôt, drag, etc.) dans le GuiListener.
 */
public class BaltopHolder implements InventoryHolder {

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
