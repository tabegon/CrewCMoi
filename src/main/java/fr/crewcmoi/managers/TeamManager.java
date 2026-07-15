package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.TeamData;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Gère la logique des équipes (team) : création, ajout/retrait de membres, dissolution.
 * Maintient un cache UUID joueur -> id équipe pour un accès synchrone rapide
 * (utilisé notamment par le système de prime/malus lors des morts en combat).
 */
public class TeamManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    // Cache : uuid joueur -> id équipe. Alimenté au join, mis à jour lors des actions team.
    private final Map<UUID, Integer> playerTeamCache = new ConcurrentHashMap<>();

    public TeamManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public enum CreateResult {
        SUCCESS, ALREADY_IN_TEAM, NAME_TAKEN, INVALID_NAME, ERROR
    }

    public enum AddResult {
        SUCCESS, NOT_OWNER, NO_TEAM, TARGET_ALREADY_IN_TEAM, TARGET_NOT_FOUND, ERROR
    }

    /**
     * Charge en cache l'équipe éventuelle d'un joueur qui vient de se connecter.
     */
    public void loadPlayer(UUID uuid) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TeamData team = databaseManager.getTeamByPlayer(uuid);
            if (team != null) {
                playerTeamCache.put(uuid, team.getId());
            } else {
                playerTeamCache.remove(uuid);
            }
        });
    }

    public void unloadPlayer(UUID uuid) {
        playerTeamCache.remove(uuid);
    }

    /**
     * Indique si le joueur fait partie d'une équipe (lecture synchrone via le cache).
     */
    public boolean hasTeam(UUID uuid) {
        return playerTeamCache.containsKey(uuid);
    }

    public void createTeam(Player owner, String name, Consumer<CreateResult> callback) {
        if (name == null || !name.matches("[a-zA-Z0-9_]{3,16}")) {
            callback.accept(CreateResult.INVALID_NAME);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TeamData existing = databaseManager.getTeamByPlayer(owner.getUniqueId());
            if (existing != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(CreateResult.ALREADY_IN_TEAM));
                return;
            }

            TeamData taken = databaseManager.getTeamByName(name);
            if (taken != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(CreateResult.NAME_TAKEN));
                return;
            }

            int id = databaseManager.createTeam(name, owner.getUniqueId(), owner.getName());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (id == -1) {
                    callback.accept(CreateResult.ERROR);
                } else {
                    playerTeamCache.put(owner.getUniqueId(), id);
                    callback.accept(CreateResult.SUCCESS);
                }
            });
        });
    }

    public void addMember(Player owner, String targetName, Consumer<AddResult> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TeamData team = databaseManager.getTeamByPlayer(owner.getUniqueId());
            if (team == null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(AddResult.NO_TEAM));
                return;
            }
            if (!team.isOwner(owner.getUniqueId())) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(AddResult.NOT_OWNER));
                return;
            }

            Player targetOnline = Bukkit.getPlayerExact(targetName);
            UUID targetUuid;
            String resolvedName;
            if (targetOnline != null) {
                targetUuid = targetOnline.getUniqueId();
                resolvedName = targetOnline.getName();
            } else {
                var offline = Bukkit.getOfflinePlayer(targetName);
                if (offline == null || offline.getName() == null || !offline.hasPlayedBefore()) {
                    Bukkit.getScheduler().runTask(plugin, () -> callback.accept(AddResult.TARGET_NOT_FOUND));
                    return;
                }
                targetUuid = offline.getUniqueId();
                resolvedName = offline.getName();
            }

            if (databaseManager.getTeamByPlayer(targetUuid) != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(AddResult.TARGET_ALREADY_IN_TEAM));
                return;
            }

            int maxMembers = plugin.getConfig().getInt("team.max-members", 0);
            if (maxMembers > 0 && team.size() >= maxMembers) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(AddResult.ERROR));
                return;
            }

            boolean success = databaseManager.addTeamMember(team.getId(), targetUuid, resolvedName);
            UUID finalTargetUuid = targetUuid;
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (success) {
                    playerTeamCache.put(finalTargetUuid, team.getId());
                    callback.accept(AddResult.SUCCESS);
                } else {
                    callback.accept(AddResult.ERROR);
                }
            });
        });
    }

    /**
     * Fait quitter son équipe à un joueur. Si le propriétaire quitte, l'équipe est dissoute.
     */
    public void leaveTeam(Player player, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TeamData team = databaseManager.getTeamByPlayer(player.getUniqueId());
            if (team == null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(false));
                return;
            }

            if (team.isOwner(player.getUniqueId())) {
                databaseManager.deleteTeam(team.getId());
                for (UUID member : team.getMembers().keySet()) {
                    playerTeamCache.remove(member);
                }
            } else {
                databaseManager.removeTeamMember(player.getUniqueId());
                playerTeamCache.remove(player.getUniqueId());
            }

            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(true));
        });
    }

    public void getTeamAsync(UUID playerUuid, Consumer<TeamData> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TeamData team = databaseManager.getTeamByPlayer(playerUuid);
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(team));
        });
    }
}
