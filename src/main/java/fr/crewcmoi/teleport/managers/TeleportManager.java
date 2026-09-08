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

public class TeleportManager {

    public enum RequestType {
        
        TPA,
        
        TPAHERE
    }

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

    private final Map<UUID, TeleportRequest> pendingRequests = new ConcurrentHashMap<>();

    public TeleportManager(Main plugin, CombatManager combatManager) {
        this.plugin = plugin;
        this.combatManager = combatManager;
        this.expirySeconds = plugin.getConfig().getInt("tpa.expiry-seconds", 60);
    }

    public int getExpirySeconds() {
        return expirySeconds;
    }

    public void createRequest(Player requester, Player target, RequestType type) {
        UUID targetUuid = target.getUniqueId();

        cancelRequest(targetUuid);

        BukkitTask expiryTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            TeleportRequest current = pendingRequests.get(targetUuid);
            if (current != null && current.getRequesterUuid().equals(requester.getUniqueId())) {
                pendingRequests.remove(targetUuid);

                Player requesterPlayer = Bukkit.getPlayer(requester.getUniqueId());
                if (requesterPlayer != null && requesterPlayer.isOnline()) {
                    requesterPlayer.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            plugin.getMessages().getString("prefix") +
                                    plugin.getMessages().getString("tpa.expired-requester")
                                            .replace("{player}", target.getName())));
                }
                if (target.isOnline()) {
                    target.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                            plugin.getMessages().getString("prefix") +
                                    plugin.getMessages().getString("tpa.expired-target")
                                            .replace("{player}", requester.getName())));
                }
            }
        }, expirySeconds * 20L);

        pendingRequests.put(targetUuid, new TeleportRequest(requester.getUniqueId(), type, expiryTask));
    }

    public TeleportRequest getRequest(UUID targetUuid) {
        return pendingRequests.get(targetUuid);
    }

    public void cancelRequest(UUID targetUuid) {
        TeleportRequest existing = pendingRequests.remove(targetUuid);
        if (existing != null && existing.expiryTask != null) {
            existing.expiryTask.cancel();
        }
    }

    public void accept(Player target) {
        UUID targetUuid = target.getUniqueId();
        TeleportRequest request = pendingRequests.remove(targetUuid);

        if (request != null && request.expiryTask != null) {
            request.expiryTask.cancel();
        }

        if (request == null) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("tpa.no-request")));
            return;
        }

        Player requester = Bukkit.getPlayer(request.getRequesterUuid());
        if (requester == null || !requester.isOnline()) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("tpa.requester-offline")));
            return;
        }

        Player moving = (request.getType() == RequestType.TPA) ? requester : target;
        if (combatManager != null && combatManager.isInCombat(moving)) {
            target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("tpa.moving-in-combat")
                                    .replace("{player}", moving.getName())));
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("tpa.moving-in-combat")
                                    .replace("{player}", moving.getName())));
            return;
        }

        if (request.getType() == RequestType.TPA) {
            requester.teleport(target.getLocation());
        } else {
            target.teleport(requester.getLocation());
        }

        requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix") +
                        plugin.getMessages().getString("tpa.accepted-requester")
                                .replace("{player}", target.getName())));
        target.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix") +
                        plugin.getMessages().getString("tpa.accepted-target")
                                .replace("{player}", requester.getName())));
    }
}
