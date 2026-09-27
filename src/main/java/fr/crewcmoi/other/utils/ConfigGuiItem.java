package fr.crewcmoi.other.utils;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Fabrique des items de GUI à partir du config.yml.
 *
 * Si "itemsadder-id" est renseigné, l'item est chargé depuis ItemsAdder.
 * Sinon, le matériau de secours est utilisé.
 */
public final class ConfigGuiItem {

    private ConfigGuiItem() {
    }

    public static ItemStack fromSection(Main plugin, ConfigurationSection section, Material fallbackMaterial) {
        if (section == null) {
            return new ItemStack(fallbackMaterial);
        }

        ItemStack item = null;
        String itemsAdderId = section.getString("itemsadder-id", "").trim();

        if (!itemsAdderId.isEmpty()) {
            CustomStack custom = CustomStack.getInstance(itemsAdderId);
            if (custom != null && custom.getItemStack() != null) {
                item = custom.getItemStack().clone();
            } else {
                plugin.getLogger().warning("GUI : impossible de charger l'item ItemsAdder '" + itemsAdderId + "'.");
            }
        }

        if (item == null) {
            String materialName = section.getString("material", fallbackMaterial.name());
            Material material = Material.matchMaterial(materialName);
            if (material == null) {
                plugin.getLogger().warning("GUI : matériau invalide '" + materialName + "'. Utilisation de " + fallbackMaterial.name() + ".");
                material = fallbackMaterial;
            }
            item = new ItemStack(material);
        }

        int amount = Math.max(1, Math.min(item.getMaxStackSize(), section.getInt("amount", 1)));
        item.setAmount(amount);

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (section.contains("display-name")) {
                meta.setDisplayName(color(section.getString("display-name", "")));
            }

            if (section.isList("lore")) {
                List<String> lore = new ArrayList<>();
                for (String line : section.getStringList("lore")) {
                    lore.add(color(line));
                }
                meta.setLore(lore);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    public static ItemStack fromSection(Main plugin, ConfigurationSection section) {
        return fromSection(plugin, section, Material.PAPER);
    }

    private static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
