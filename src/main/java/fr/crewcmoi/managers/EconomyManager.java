package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère la logique économique du plugin : cache des données joueurs en ligne,
 * opérations de dépôt/retrait/set, et accès à la base de données.
 */
public class EconomyManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    // Cache des joueurs actuellement en ligne (accès rapide, thread-safe)
    private final Map<UUID, PlayerData> cache = new ConcurrentHashMap<>();

    public EconomyManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Charge les données d'un joueur en cache de manière asynchrone.
     */
    public void loadPlayer(UUID uuid, String name) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = databaseManager.loadOrCreatePlayer(uuid, name);
            cache.put(uuid, data);
        });
    }

    /**
     * Décharge un joueur du cache (à appeler à la déconnexion), en sauvegardant au préalable.
     */
    public void unloadPlayer(UUID uuid) {
        PlayerData data = cache.remove(uuid);
        if (data != null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> databaseManager.savePlayer(data));
        }
    }

    /**
     * Récupère les données d'un joueur, en cache si disponible, sinon en base (synchrone, à utiliser hors du thread principal si possible).
     */
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

    /**
     * Ajoute de l'argent au solde d'un joueur.
     */
    public void deposit(UUID uuid, double amount) {
        if (amount <= 0) return;
        PlayerData data = getPlayerData(uuid);
        if (data == null) return;
        data.addBalance(amount);
        persist(data);
    }

    /**
     * Retire de l'argent au solde d'un joueur. Ne descend jamais en dessous de 0
     * sauf si "economy.allow-negative-balance" est activé dans la config.
     */
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

    /**
     * Définit directement le solde d'un joueur.
     */
    public void setBalance(UUID uuid, double amount) {
        PlayerData data = getPlayerData(uuid);
        if (data == null) return;
        data.setBalance(amount);
        persist(data);
    }

    /**
     * Sauvegarde une donnée joueur : met à jour le cache si présent, et sauvegarde en base de façon asynchrone.
     */
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
