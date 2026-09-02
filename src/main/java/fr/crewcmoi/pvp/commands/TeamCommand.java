package fr.crewcmoi.pvp.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.database.TeamData;
import fr.crewcmoi.pvp.managers.TeamManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande /team :
 *  - /team create <nom>   : crée une équipe (le joueur en devient le propriétaire).
 *  - /team add <joueur>   : ajoute un joueur à votre équipe (propriétaire uniquement).
 *  - /team leave          : quitte votre équipe (dissout l'équipe si vous êtes le propriétaire).
 *  - /team disband        : dissout votre équipe (propriétaire uniquement).
 *  - /team info [nom]     : affiche les membres de votre équipe ou de celle indiquée.
 */
public class TeamCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final TeamManager teamManager;

    public TeamCommand(Main plugin, TeamManager teamManager) {
        this.plugin = plugin;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "server.team-59b5161");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "add" -> handleAdd(player, args);
            case "accept" -> handleAccept(player);
            case "deny" -> handleDeny(player);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            case "info" -> handleInfo(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "server.team-8c598e7");
            return;
        }

        teamManager.createTeam(player, args[1], result -> {
            switch (result) {
                case SUCCESS -> Messages.send(player, "server.team-created", java.util.Map.of("team", args[1]), true);
                case ALREADY_IN_TEAM -> Messages.send(player, "server.team-d5bf232");
                case NAME_TAKEN -> Messages.send(player, "server.team-5205a32");
                case INVALID_NAME -> Messages.send(player, "server.team-d5322a2");
                case ERROR -> Messages.send(player, "server.team-5ff57db");
            }
        });
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "server.team-dd5be7d");
            return;
        }

        String targetName = args[1];
        teamManager.inviteMember(player, targetName, result -> {
            switch (result) {
                case SUCCESS -> {
                    Messages.send(player, "server.team-invite-sent", java.util.Map.of("player", targetName), true);
                    Player online = Bukkit.getPlayerExact(targetName);
                    if (online != null) {
                        Messages.send(online, "server.team-invite-received", java.util.Map.of("player", player.getName()), true);
                        Messages.send(online, "server.team-1e81a35");
                    }
                }
                case NOT_OWNER -> Messages.send(player, "server.team-4df8de9");
                case NO_TEAM -> Messages.send(player, "server.team-360c6a0");
                case TARGET_ALREADY_IN_TEAM -> Messages.send(player, "server.team-target-already", java.util.Map.of("player", targetName), true);
                case TARGET_NOT_FOUND -> Messages.send(player, "server.team-48408b8");
                case TARGET_NOT_ONLINE -> Messages.send(player, "server.team-target-offline", java.util.Map.of("player", targetName), true);
                case ALREADY_INVITED -> Messages.send(player, "server.team-already-invited", java.util.Map.of("player", targetName), true);
                case ERROR -> Messages.send(player, "server.team-dde9be9");
            }
        });
    }

    private void handleAccept(Player player) {
        teamManager.acceptInvite(player, result -> {
            switch (result) {
                case SUCCESS -> Messages.send(player, "server.team-3ec760c");
                case NO_PENDING_INVITE -> Messages.send(player, "server.team-9e1ac4c");
                case INVITER_OFFLINE -> Messages.send(player, "server.team-fded93a");
                case TARGET_ALREADY_IN_TEAM -> Messages.send(player, "server.team-80f0750");
                case TEAM_GONE -> Messages.send(player, "server.team-062db57");
                case ERROR -> Messages.send(player, "server.team-dde9be9x");
            }
        });
    }

    private void handleDeny(Player player) {
        java.util.UUID ownerUuid = teamManager.denyInvite(player);
        if (ownerUuid == null) {
            Messages.send(player, "server.team-9e1ac4cx");
            return;
        }
        Messages.send(player, "server.team-6256072");
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null) {
            Messages.send(owner, "server.team-invite-refused", java.util.Map.of("player", player.getName()), true);
        }
    }

    private void handleLeave(Player player) {
        teamManager.leaveTeam(player, success -> {
            if (Boolean.TRUE.equals(success)) {
                Messages.send(player, "server.team-03ca43a");
            } else {
                Messages.send(player, "server.team-6a3bce5");
            }
        });
    }

    private void handleDisband(Player player) {
        teamManager.getTeamAsync(player.getUniqueId(), team -> {
            if (team == null) {
                Messages.send(player, "server.team-6a3bce5x");
                return;
            }
            if (!team.isOwner(player.getUniqueId())) {
                Messages.send(player, "server.team-01e7540");
                return;
            }
            teamManager.leaveTeam(player, success -> Messages.send(player, "server.team-disbanded", java.util.Map.of(), true));
        });
    }

    private void handleInfo(Player player, String[] args) {
        if (args.length >= 2) {
            String name = args[1];
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                TeamData team = plugin.getDatabaseManager().getTeamByName(name);
                Bukkit.getScheduler().runTask(plugin, () -> displayTeamInfo(player, team, name));
            });
            return;
        }

        teamManager.getTeamAsync(player.getUniqueId(), team -> displayTeamInfo(player, team, null));
    }

    private void displayTeamInfo(Player player, TeamData team, String queriedName) {
        if (team == null) {
            if (queriedName != null) {
                Messages.send(player, "server.team-not-found-by-name", java.util.Map.of("team", queriedName), true);
            } else {
                Messages.send(player, "server.team-6a3bce5xx");
            }
            return;
        }

        Player ownerOnline = Bukkit.getPlayer(team.getOwnerUuid());
        String ownerName = ownerOnline != null ? ownerOnline.getName() : team.getMembers().getOrDefault(team.getOwnerUuid(), "?");

        Messages.send(player, "server.team-info-header", java.util.Map.of("team", team.getName()), true);
        Messages.send(player, "server.team-info-owner", java.util.Map.of("owner", ownerName), true);
        String members = team.getMembers().values().stream().collect(Collectors.joining("&7, &e"));
        Messages.send(player, "server.team-info-members", java.util.Map.of("count", team.size(), "members", members), true);
    }

    private void sendUsage(Player player) {
        Messages.send(player, "server.team-d8415be");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : List.of("create", "add", "accept", "deny", "leave", "disband", "info")) {
                if (sub.startsWith(partial)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            String partial = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(partial)) {
                    completions.add(online.getName());
                }
            }
        }

        return completions;
    }

    private void sendMessage(CommandSender sender, String message) {
        String prefix = plugin.getMessages().getString("prefix", "");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
