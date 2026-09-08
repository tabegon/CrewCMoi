package fr.crewcmoi.claims.gui;

import fr.crewcmoi.claims.database.ClaimFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

public class ClaimSettingsHolder implements InventoryHolder {

    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private Inventory inventory;
    private final boolean adminMode;
    private boolean deleteArmed;
    private final Map<Integer, ClaimFlag> slotToFlag = new HashMap<>();

    public ClaimSettingsHolder(String world, int chunkX, int chunkZ) {
        this(world, chunkX, chunkZ, false);
    }

    public ClaimSettingsHolder(String world, int chunkX, int chunkZ, boolean adminMode) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.adminMode = adminMode;
    }

    public boolean isAdminMode() {
        return adminMode;
    }

    public boolean isDeleteArmed() {
        return deleteArmed;
    }

    public void setDeleteArmed(boolean deleteArmed) {
        this.deleteArmed = deleteArmed;
    }

    public String getWorld() {
        return world;
    }

    public int getChunkX() {
        return chunkX;
    }

    public int getChunkZ() {
        return chunkZ;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public void mapSlot(int slot, ClaimFlag flag) {
        slotToFlag.put(slot, flag);
    }

    public ClaimFlag getFlag(int slot) {
        return slotToFlag.get(slot);
    }
}
