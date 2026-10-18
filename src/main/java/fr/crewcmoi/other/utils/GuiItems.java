package fr.crewcmoi.other.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class GuiItems {

    private static final Material BASE_MATERIAL = Material.GHAST_SPAWN_EGG;

    private static final int CANCEL_MODEL_DATA = 15;
    private static final int CHECKMARK_MODEL_DATA = 18;
    private static final int NOTHING_MODEL_DATA = 20;
    private static final int NPC_MODEL_DATA = 33;
    private static final int SEARCH_MODEL_DATA = 16;
    private static final int GOAT_MODEL_DATA = 34;
    private static final int LOCK_MODEL_DATA = 35;
    private static final int MONEY_MODEL_DATA = 17;
    private static final int CHEST_MODEL_DATA = 37;
    private static final int DUEL_MODEL_DATA = 39;
    private static final int ELYTRA_OFF_MODEL_DATA = 40;
    private static final int ELYTRA_ON_MODEL_DATA = 41;
    private static final int KEEPINVENTORY_OFF_MODEL_DATA = 42;
    private static final int NO_KIT_MODEL_DATA = 43;
    private static final int KIT_MODEL_DATA = 44;
    private static final int HEAD_DROP_OFF_MODEL_DATA = 45;
    private static final int HEAD_DROP_ON_MODEL_DATA = 46;
    private static final int BARRIERE_MODEL_DATA = 19;

    private GuiItems() {
    }


    public static ItemStack arrowButton(String displayName) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack cancelButton(String displayName) {
        return build(CANCEL_MODEL_DATA, displayName);
    }

    public static ItemStack checkmarkButton(String displayName) {
        return build(CHECKMARK_MODEL_DATA, displayName);
    }

    public static ItemStack nothing(String displayName) {
        return build(NOTHING_MODEL_DATA, displayName);
    }

    public static ItemStack npc(String displayName) {
        return build(NPC_MODEL_DATA, displayName);
    }

    public static ItemStack searchButton(String displayName) {
        return build(SEARCH_MODEL_DATA, displayName);
    }

    public static ItemStack goat(String displayName) {
        return build(GOAT_MODEL_DATA, displayName);
    }

    public static ItemStack lock(String displayName) {
        return build(LOCK_MODEL_DATA, displayName);
    }

    public static ItemStack moneyButton(String displayName) {
        return build(MONEY_MODEL_DATA, displayName);
    }

    public static ItemStack chestButton(String displayName) {
        return build(CHEST_MODEL_DATA, displayName);
    }

    public static ItemStack duelButton(String displayName) {
        return build(DUEL_MODEL_DATA, displayName);
    }

    public static ItemStack elytra(boolean enabled, String displayName) {
        return build(enabled ? ELYTRA_ON_MODEL_DATA : ELYTRA_OFF_MODEL_DATA, displayName);
    }

    public static ItemStack keepInventory(boolean enabled, String displayName) {
        return build(enabled ? CHEST_MODEL_DATA : KEEPINVENTORY_OFF_MODEL_DATA, displayName);
    }

    public static ItemStack kit(boolean enabled, String displayName) {
        return build(enabled ? KIT_MODEL_DATA : NO_KIT_MODEL_DATA, displayName);
    }

    public static ItemStack headDrop(boolean enabled, String displayName) {
        return build(enabled ? HEAD_DROP_ON_MODEL_DATA : HEAD_DROP_OFF_MODEL_DATA, displayName);
    }

    public static ItemStack barrier(String displayName) {
        return build(BARRIERE_MODEL_DATA, displayName);
    }

    private static ItemStack build(int customModelData, String displayName) {
        ItemStack item = new ItemStack(BASE_MATERIAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setCustomModelData(customModelData);
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            item.setItemMeta(meta);
        }
        return item;
    }
}
