package fr.crewcmoi.pvp.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.pvp.managers.DuelManager;
import fr.crewcmoi.pvp.managers.TeamManager;
import fr.crewcmoi.pvp.utils.HeadSellPrice;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerHeadDropListener implements Listener {
    private final Main plugin;
    private final DuelManager duelManager;
    private final TeamManager teamManager;
    private final File dataFile;
    private YamlConfiguration data;
    private String loadedDate;

    public PlayerHeadDropListener(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.teamManager = plugin.getTeamManager();
        this.dataFile = new File(plugin.getDataFolder(), "player-head-drops.yml");
        loadData();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean("player-head-drop.enabled", true)) return;

        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.getUniqueId().equals(victim.getUniqueId())) return;
        if (duelManager.getSession(victim.getUniqueId()) != null) return;
        if (teamManager != null && teamManager.isSameTeam(victim.getUniqueId(), killer.getUniqueId())) return;

        rollDateIfNeeded();
        int maxPerDay = Math.max(0, plugin.getConfig().getInt("player-head-drop.max-per-day", 3));
        if (maxPerDay == 0) return;

        String path = "drops." + victim.getUniqueId();
        int currentDrops = data.getInt(path, 0);
        if (currentDrops >= maxPerDay) return;

        double price = plugin.getConfig().getDouble("player-head-drop.price", 250.0);
        ItemStack head = createHead(plugin, victim, price);
        if (head == null) return;

        victim.getWorld().dropItemNaturally(victim.getLocation(), head);
        data.set(path, currentDrops + 1);
        saveData();
    }

    private void loadData() {
        data = YamlConfiguration.loadConfiguration(dataFile);
        loadedDate = LocalDate.now(ZoneId.systemDefault()).toString();
        String storedDate = data.getString("date", "");
        if (!loadedDate.equals(storedDate)) {
            data.set("drops", null);
            data.set("date", loadedDate);
            saveData();
        }
    }

    private void rollDateIfNeeded() {
        String today = LocalDate.now(ZoneId.systemDefault()).toString();
        if (today.equals(loadedDate)) return;
        loadedDate = today;
        data.set("date", today);
        data.set("drops", null);
        saveData();
    }

    private void saveData() {
        try {
            File parent = dataFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Impossible de créer le dossier pour player-head-drops.yml.");
                return;
            }
            data.save(dataFile);
        } catch (IOException error) {
            plugin.getLogger().warning("Impossible de sauvegarder les limites de drop des têtes : " + error.getMessage());
        }
    }

    public static ItemStack createHead(Main plugin, Player victim, double price) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return null;

        meta.setOwningPlayer(victim);
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fTête de &e" + victim.getName()));
        String currency = plugin.getConfig().getString("economy.currency-symbol", "");
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.translateAlternateColorCodes('&', "&7Prix de vente : &a" + MoneyFormat.format(price) + currency));
        meta.setLore(lore);
        HeadSellPrice.apply(plugin, meta, price);
        head.setItemMeta(meta);
        return head;
    }
}
