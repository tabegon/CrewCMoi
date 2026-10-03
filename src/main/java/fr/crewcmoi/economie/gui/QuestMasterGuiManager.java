package fr.crewcmoi.economie.gui;

import fr.crewcmoi.Main;
import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class QuestMasterGuiManager {
    private final Main plugin;

    public QuestMasterGuiManager(Main plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        QuestMasterHolder holder = new QuestMasterHolder();
        String title = plugin.getConfig().getString("quest-master.title", "&5&lQuest Master");
        Inventory gui = Bukkit.createInventory(holder, 27, Messages.color(title));
        holder.setInventory(gui);

        // Fond NOTHING.
        for (int slot = 0; slot < gui.getSize(); slot++) {
            gui.setItem(slot, GuiItems.nothing(" "));
        }

        // Slots 10 à 16 : le modèle est fixe, mais le nom et le lore sont
        // entièrement configurables individuellement dans config.yml.
        for (int slot = 10; slot <= 16; slot++) {
            ConfigurationSection section = plugin.getConfig()
                    .getConfigurationSection("quest-master.slots." + slot);

            ItemStack item;
            if (slot == 10) {
                item = createQuestItem(section, player);
            } else if (slot == 11 && plugin.getQuestManager().isCompleted(player)) {
                ConfigurationSection unlocked = plugin.getConfig()
                        .getConfigurationSection("quest-master.slots.11.unlocked");
                item = createConfiguredItem(unlocked, GuiItems.lock(getDisplayName(section, "&c&lVERROUILLÉ")));
                applyLore(item, unlocked);
            } else {
                item = GuiItems.lock(getDisplayName(section, "&c&lVERROUILLÉ"));
                applyLore(item, section);
            }
            gui.setItem(slot, item);
        }

        // Slot 22 : téléportation aléatoire dans le monde quest.
        ConfigurationSection randomTp = plugin.getConfig()
                .getConfigurationSection("quest-master.random-teleport");
        ItemStack randomTpItem = new ItemStack(Material.valueOf(
                plugin.getConfig().getString("quest-master.random-teleport.material", "NETHER_STAR")));
        ItemMeta randomMeta = randomTpItem.getItemMeta();
        if (randomMeta != null) {
            randomMeta.setDisplayName(getDisplayName(randomTp, "&d&lTéléportation aléatoire"));
            applyLore(randomTpItem, randomTp);
        }
        gui.setItem(22, randomTpItem);

        player.openInventory(gui);
    }

    private ItemStack createQuestItem(ConfigurationSection section, Player player) {
        ConfigurationSection displaySection = section;
        if (plugin.getQuestManager().isCompleted(player)) {
            ConfigurationSection completed = plugin.getConfig()
                    .getConfigurationSection("quest-master.slots.10.completed");
            if (completed != null) {
                displaySection = completed;
            }
        }

        ItemStack item = GuiItems.goat(getDisplayName(displaySection, "&a&lElectric Goat"));
        applyLore(item, displaySection);
        return item;
    }

    private ItemStack createConfiguredItem(ConfigurationSection section, ItemStack fallback) {
        if (section == null) return fallback;

        String itemsAdderId = section.getString("itemsadder-id", "").trim();
        if (!itemsAdderId.isEmpty()) {
            CustomStack stack = CustomStack.getInstance(itemsAdderId);
            if (stack != null) return stack.getItemStack().clone();
        }

        Material material;
        try {
            material = Material.valueOf(section.getString("material", "PAPER").toUpperCase());
        } catch (IllegalArgumentException ignored) {
            material = Material.PAPER;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int customModelData = section.getInt("custom-model-data", 0);
            if (customModelData > 0) meta.setCustomModelData(customModelData);
            meta.setDisplayName(getDisplayName(section, "&a&lQuête"));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String getDisplayName(ConfigurationSection section, String fallback) {
        if (section == null) return color(fallback);
        return color(section.getString("display-name", fallback));
    }

    private void applyLore(ItemStack item, ConfigurationSection section) {
        if (section == null || !section.isList("lore")) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        List<String> lore = new ArrayList<>();
        for (String line : section.getStringList("lore")) {
            lore.add(color(line));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
