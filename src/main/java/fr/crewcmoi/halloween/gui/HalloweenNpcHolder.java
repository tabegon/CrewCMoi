package fr.crewcmoi.halloween.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HalloweenNpcHolder implements InventoryHolder {
    private Inventory inventory;
    private int page = 1;

    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    public int getPage() { return page; }
    public void setPage(int page) { this.page = Math.max(1, Math.min(2, page)); }

    @Override public Inventory getInventory() { return inventory; }
}
