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

    private GuiItems() {
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
