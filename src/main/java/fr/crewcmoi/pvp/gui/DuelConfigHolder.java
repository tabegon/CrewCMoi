package fr.crewcmoi.pvp.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class DuelConfigHolder implements InventoryHolder {

    public static final int SIZE = 27;
    public static final int KEEPINVENTORY_SLOT = 2;
    public static final int BET_SLOT = 6;
    public static final int KIT_SLOT = 4;
    public static final int DROPHEAD_SLOT = 8;
    public static final int CONFIRM_SLOT = 24;
    public static final int CANCEL_SLOT = 20;
    public static final int INFO_SLOT = 13;

    private final UUID targetUuid;
    private final String targetName;
    private Inventory inventory;

    private boolean keepInventory = false;
    private double bet = 0.0;
    private boolean dropHead = false;
    private String kitId = null;

    public DuelConfigHolder(UUID targetUuid, String targetName) {
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    public UUID getTargetUuid() {
        return targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public boolean isKeepInventory() {
        return keepInventory;
    }

    public void setKeepInventory(boolean keepInventory) {
        this.keepInventory = keepInventory;
    }

    public double getBet() {
        return bet;
    }

    public void setBet(double bet) {
        this.bet = bet;
    }

    public String getKitId() {
        return kitId;
    }

    public void setKitId(String kitId) {
        this.kitId = kitId;
        if (kitId != null) {
            this.keepInventory = false;
        }
    }

    public boolean isDropHead() {
        return dropHead;
    }

    public void setDropHead(boolean dropHead) {
        this.dropHead = dropHead;
    }
}
