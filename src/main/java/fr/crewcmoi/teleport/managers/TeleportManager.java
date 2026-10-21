package fr.crewcmoi.teleport.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.CombatManager;
import fr.crewcmoi.pvp.managers.DuelArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class TeleportManager {
    public enum RequestType { TPA, TPAHERE }

    public static class TeleportRequest {
        private final UUID requesterUuid;
        private final RequestType type;
        private final BukkitTask expiryTask;
        private final long createdAt;

        public TeleportRequest(UUID requesterUuid, RequestType type, BukkitTask expiryTask) {
            this.requesterUuid = requesterUuid;
            this.type = type;
            this.expiryTask = expiryTask;
            this.createdAt = System.currentTimeMillis();
        }

        public UUID getRequesterUuid() { return requesterUuid; }
        public RequestType getType() { return type; }
        public long getCreatedAt() { return createdAt; }
    }

    private final Main plugin;
    private final CombatManager combatManager;
    private final int expirySeconds;
    private final Map<UUID, Map<UUID, TeleportRequest>> pendingRequests = new ConcurrentHashMap<>();

    public TeleportManager(Main plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.expirySeconds = plugin.getConfig().getInt("tpa.expiry-seconds", 60);
    }

    public int getExpirySeconds() { return expirySeconds; }

    public boolean isInDuelArena(Player player) {
        if (player == null || plugin.getDuelArenaManager() == null) return false;
        return plugin.getDuelArenaManager().isInsideArena(player.getLocation());
    }

    public boolean isArenaTeleportBlocked(Player first, Player second) {
        return isInDuelArena(first) || isInDuelArena(second);
    }

    public void sendArenaBlocked(Player player) {
        send(player, "tpa.arena-blocked", null, null);
    }

    public void createRequest(Player requester, Player target, RequestType type) {
        UUID targetUuid = target.getUniqueId();
        UUID requesterUuid = requester.getUniqueId();
        Map<UUID, TeleportRequest> requests = pendingRequests.computeIfAbsent(targetUuid, key -> new ConcurrentHashMap<>());
        TeleportRequest old = requests.remove(requesterUuid);
        if (old != null && old.expiryTask != null) old.expiryTask.cancel();

        AtomicReference<TeleportRequest> requestReference = new AtomicReference<>();
        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TeleportRequest expected = requestReference.get();
            if (expected == null) return;
            Map<UUID, TeleportRequest> currentRequests = pendingRequests.get(targetUuid);
            if (currentRequests == null || !currentRequests.remove(requesterUuid, expected)) return;
            if (currentRequests.isEmpty()) pendingRequests.remove(targetUuid, currentRequests);

            Player requesterPlayer = Bukkit.getPlayer(requesterUuid);
            if (requesterPlayer != null && requesterPlayer.isOnline()) {
                send(requesterPlayer, "tpa.expired-requester", "{player}", target.getName());
            }
            Player targetPlayer = Bukkit.getPlayer(targetUuid);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                send(targetPlayer, "tpa.expired-target", "{player}", requester.getName());
            }
        }, expirySeconds * 20L);
        TeleportRequest request = new TeleportRequest(requesterUuid, type, expiryTask);
        requestReference.set(request);
        requests.put(requesterUuid, request);
    }

    public TeleportRequest getRequest(UUID targetUuid) {
        Map<UUID, TeleportRequest> requests = pendingRequests.get(targetUuid);
        if (requests == null || requests.isEmpty()) return null;
        return requests.values().stream().max(Comparator.comparingLong(TeleportRequest::getCreatedAt)).orElse(null);
    }

    public void cancelRequest(UUID targetUuid) {
        Map<UUID, TeleportRequest> removed = pendingRequests.remove(targetUuid);
        if (removed != null) removed.values().forEach(request -> {
            if (request.expiryTask != null) request.expiryTask.cancel();
        });
    }

    public void accept(Player target) {
        TeleportRequest request = getRequest(target.getUniqueId());
        if (request == null) {
            send(target, "tpa.no-request", null, null);
            return;
        }
        removeRequest(target.getUniqueId(), request.getRequesterUuid());
        acceptRequest(target, request);
    }

    public void acceptAll(Player target, boolean teamOnly) {
        Map<UUID, TeleportRequest> requests = pendingRequests.get(target.getUniqueId());
        if (requests == null || requests.isEmpty()) {
            send(target, "tpa.no-request", null, null);
            return;
        }

        List<TeleportRequest> selected = new ArrayList<>();
        for (TeleportRequest request : requests.values()) {
            if (!teamOnly || (plugin.getTeamManager() != null
                    && plugin.getTeamManager().isSameTeam(target.getUniqueId(), request.getRequesterUuid()))) {
                selected.add(request);
            }
        }
        if (selected.isEmpty()) {
            target.sendMessage(ChatColor.RED + (teamOnly
                    ? "Aucune demande de téléportation récente d'un membre de ton équipe."
                    : "Aucune demande de téléportation en attente."));
            return;
        }

        selected.sort(Comparator.comparingLong(TeleportRequest::getCreatedAt));
        for (TeleportRequest request : selected) {
            removeRequest(target.getUniqueId(), request.getRequesterUuid());
            acceptRequest(target, request);
        }
        target.sendMessage(ChatColor.GREEN + (teamOnly
                ? "Demandes de téléportation de ton équipe traitées : "
                : "Demandes de téléportation traitées : ") + selected.size() + ".");
    }

    private void removeRequest(UUID targetUuid, UUID requesterUuid) {
        Map<UUID, TeleportRequest> requests = pendingRequests.get(targetUuid);
        if (requests == null) return;
        TeleportRequest removed = requests.remove(requesterUuid);
        if (removed != null && removed.expiryTask != null) removed.expiryTask.cancel();
        if (requests.isEmpty()) pendingRequests.remove(targetUuid, requests);
    }

    private void acceptRequest(Player target, TeleportRequest request) {
        Player requester = Bukkit.getPlayer(request.getRequesterUuid());
        if (requester == null || !requester.isOnline()) {
            send(target, "tpa.requester-offline", null, null);
            return;
        }

        if (isArenaTeleportBlocked(requester, target)) {
            sendArenaBlocked(requester);
            sendArenaBlocked(target);
            return;
        }

        Player combatant = null;
        if (combatManager != null) {
            if (combatManager.isInCombat(requester)) {
                combatant = requester;
            } else if (combatManager.isInCombat(target)) {
                combatant = target;
            }
        }
        if (combatant != null) {
            String prefix = plugin.getMessages().getString("prefix", "");
            String message = plugin.getMessages().getString("tpa.moving-in-combat", "&c{player} est en combat, annulation.")
                    .replace("{player}", combatant.getName());
            target.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
            return;
        }

        if (request.getType() == RequestType.TPA) requester.teleport(target.getLocation());
        else target.teleport(requester.getLocation());

        send(requester, "tpa.accepted-requester", "{player}", target.getName());
        send(target, "tpa.accepted-target", "{player}", requester.getName());
    }

    private void send(Player player, String path, String placeholder, String value) {
        String prefix = plugin.getMessages().getString("prefix", "");
        String message = plugin.getMessages().getString(path, "&cAucune demande disponible.");
        if (placeholder != null && value != null) message = message.replace(placeholder, value);
        player.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
