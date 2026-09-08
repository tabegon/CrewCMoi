package fr.crewcmoi.economie.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.other.managers.DatabaseManager;
import org.bukkit.Bukkit;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EconomyManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public EconomyManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void loadPlayer(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = databaseManager.loadOrCreatePlayer(uuid, name);
            cache.put(uuid, data);
        });
    }

    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.savePlayer(data));
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        PlayerData cached = cache.get(uuid);
        if (cached != null) {
            return cached;
        }
        return databaseManager.getPlayer(uuid);
    }

    public PlayerData getPlayerDataByName(String name) {
        for (PlayerData data : cache.values()) {
            if (data.getName().equalsIgnoreCase(name)) {
                return data;
            }
        }
        return databaseManager.getPlayerByName(name);
    }

    public double getBalance(UUID uuid) {
        PlayerData data = getPlayerData(uuid);
        return data != null ? data.getBalance() : 0.0;
    }

    public boolean has(UUID uuid, double amount) {
        return getBalance(uuid) >= amount;
    }

    public void deposit(UUID uuid, double amount) {
        if (amount <= 0) return;
        PlayerData data = getPlayerData(uuid);
        if (data == null) return;
        data.addBalance(amount);
        persist(data);
    }

    public boolean withdraw(UUID uuid, double amount) {
        if (amount <= 0) return false;
        PlayerData data = getPlayerData(uuid);
        if (data == null) return false;

        boolean allowNegative = plugin.getConfig().getBoolean("economy.allow-negative-balance", false);
        if (!allowNegative && data.getBalance() < amount) {
            return false;
        }

        data.removeBalance(amount);
        persist(data);
        return true;
    }

    public void setBalance(UUID uuid, double amount) {
        PlayerData data = getPlayerData(uuid);
        if (data == null) return;
        data.setBalance(amount);
        persist(data);
    }

    private void persist(PlayerData data) {
        if (cache.containsKey(data.getUuid())) {
            cache.put(data.getUuid(), data);
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.savePlayer(data));
    }

    public List<PlayerData> getTopBalances(int limit) {
        return databaseManager.getTopBalances(limit);
    }

    public boolean isOnline(UUID uuid) {
        return cache.containsKey(uuid);
    }

    public Main getPlugin() {
        return plugin;
    }
}
