package fr.crewcmoi.halloween.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.managers.EconomyManager;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class HalloweenManager {
    private final Main plugin;
    private final EconomyManager economy;
    private final NamespacedKey tableKey;
    private final File dataFile;
    private final YamlConfiguration data;
    private final Set<String> tables = new HashSet<>();

    public HalloweenManager(Main plugin, EconomyManager economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.tableKey = new NamespacedKey(plugin, "halloween_table");
        this.dataFile = new File(plugin.getDataFolder(), "halloween.yml");
        if (!dataFile.exists()) {
            try { plugin.getDataFolder().mkdirs(); dataFile.createNewFile(); } catch (IOException e) { plugin.getLogger().warning("Impossible de créer halloween.yml"); }
        }
        this.data = YamlConfiguration.loadConfiguration(dataFile);
        tables.addAll(data.getStringList("tables"));
    }

    public Main getPlugin() { return plugin; }
    public EconomyManager getEconomy() { return economy; }

    public ItemStack createTableItem() {
        Material material = Material.ENCHANTING_TABLE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(color(plugin.getConfig().getString("halloween.table.item-name", "&5&lTable d'enchantement maudite")));
            meta.setLore(List.of(color("&7Cette table permet de relever"), color("&7un défi d'Halloween chaque jour.")));
            meta.getPersistentDataContainer().set(tableKey, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public boolean isTableItem(ItemStack item) {
        if (item == null || item.getType() != Material.ENCHANTING_TABLE || !item.hasItemMeta()) return false;
        Byte value = item.getItemMeta().getPersistentDataContainer().get(tableKey, PersistentDataType.BYTE);
        return value != null && value == (byte) 1;
    }

    public String locationKey(Location l) {
        return l.getWorld().getName() + ":" + l.getBlockX() + ":" + l.getBlockY() + ":" + l.getBlockZ();
    }
    public boolean isSpecialTable(Location l) { return tables.contains(locationKey(l)); }
    public void registerTable(Location l) { tables.add(locationKey(l)); save(); }
    public void unregisterTable(Location l) { tables.remove(locationKey(l)); save(); }

    public List<String> getChallengeIds() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("halloween.challenges");
        return section == null ? List.of() : new ArrayList<>(section.getKeys(false));
    }

    public boolean hasChosenToday(UUID uuid) {
        String path = "players." + uuid;
        return LocalDate.now().toString().equals(data.getString(path + ".date"));
    }
    public String getChosenChallenge(UUID uuid) {
        if (!hasChosenToday(uuid)) return null;
        return data.getString("players." + uuid + ".challenge");
    }
    public int getProgress(UUID uuid) { return data.getInt("players." + uuid + ".progress", 0); }

    public boolean chooseChallenge(Player player, String id) {
        if (hasChosenToday(player.getUniqueId())) return false;
        if (!plugin.getConfig().isConfigurationSection("halloween.challenges." + id)) return false;
        String path = "players." + player.getUniqueId();
        data.set(path + ".date", LocalDate.now().toString());
        data.set(path + ".challenge", id);
        data.set(path + ".progress", 0);
        data.set(path + ".completed", false);
        save();
        player.sendMessage(color(plugin.getConfig().getString("halloween.messages.challenge-selected", "&5Défi choisi : &d%challenge%&5 !").replace("%challenge%", getChallengeName(id))));
        return true;
    }

    public boolean isCompleted(UUID uuid) { return hasChosenToday(uuid) && data.getBoolean("players." + uuid + ".completed", false); }

    public void handleKill(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) return;
        ConfigurationSection c = activeChallenge(player);
        if (c == null || !c.getString("type", "KILL").equalsIgnoreCase("KILL")) return;
        String target = c.getString("target", "ANY");
        if (!target.equalsIgnoreCase("ANY") && !event.getEntityType().name().equalsIgnoreCase(target)) return;
        addProgress(player, c);
    }

    public void handleBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ConfigurationSection c = activeChallenge(player);
        if (c == null || !c.getString("type", "BREAK").equalsIgnoreCase("BREAK")) return;
        String target = c.getString("target", "ANY");
        if (!target.equalsIgnoreCase("ANY") && !event.getBlock().getType().name().equalsIgnoreCase(target)) return;
        addProgress(player, c);
    }

    private ConfigurationSection activeChallenge(Player p) {
        String id = getChosenChallenge(p.getUniqueId());
        if (id == null || isCompleted(p.getUniqueId())) return null;
        return plugin.getConfig().getConfigurationSection("halloween.challenges." + id);
    }

    private void addProgress(Player p, ConfigurationSection c) {
        int goal = Math.max(1, c.getInt("amount", 1));
        int current = Math.min(goal, getProgress(p.getUniqueId()) + 1);
        data.set("players." + p.getUniqueId() + ".progress", current);
        if (current >= goal) {
            data.set("players." + p.getUniqueId() + ".completed", true);
            giveReward(p, c.getConfigurationSection("reward"));
            p.sendMessage(color(plugin.getConfig().getString("halloween.messages.challenge-completed", "&aDéfi terminé !").replace("%challenge%", getChallengeName(getChosenChallenge(p.getUniqueId())))));
        }
        save();
    }

    public String getChallengeName(String id) {
        return color(plugin.getConfig().getString("halloween.challenges." + id + ".name", id));
    }

    private void giveReward(Player p, ConfigurationSection reward) {
        if (reward == null) return;
        if (reward.contains("money")) {
            double amount = reward.getDouble("money");
            if (amount > 0) economy.deposit(p.getUniqueId(), amount);
            else if (amount < 0) economy.withdraw(p.getUniqueId(), -amount);
        }
        for (String command : reward.getStringList("commands")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", p.getName()));
        }
        if (reward.isConfigurationSection("items")) {
            for (String key : reward.getConfigurationSection("items").getKeys(false)) {
                Material mat = Material.matchMaterial(key);
                if (mat == null) continue;
                int amount = Math.max(1, reward.getInt("items." + key, 1));
                p.getInventory().addItem(new ItemStack(mat, amount));
            }
        }
        String message = reward.getString("message");
        if (message != null && !message.isBlank()) p.sendMessage(color(message));
    }

    private void save() {
        data.set("tables", new ArrayList<>(tables));
        try { data.save(dataFile); } catch (IOException e) { plugin.getLogger().warning("Impossible de sauvegarder halloween.yml"); }
    }

    public String formatProgress(Player p) {
        String id = getChosenChallenge(p.getUniqueId());
        if (id == null) return "Aucun défi choisi aujourd'hui";
        ConfigurationSection c = plugin.getConfig().getConfigurationSection("halloween.challenges." + id);
        return getProgress(p.getUniqueId()) + "/" + (c == null ? 0 : c.getInt("amount", 1));
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s); }
}
