package fr.crewcmoi.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Marqueur permettant d'identifier l'inventaire de révision des primes
 * (/bounty review, réservé aux admins) : liste des raisons de primes en attente
 * de validation. Clic gauche sur une entrée = approuver, clic droit = refuser
 * (remboursé au contributeur).
 */
public class BountyReviewHolder implements InventoryHolder {

    public static final int SIZE = 54;
    public static final int ITEMS_PER_PAGE = 45; // slots 0-44
    public static final int PREV_PAGE_SLOT = 45;
    public static final int NEXT_PAGE_SLOT = 53;
    public static final int INFO_SLOT = 49;

    private Inventory inventory;
    private int page;
    private final Map<Integer, Integer> slotToEntryId = new HashMap<>();

    public static boolean isEntrySlot(int slot) {
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

    public void mapSlot(int slot, int entryId) {
        slotToEntryId.put(slot, entryId);
    }

    public void clearMapping() {
        slotToEntryId.clear();
    }

    public Integer getEntryId(int slot) {
        return slotToEntryId.get(slot);
    }
}
