package fr.crewcmoi.economie.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class FishermanMenuHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int SELL_BUTTON_SLOT = 11;
    public static final int BUY_BUTTON_SLOT = 15;

    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
