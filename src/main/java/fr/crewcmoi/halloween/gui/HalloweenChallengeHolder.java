package fr.crewcmoi.halloween.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HalloweenChallengeHolder implements InventoryHolder {
    private Inventory inventory;
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
