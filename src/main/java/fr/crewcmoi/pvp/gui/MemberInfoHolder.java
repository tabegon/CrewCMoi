package fr.crewcmoi.pvp.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class MemberInfoHolder implements InventoryHolder {
    public static final int SIZE = 27;
    public static final int BACK_SLOT = 0;
    public static final int PLAYER_HEAD_SLOT = 13;
    public static final int MONEY_SLOT = 18;
    public static final int EXCLUDE_SLOT = 26;

    private Inventory inventory;
    private final UUID memberUuid;
    private final UUID ownerUuid;

    public MemberInfoHolder(UUID memberUuid, UUID ownerUuid) {
        this.memberUuid = memberUuid;
        this.ownerUuid = ownerUuid;
    }

    @Override
    public Inventory getInventory() { return inventory; }

    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public UUID getMemberUuid() { return memberUuid; }

    public UUID getOwnerUuid() { return ownerUuid; }
}
