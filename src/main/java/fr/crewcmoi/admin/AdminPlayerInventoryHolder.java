package fr.crewcmoi.admin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.UUID;

public final class AdminPlayerInventoryHolder implements InventoryHolder {
    public enum Type { PLAYER_INVENTORY, ENDER_CHEST }
    private final UUID target;
    private final Type type;
    private final String targetName;
    private final boolean offline;
    private Inventory inventory;

    public AdminPlayerInventoryHolder(UUID target, Type type) {
        this(target, type, target.toString(), false);
    }

    public AdminPlayerInventoryHolder(UUID target, Type type, String targetName, boolean offline) {
        this.target = target;
        this.type = type;
        this.targetName = targetName == null ? target.toString() : targetName;
        this.offline = offline;
    }

    public UUID getTarget() { return target; }
    public Type getType() { return type; }
    public String getTargetName() { return targetName; }
    public boolean isOffline() { return offline; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
