package fr.crewcmoi.pvp.utils;

import fr.crewcmoi.Main;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class HeadSellPrice {

    private static final String KEY = "head_sell_price";

    private HeadSellPrice() {
    }

    private static NamespacedKey key(Main plugin) {
        return new NamespacedKey(plugin, KEY);
    }

    public static void apply(Main plugin, ItemMeta meta, double price) {
        meta.getPersistentDataContainer().set(key(plugin), PersistentDataType.DOUBLE, price);
    }

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
