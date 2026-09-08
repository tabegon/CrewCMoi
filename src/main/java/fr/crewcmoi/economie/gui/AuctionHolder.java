package fr.crewcmoi.economie.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class AuctionHolder implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45; 
    public static final int PREV_PAGE_SLOT = 45;
    public static final int NEXT_PAGE_SLOT = 53;
    public static final int INFO_SLOT = 49;
    
    public static final int MY_LISTINGS_SLOT = 48;

    private Inventory inventory;
    private int page;
    
    private boolean myListings;
    private final Map<Integer, Integer> slotToAuctionId = new HashMap<>();

    public static boolean isAuctionSlot(int slot) {
        return slot >= 0 && slot < ITEMS_PER_PAGE;
    }

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

    public void mapSlot(int slot, int auctionId) {
        slotToAuctionId.put(slot, auctionId);
    }

    public void clearMapping() {
        slotToAuctionId.clear();
    }

    public Integer getAuctionId(int slot) {
        return slotToAuctionId.get(slot);
    }

    public boolean isMyListings() {
        return myListings;
    }

    public void setMyListings(boolean myListings) {
        this.myListings = myListings;
    }
}
