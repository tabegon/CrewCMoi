package fr.crewcmoi.economie.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class AuctionHolder implements InventoryHolder {

    public static final int MAIN_SIZE = 54;
    public static final int MY_LISTINGS_SIZE = 27;
    public static final int MAIN_ITEMS_PER_PAGE = 35;
    public static final int MY_ITEMS_PER_PAGE = 14;
    public static final int PREV_PAGE_SLOT = 51;
    public static final int NEXT_PAGE_SLOT = 47;
    public static final int MAIN_BACK_SLOT = 0;
    public static final int MY_BACK_SLOT = 22;
    public static final int MY_LISTINGS_SLOT = 49;

    private static final int[] MAIN_ITEM_SLOTS = {
            1, 2, 3, 4, 5, 6, 7,
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
    };

    private static final int[] MY_ITEM_SLOTS = {
            1, 2, 3, 4, 5, 6, 7,
            10, 11, 12, 13, 14, 15, 16
    };

    private Inventory inventory;
    private int page;
    private boolean myListings;
    private final Map<Integer, Integer> slotToAuctionId = new HashMap<>();

    public static int itemSlot(boolean myListings, int index) {
        int[] slots = myListings ? MY_ITEM_SLOTS : MAIN_ITEM_SLOTS;
        return index >= 0 && index < slots.length ? slots[index] : -1;
    }

    public static boolean isAuctionSlot(boolean myListings, int slot) {
        int[] slots = myListings ? MY_ITEM_SLOTS : MAIN_ITEM_SLOTS;
        for (int itemSlot : slots) {
            if (itemSlot == slot) return true;
        }
        return false;
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
