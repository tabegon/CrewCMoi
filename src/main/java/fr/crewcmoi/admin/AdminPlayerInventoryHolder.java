package fr.crewcmoi.admin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import java.util.UUID;

public final class AdminPlayerInventoryHolder implements InventoryHolder {
    public enum Type { PLAYER_INVENTORY, ENDER_CHEST }
    private final UUID target;
    private final Type type;
    private Inventory inventory;

    public AdminPlayerInventoryHolder(UUID target, Type type) {
        this.target = target;
        this.type = type;
    }

    public UUID getTarget() { return target; }
    public Type getType() { return type; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }
    @Override public Inventory getInventory() { return inventory; }
}
