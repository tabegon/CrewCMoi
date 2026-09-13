package fr.crewcmoi.economie.managers;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class LootboxHolder implements InventoryHolder {
    private Inventory inventory;

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
