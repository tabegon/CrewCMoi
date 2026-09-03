package fr.crewcmoi.pvp.utils;

import fr.crewcmoi.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Attache un prix de vente à un item (typiquement une tête de joueur droppée à la mort,
 * voir PlayerHeadDropListener) via son PersistentDataContainer plutôt que via son lore.
 *
 * Le lore reste purement informatif ; c'est cette donnée (invisible et non modifiable par
 * le joueur, contrairement au lore qu'une enclume pourrait altérer) qui fait foi pour le
 * calcul du prix réel lors d'une vente (voir SellGuiManager).
 */
public final class HeadSellPrice {

    private static final String KEY = "head_sell_price";

    private HeadSellPrice() {
    }

    private static NamespacedKey key(Main plugin) {
        return new NamespacedKey(plugin, KEY);
    }

    /**
     * Tague le prix de vente sur le meta d'un item. À appeler avant item.setItemMeta(meta).
     */
    public static void apply(Main plugin, ItemMeta meta, double price) {
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.DOUBLE, price);
    }

    /**
     * Retourne le prix de vente tagué sur cet item, ou -1 s'il n'en a pas (item normal).
     */
    public static double getPrice(Main plugin, ItemStack stack) {
        if (stack == null) {
            return -1.0;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return -1.0;
        }
        Double value = meta.getPersistentDataContainer().get(key(plugin), PersistentDataType.DOUBLE);
        return value != null ? value : -1.0;
    }
}
