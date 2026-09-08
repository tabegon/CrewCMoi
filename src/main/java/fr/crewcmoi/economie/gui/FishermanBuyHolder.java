package fr.crewcmoi.economie.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class FishermanBuyHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int BACK_SLOT = 22;

    private Inventory inventory;
    private final Map<Integer, String> slotToOfferId = new HashMap<>();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void mapSlot(int slot, String offerId) {
        slotToOfferId.put(slot, offerId);
    }

    public String getOfferId(int slot) {
        return slotToOfferId.get(slot);
    }
}
