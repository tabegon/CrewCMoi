package fr.crewcmoi.pvp.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class TeamGuiHolder implements InventoryHolder {
    public static final int SIZE = 27;
    public static final int BACK_SLOT = 7;
    public static final int MONEY_SLOT = 16;
    private static final int[] MEMBER_SLOTS = {2,3,4,5,11,12,13,14};

    private Inventory inventory;
    private final Map<Integer, UUID> memberSlots = new LinkedHashMap<>();

    @Override
    public Inventory getInventory() { return inventory; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public void mapMember(int slot, UUID uuid) { memberSlots.put(slot, uuid); }

    public UUID getMember(int slot) { return memberSlots.get(slot); }

    public void clearMembers() { memberSlots.clear(); }

    public static boolean isMemberSlot(int slot) {
        for (int value : MEMBER_SLOTS) if (value == slot) return true;
        return false;
    }

    public static int[] memberSlots() { return MEMBER_SLOTS.clone(); }
}
