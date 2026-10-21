package fr.crewcmoi.admin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.List;
import java.util.UUID;

public final class AdminModerationHolder implements InventoryHolder {
    public enum Mode { PET_PLAYERS, QUEST_PLAYERS, PET_ACTIONS, QUEST_ACTIONS, INVENTORY_PLAYERS, INVENTORY_ACTIONS }
    private final Mode mode;
    private final UUID target;
    private final List<UUID> players;
    private Inventory inventory;

    public AdminModerationHolder(Mode mode, UUID target, List<UUID> players) {
        this.mode = mode;
        this.target = target;
        this.players = players == null ? List.of() : List.copyOf(players);
    }

    public Mode getMode() { return mode; }
    public UUID getTarget() { return target; }
    public List<UUID> getPlayers() { return players; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
