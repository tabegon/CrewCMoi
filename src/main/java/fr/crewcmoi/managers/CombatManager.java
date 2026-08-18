package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

    // Agresseur de l'affrontement en cours pour chaque joueur impliqué (peut être lui-même
    // s'il a porté le premier coup, ou l'adversaire si c'est lui qui l'a attaqué en premier).
    // Utilisé pour déterminer qui a "commencé le combat" au moment d'une mort (système de prime/malus).
    private final Map<UUID, UUID> engagementAggressor = new ConcurrentHashMap<>();

    // Adversaire direct actuel de chaque joueur en combat (dernier joueur avec qui il a échangé
    // des coups). Utilisé pour stopper le combat log des deux joueurs dès que l'un d'eux meurt.
    private final Map<UUID, UUID> currentOpponent = new ConcurrentHashMap<>();

    // Tâche répétitive affichant le compte à rebours de combat dans l'action bar des
    // joueurs actuellement tagués.
    private BukkitTask actionBarTask;

    public CombatManager(Main plugin) {
        this.plugin = plugin;
        this.combatDurationSeconds = plugin.getConfig().getInt("combat-log.duration-seconds", 30);
    }

    /**
     * Démarre la tâche répétitive qui affiche "Combat : Xs" dans l'action bar de chaque
     * joueur actuellement en combat. À appeler une seule fois, au démarrage du plugin.
     */
    public void startActionBar() {
        if (actionBarTask != null) {
            return;
        }
        actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (UUID uuid : combatExpiry.keySet()) {
                Player player = Bukkit.getPlayer(uuid);
                if (player == null || !player.isOnline()) {
                    continue;
                }
                int remaining = getRemainingSeconds(player);
                if (remaining <= 0) {
                    continue;
                }
                player.sendActionBar(Component.text("Combat : " + remaining + "s", NamedTextColor.RED));
            }
        }, 0L, 20L);
    }

    /**
     * Arrête la tâche d'action bar (à appeler au disable du plugin).
     */
    public void stopActionBar() {
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
    }

    /**
     * Met (ou remet) un joueur en combat pour la durée configurée.
     */
    public void tagCombat(Player player) {
        UUID uuid = player.getUniqueId();
        combatExpiry.put(uuid, System.currentTimeMillis() + (combatDurationSeconds * 1000L));

        // Empêche de fuir un combat en élytre : si le joueur est déjà en vol plané au
        // moment où il est tagué (ou re-tagué), on le force à atterrir.
        if (player.isGliding()) {
            player.setGliding(false);
        }

        BukkitTask existing = combatTasks.get(uuid);
        if (existing != null) {
            existing.cancel();
        }

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            combatTasks.remove(uuid);
            combatExpiry.remove(uuid);
            firstAttacker.remove(uuid);
            engagementAggressor.remove(uuid);
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
        UUID attackerId = attacker.getUniqueId();
        boolean victimWasInCombat = isInCombat(victim);
        boolean attackerWasInCombat = isInCombat(attacker);

        if (!victimWasInCombat) {
            firstAttacker.put(victimId, attackerId);
            sendMessage(victim, "combat-log.tagged");
        }
        if (!attackerWasInCombat) {
            sendMessage(attacker, "combat-log.tagged");
        }

        // Détermine l'agresseur de cet affrontement : le premier des deux à avoir frappé.
        if (!victimWasInCombat && !attackerWasInCombat) {
            // Nouvel engagement : l'attaquant est l'agresseur pour les deux joueurs.
            engagementAggressor.put(victimId, attackerId);
            engagementAggressor.put(attackerId, attackerId);
        } else if (!victimWasInCombat) {
            // La victime rejoint un affrontement où l'attaquant était déjà engagé ailleurs :
            // on hérite de l'agresseur connu de l'attaquant (par défaut lui-même).
            UUID knownAggressor = engagementAggressor.getOrDefault(attackerId, attackerId);
            engagementAggressor.put(victimId, knownAggressor);
        } else if (!engagementAggressor.containsKey(attackerId)) {
            engagementAggressor.put(attackerId, engagementAggressor.getOrDefault(victimId, attackerId));
        }

        currentOpponent.put(victimId, attackerId);
        currentOpponent.put(attackerId, victimId);

        // Les DEUX joueurs doivent être tagués en combat (et donc présents dans
        // combatExpiry) pour que la tâche d'action bar (voir startActionBar) affiche le
        // compte à rebours des deux côtés. Avant ce correctif, seule la victime était
        // taguée : l'attaquant ne se retrouvait jamais dans combatExpiry et ne voyait donc
        // jamais l'action bar "Combat : Xs" pendant qu'il combat.
        tagCombat(victim);
        tagCombat(attacker);
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
     * Retourne l'agresseur de l'affrontement en cours pour ce joueur : lui-même s'il a porté
     * le premier coup, ou l'adversaire si c'est lui qui a initié l'agression. Peut retourner null
     * si le joueur n'est pas/plus en combat suivi.
     */
    public UUID getAggressor(Player player) {
        return engagementAggressor.get(player.getUniqueId());
    }

    public UUID getOpponent(Player player) {
        return currentOpponent.get(player.getUniqueId());
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
        engagementAggressor.remove(uuid);
        currentOpponent.remove(uuid);
    }

    /**
     * Stoppe immédiatement le combat log des deux joueurs impliqués dans un affrontement
     * dès que l'un des deux meurt, sans envoyer le message "combat-ended" (ils viennent
     * déjà de recevoir un message de mort / de victoire).
     */
    public void stopCombatForBoth(Player player) {
        UUID opponentId = currentOpponent.get(player.getUniqueId());
        clearCombat(player);
        if (opponentId != null) {
            Player opponent = Bukkit.getPlayer(opponentId);
            if (opponent != null) {
                clearCombat(opponent);
            }
        }
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
