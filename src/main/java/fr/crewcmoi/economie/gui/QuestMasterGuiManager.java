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
        String title = plugin.getConfig().getString("quest-master.title", "&8&lQuest Master");
        Inventory gui = Bukkit.createInventory(holder, 27, Messages.color(title));
        holder.setInventory(gui);

        // Fond NOTHING.
        for (int slot = 0; slot < gui.getSize(); slot++) {
            gui.setItem(slot, GuiItems.nothing(" "));
        }

        // Slot 10 lance l'énigme et téléporte dans le monde Quest.
        ConfigurationSection riddle = plugin.getConfig().getConfigurationSection("quest-master.slots.10");
        ConfigurationSection riddleCompleted = plugin.getConfig().getConfigurationSection("quest-master.slots.10.completed");
        ItemStack riddleItem;
        if (plugin.getQuestManager().isCompleted(player)) {
            riddleItem = createConfiguredItem(riddleCompleted, createConfiguredItem(riddle, GuiItems.lock("&f&l???")));
            applyLore(riddleItem, riddleCompleted != null ? riddleCompleted : riddle);
        } else {
            riddleItem = createConfiguredItem(riddle, GuiItems.lock("&f&l???"));
            applyLore(riddleItem, riddle);
        }
        gui.setItem(10, riddleItem);

        // Slot 11 lance la quête de l'Electric Goat après la validation de l'énigme.
        ConfigurationSection goat = plugin.getConfig().getConfigurationSection("quest-master.slots.11");
        ItemStack goatItem;
        if (!plugin.getQuestManager().isCompleted(player)) {
            goatItem = GuiItems.lock(getDisplayName(goat, "&c&lLocked Boss"));
            applyLore(goatItem, goat);
        } else if (plugin.getQuestManager().isQuest2Completed(player)) {
            ConfigurationSection completed = goat == null ? null : goat.getConfigurationSection("completed");
            goatItem = createConfiguredItem(completed, GuiItems.goat("&a&lElectric Goat &2✓"));
            applyLore(goatItem, completed);
        } else {
            ConfigurationSection unlocked = goat == null ? null : goat.getConfigurationSection("unlocked");
            goatItem = createConfiguredItem(unlocked, GuiItems.goat("&a&lElectric Goat"));
            applyLore(goatItem, unlocked);
        }
        gui.setItem(11, goatItem);

        for (int slot = 12; slot <= 16; slot++) {
            ConfigurationSection section = plugin.getConfig().getConfigurationSection("quest-master.slots." + slot);
            ItemStack item = GuiItems.lock(getDisplayName(section, "&c&lVERROUILLÉ"));
            applyLore(item, section);
            gui.setItem(slot, item);
        }

        // Slot 22 : téléportation aléatoire dans le monde quest.
        ConfigurationSection randomTp = plugin.getConfig()
                .getConfigurationSection("quest-master.random-teleport");
        ItemStack randomTpItem = new ItemStack(Material.valueOf(
                plugin.getConfig().getString("quest-master.random-teleport.material", "NETHER_STAR")));
        ItemMeta randomMeta = randomTpItem.getItemMeta();
        if (randomMeta != null) {
            randomMeta.setDisplayName(getDisplayName(randomTp, "&7&lTéléportation aléatoire"));
            applyLore(randomTpItem, randomTp);
        }
        gui.setItem(22, randomTpItem);

        player.openInventory(gui);
    }

    private ItemStack createQuestItem(ConfigurationSection section, Player player) {
        ConfigurationSection displaySection = section;
        if (plugin.getQuestManager().isQuest2Completed(player)) {
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
