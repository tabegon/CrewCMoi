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

    // Invitations en attente : uuid du joueur invité -> uuid du propriétaire qui l'a invité.
    // Une invitation expire automatiquement après 60 secondes si elle n'est ni acceptée ni refusée.
    private final Map<UUID, UUID> pendingInvites = new ConcurrentHashMap<>();
    private static final long INVITE_EXPIRY_TICKS = 20L * 60L;

    public TeamManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public enum CreateResult {
        SUCCESS, ALREADY_IN_TEAM, NAME_TAKEN, INVALID_NAME, ERROR
    }

    public enum InviteResult {
        SUCCESS, NOT_OWNER, NO_TEAM, TARGET_ALREADY_IN_TEAM, TARGET_NOT_FOUND, TARGET_NOT_ONLINE, ALREADY_INVITED, ERROR
    }

    public enum AcceptResult {
        SUCCESS, NO_PENDING_INVITE, INVITER_OFFLINE, TARGET_ALREADY_IN_TEAM, TEAM_GONE, ERROR
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
        pendingInvites.remove(uuid);
    }

    /**
     * Indique si le joueur fait partie d'une équipe (lecture synchrone via le cache).
     */
    public boolean hasTeam(UUID uuid) {
        return playerTeamCache.containsKey(uuid);
    }

    /**
     * Indique (lecture synchrone via le cache) si deux joueurs font partie de la même
     * équipe. Utilisé notamment pour exclure les contributions de prime du tueur et de
     * ses alliés lors du calcul de la prime "légitime" d'une victime.
     */
    public boolean isSameTeam(UUID a, UUID b) {
        Integer teamA = playerTeamCache.get(a);
        return teamA != null && teamA.equals(playerTeamCache.get(b));
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

    /**
     * Envoie une invitation à rejoindre l'équipe du propriétaire. Le joueur ciblé doit
     * l'accepter via /team accept avant de rejoindre effectivement l'équipe.
     */
    public void inviteMember(Player owner, String targetName, Consumer<InviteResult> callback) {
        Player targetOnline = Bukkit.getPlayerExact(targetName);
        if (targetOnline == null) {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                var offline = Bukkit.getOfflinePlayer(targetName);
                boolean exists = offline != null && offline.getName() != null && offline.hasPlayedBefore();
                Bukkit.getScheduler().runTask(plugin, () ->
                        callback.accept(exists ? InviteResult.TARGET_NOT_ONLINE : InviteResult.TARGET_NOT_FOUND));
            });
            return;
        }

        UUID targetUuid = targetOnline.getUniqueId();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            TeamData team = databaseManager.getTeamByPlayer(owner.getUniqueId());
            if (team == null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(InviteResult.NO_TEAM));
                return;
            }
            if (!team.isOwner(owner.getUniqueId())) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(InviteResult.NOT_OWNER));
                return;
            }
            if (databaseManager.getTeamByPlayer(targetUuid) != null) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(InviteResult.TARGET_ALREADY_IN_TEAM));
                return;
            }

            int maxMembers = plugin.getConfig().getInt("team.max-members", 0);
            if (maxMembers > 0 && team.size() >= maxMembers) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(InviteResult.ERROR));
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                if (pendingInvites.containsKey(targetUuid)) {
                    callback.accept(InviteResult.ALREADY_INVITED);
                    return;
                }

                pendingInvites.put(targetUuid, owner.getUniqueId());
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    // N'annule que si l'invitation en attente est toujours la même (pas déjà traitée/remplacée).
                    if (owner.getUniqueId().equals(pendingInvites.get(targetUuid))) {
                        pendingInvites.remove(targetUuid);
                    }
                }, INVITE_EXPIRY_TICKS);

                callback.accept(InviteResult.SUCCESS);
            });
        });
    }

    public boolean hasPendingInvite(UUID targetUuid) {
        return pendingInvites.containsKey(targetUuid);
    }

    /**
     * Le joueur invité accepte l'invitation en attente et rejoint effectivement l'équipe.
     */
    public void acceptInvite(Player player, Consumer<AcceptResult> callback) {
        UUID ownerUuid = pendingInvites.get(player.getUniqueId());
        if (ownerUuid == null) {
            callback.accept(AcceptResult.NO_PENDING_INVITE);
            return;
        }

        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner == null) {
            pendingInvites.remove(player.getUniqueId());
            callback.accept(AcceptResult.INVITER_OFFLINE);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            if (databaseManager.getTeamByPlayer(player.getUniqueId()) != null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    pendingInvites.remove(player.getUniqueId());
                    callback.accept(AcceptResult.TARGET_ALREADY_IN_TEAM);
                });
                return;
            }

            TeamData team = databaseManager.getTeamByPlayer(ownerUuid);
            if (team == null) {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    pendingInvites.remove(player.getUniqueId());
                    callback.accept(AcceptResult.TEAM_GONE);
                });
                return;
            }

            boolean success = databaseManager.addTeamMember(team.getId(), player.getUniqueId(), player.getName());
            Bukkit.getScheduler().runTask(plugin, () -> {
                pendingInvites.remove(player.getUniqueId());
                if (success) {
                    playerTeamCache.put(player.getUniqueId(), team.getId());
                    callback.accept(AcceptResult.SUCCESS);
                } else {
                    callback.accept(AcceptResult.ERROR);
                }
            });
        });
    }

    /**
     * Le joueur invité refuse l'invitation en attente.
     *
     * @return l'uuid du propriétaire ayant invité, ou null s'il n'y avait aucune invitation en attente.
     */
    public UUID denyInvite(Player player) {
        return pendingInvites.remove(player.getUniqueId());
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
