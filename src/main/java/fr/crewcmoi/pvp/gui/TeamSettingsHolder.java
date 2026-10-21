package fr.crewcmoi.pvp.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class TeamSettingsHolder implements InventoryHolder {
    public static final int SIZE = 27;
    public static final int FRIENDLY_FIRE_SLOT = 13;
    public static final int BACK_SLOT = 18;
    public static final int CLOSE_SLOT = 26;

    private final int teamId;
    private Inventory inventory;

    public TeamSettingsHolder(int teamId) {
        this.teamId = teamId;
    }

    public int getTeamId() {
        return teamId;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }
}
