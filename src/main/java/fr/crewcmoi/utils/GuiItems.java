package fr.crewcmoi.utils;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Fabrique les items décoratifs utilisés dans les GUIs (bouton annuler, bouton
 * confirmer, remplissage vide), basés sur des ghast_spawn_egg avec un
 * CustomModelData spécifique (resource pack), en remplacement des vitres teintées.
 */
public final class GuiItems {

    private static final Material BASE_MATERIAL = Material.GHAST_SPAWN_EGG;

    private static final int CANCEL_MODEL_DATA = 15;
    private static final int CHECKMARK_MODEL_DATA = 18;
    private static final int NOTHING_MODEL_DATA = 20;

    private GuiItems() {
    }

    /**
     * Item "annuler" (remplaçait RED_STAINED_GLASS_PANE).
     */
    public static ItemStack cancelButton(String displayName) {
        return build(CANCEL_MODEL_DATA, displayName);
    }

    /**
     * Item "confirmer" (remplaçait LIME_STAINED_GLASS_PANE).
     */
    public static ItemStack checkmarkButton(String displayName) {
        return build(CHECKMARK_MODEL_DATA, displayName);
    }

    /**
     * Item de remplissage neutre (remplaçait GRAY_STAINED_GLASS_PANE / BLACK_STAINED_GLASS_PANE).
     */
    public static ItemStack nothing(String displayName) {
        return build(NOTHING_MODEL_DATA, displayName);
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
