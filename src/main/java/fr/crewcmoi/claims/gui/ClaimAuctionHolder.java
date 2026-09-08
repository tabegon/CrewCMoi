package fr.crewcmoi.claims.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class ClaimAuctionHolder implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45; 
    public static final int PREV_PAGE_SLOT = 45;
    public static final int NEXT_PAGE_SLOT = 53;
    public static final int INFO_SLOT = 49;

    private Inventory inventory;
    private int page;
    
    private final Map<Integer, String> slotToChunkKey = new HashMap<>();

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public void mapSlot(int slot, String world, int chunkX, int chunkZ) {
        slotToChunkKey.put(slot, world + ";" + chunkX + ";" + chunkZ);
    }

    public void clearMapping() {
        slotToChunkKey.clear();
    }

    public String getChunkKey(int slot) {
        return slotToChunkKey.get(slot);
    }
}
