package fr.crewcmoi.claims.gui;

import fr.crewcmoi.claims.database.ClaimFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;

/**
 * Marqueur permettant d'identifier l'inventaire de la GUI /claims settings :
 * chaque slot correspond à une règle (ClaimFlag) du claim que le joueur édite.
 */
public class ClaimSettingsHolder implements InventoryHolder {

    private final String world;
    private final int chunkX;
    private final int chunkZ;
    private Inventory inventory;
    private final Map<Integer, ClaimFlag> slotToFlag = new HashMap<>();

    public ClaimSettingsHolder(String world, int chunkX, int chunkZ) {
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
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
