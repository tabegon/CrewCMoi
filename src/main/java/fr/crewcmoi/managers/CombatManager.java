package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère le "combat log" : lorsqu'un joueur est tagué en combat, il ne peut plus
 * se téléporter ni utiliser d'élytre pendant une durée définie. S'il se déconnecte
 * pendant ce délai, il est tué et le premier joueur l'ayant frappé est mémorisé
 * (par exemple pour une future récompense/statistique).
 */
public class CombatManager {

    private final Main plugin;

    // Durée du tag de combat en secondes (configurable)
    private final int combatDurationSeconds;

    // Joueurs actuellement en combat -> tâche de fin de combat programmée
    private final Map<UUID, BukkitTask> combatTasks = new ConcurrentHashMap<>();

    // Timestamp (millis) auquel le tag de combat de chaque joueur expire
    private final Map<UUID, Long> combatExpiry = new ConcurrentHashMap<>();

    // Premier joueur ayant frappé un joueur donné durant son combat en cours
    private final Map<UUID, UUID> firstAttacker = new ConcurrentHashMap<>();

    public CombatManager(Main plugin) {
        this.plugin = plugin;
        this.combatDurationSeconds = plugin.getConfig().getInt("combat-log.duration-seconds", 30);
    }

    /**
     * Met (ou remet) un joueur en combat pour la durée configurée.
     */
    public void tagCombat(Player player) {
        UUID uuid = player.getUniqueId();
        combatExpiry.put(uuid, System.currentTimeMillis() + (combatDurationSeconds * 1000L));

        BukkitTask existing = combatTasks.get(uuid);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            combatTasks.remove(uuid);
            combatExpiry.remove(uuid);
            firstAttacker.remove(uuid);
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && p.isOnline()) {
                sendMessage(p, "combat-log.combat-ended");
            }
        }, combatDurationSeconds * 20L);

        combatTasks.put(uuid, task);
    }

    /**
     * Enregistre une attaque : tague la victime en combat et retient l'attaquant
     * s'il s'agit du premier coup reçu depuis le début de ce combat.
     */
    public void registerAttack(Player victim, Player attacker) {
        UUID victimId = victim.getUniqueId();
        boolean wasInCombat = isInCombat(victim);

        if (!wasInCombat) {
            firstAttacker.put(victimId, attacker.getUniqueId());
            sendMessage(victim, "combat-log.tagged");
        }

        tagCombat(victim);
    }

    /**
     * Remet le combat à zéro (30s) sans changer le premier attaquant enregistré.
     * Utilisé lors de l'utilisation de pearls / xp pendant un combat en cours.
     */
    public void refreshCombat(Player player) {
        if (isInCombat(player)) {
            tagCombat(player);
        }
    }

    public boolean isInCombat(Player player) {
        Long expiry = combatExpiry.get(player.getUniqueId());
        return expiry != null && expiry > System.currentTimeMillis();
    }

    public int getRemainingSeconds(Player player) {
        Long expiry = combatExpiry.get(player.getUniqueId());
        if (expiry == null) {
            return 0;
        }
        long remainingMs = expiry - System.currentTimeMillis();
        return (int) Math.max(0, Math.ceil(remainingMs / 1000.0));
    }

    public UUID getFirstAttacker(Player player) {
        return firstAttacker.get(player.getUniqueId());
    }

    /**
     * Retire immédiatement le tag de combat d'un joueur (ex: après sa mort).
     */
    public void clearCombat(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitTask task = combatTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
        combatExpiry.remove(uuid);
        firstAttacker.remove(uuid);
    }

    private void sendMessage(Player player, String path) {
        String message = plugin.getMessages().getString(path);
        if (message == null) {
            return;
        }
        String prefix = plugin.getMessages().getString("prefix", "");
        player.sendMessage((prefix + message).replace('&', '§'));
    }
}
