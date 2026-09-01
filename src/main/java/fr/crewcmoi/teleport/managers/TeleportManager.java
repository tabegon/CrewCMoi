package fr.crewcmoi.teleport.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère les demandes de téléportation entre joueurs (/tpa, /tpahere, /tpaccept).
 * Une seule demande en attente à la fois par joueur receveur : une nouvelle demande
 * écrase la précédente. Chaque demande expire après un délai configurable.
 */
public class TeleportManager {

    /**
     * Type de demande : détermine qui se téléporte vers qui une fois acceptée.
     */
    public enum RequestType {
        // /tpa : le demandeur se téléporte vers la cible (une fois que la cible accepte)
        TPA,
        // /tpahere : la cible se téléporte vers le demandeur (une fois que la cible accepte)
        TPAHERE
    }

    /**
     * Une demande de téléportation en attente.
     */
    public static class TeleportRequest {
        private final UUID requesterUuid;
        private final RequestType type;
        private final BukkitTask expiryTask;

        public TeleportRequest(UUID requesterUuid, RequestType type, BukkitTask expiryTask) {
            this.requesterUuid = requesterUuid;
            this.type = type;
            this.expiryTask = expiryTask;
        }

        public UUID getRequesterUuid() {
            return requesterUuid;
        }

        public RequestType getType() {
            return type;
        }
    }

    private final Main plugin;
    private final CombatManager combatManager;
    private final int expirySeconds;

    // Clé : UUID du joueur qui doit répondre (target) -> demande en attente le concernant
    private final Map<UUID, TeleportRequest> pendingRequests = new ConcurrentHashMap<>();

    public TeleportManager(Main plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.expirySeconds = plugin.getConfig().getInt("tpa.expiry-seconds", 60);
    }

    public int getExpirySeconds() {
        return expirySeconds;
    }

    /**
     * Enregistre une nouvelle demande de téléportation. Écrase toute demande précédente
     * en attente pour ce même receveur.
     */
    public void createRequest(Player requester, Player target, RequestType type) {
        UUID targetUuid = target.getUniqueId();

        // Annule l'ancienne demande (et sa tâche d'expiration) si elle existe
        cancelRequest(targetUuid);

        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TeleportRequest current = pendingRequests.get(targetUuid);
            if (current != null && current.getRequesterUuid().equals(requester.getUniqueId())) {
                pendingRequests.remove(targetUuid);

                Player requesterPlayer = Bukkit.getPlayer(requester.getUniqueId());
                if (requesterPlayer != null && requesterPlayer.isOnline()) {
                    requesterPlayer.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            plugin.getMessages().getString("prefix", "") +
                                    plugin.getMessages().getString("tpa.expired-requester", "&cVotre demande de téléportation a expiré.")
                                            .replace("{player}", target.getName())));
                }
                if (target.isOnline()) {
                    target.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            plugin.getMessages().getString("prefix", "") +
                                    plugin.getMessages().getString("tpa.expired-target", "&cLa demande de téléportation de {player} a expiré.")
                                            .replace("{player}", requester.getName())));
                }
            }
        }, expirySeconds * 20L);

        pendingRequests.put(targetUuid, new TeleportRequest(requester.getUniqueId(), type, expiryTask));
    }

    /**
     * Récupère la demande en attente pour ce joueur (celui qui doit répondre), ou null.
     */
    public TeleportRequest getRequest(UUID targetUuid) {
        return pendingRequests.get(targetUuid);
    }

    /**
     * Supprime et annule la tâche d'expiration de la demande en attente pour ce joueur, s'il y en a une.
     */
    public void cancelRequest(UUID targetUuid) {
        TeleportRequest existing = pendingRequests.remove(targetUuid);
        if (existing != null && existing.expiryTask != null) {
            existing.expiryTask.cancel();
        }
    }

    /**
     * Traite l'acceptation d'une demande par le joueur receveur : vérifie que la demande
     * est toujours valide, que les deux joueurs sont en ligne et pas en combat, effectue
     * la téléportation (dans le bon sens selon le type de demande) et envoie les messages
     * de confirmation. Retire la demande dans tous les cas si elle existait.
     */
    public void accept(Player target) {
        UUID targetUuid = target.getUniqueId();
        TeleportRequest request = pendingRequests.remove(targetUuid);

        if (request != null && request.expiryTask != null) {
            request.expiryTask.cancel();
        }

        if (request == null) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("tpa.no-request", "&cVous n'avez aucune demande de téléportation en attente.")));
            return;
        }

        Player requester = Bukkit.getPlayer(request.getRequesterUuid());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("tpa.requester-offline", "&cCe joueur n'est plus en ligne.")));
            return;
        }

        // Détermine qui doit effectivement se déplacer et vérifie que ce joueur n'est pas en combat.
        Player moving = (request.getType() == RequestType.TPA) ? requester : target;
        if (combatManager != null && combatManager.isInCombat(moving)) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("tpa.moving-in-combat", "&c{player} est en combat, la téléportation est annulée.")
                                    .replace("{player}", moving.getName())));
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("tpa.moving-in-combat", "&c{player} est en combat, la téléportation est annulée.")
                                    .replace("{player}", moving.getName())));
            return;
        }

        if (request.getType() == RequestType.TPA) {
            requester.teleport(target.getLocation());
        } else {
            target.teleport(requester.getLocation());
        }

        requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") +
                        plugin.getMessages().getString("tpa.accepted-requester", "&a{player} a accepté votre demande de téléportation.")
                                .replace("{player}", target.getName())));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") +
                        plugin.getMessages().getString("tpa.accepted-target", "&aVous avez accepté la téléportation de &e{player}&a.")
                                .replace("{player}", requester.getName())));
    }
}
