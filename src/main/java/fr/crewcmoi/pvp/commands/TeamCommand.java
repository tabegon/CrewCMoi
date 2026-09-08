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
            Messages.send(sender, "teams.command.player-only");
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
            Messages.send(player, "teams.create.usage");
            return;
        }

        teamManager.createTeam(player, args[1], result -> {
            switch (result) {
                case SUCCESS -> Messages.send(player, "teams.created", java.util.Map.of("team", args[1]), true);
                case ALREADY_IN_TEAM -> Messages.send(player, "teams.create.already-in-team");
                case NAME_TAKEN -> Messages.send(player, "teams.create.name-taken");
                case INVALID_NAME -> Messages.send(player, "teams.create.invalid-name");
                case ERROR -> Messages.send(player, "teams.create.failed");
            }
        });
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            Messages.send(player, "teams.add.usage");
            return;
        }

        String targetName = args[1];
        teamManager.inviteMember(player, targetName, result -> {
            switch (result) {
                case SUCCESS -> {
                    Messages.send(player, "teams.invite-sent", java.util.Map.of("player", targetName), true);
                    Player online = Bukkit.getPlayerExact(targetName);
                    if (online != null) {
                        Messages.send(online, "teams.invite-received", java.util.Map.of("player", player.getName()), true);
                        Messages.send(online, "teams.invite.instructions");
                    }
                }
                case NOT_OWNER -> Messages.send(player, "teams.add.owner-only");
                case NO_TEAM -> Messages.send(player, "teams.add.not-in-team");
                case TARGET_ALREADY_IN_TEAM -> Messages.send(player, "teams.target-already", java.util.Map.of("player", targetName), true);
                case TARGET_NOT_FOUND -> Messages.send(player, "general.player-not-found");
                case TARGET_NOT_ONLINE -> Messages.send(player, "teams.target-offline", java.util.Map.of("player", targetName), true);
                case ALREADY_INVITED -> Messages.send(player, "teams.already-invited", java.util.Map.of("player", targetName), true);
                case ERROR -> Messages.send(player, "teams.generic.error");
            }
        });
    }

    private void handleAccept(Player player) {
        teamManager.acceptInvite(player, result -> {
            switch (result) {
                case SUCCESS -> Messages.send(player, "teams.accept.success");
                case NO_PENDING_INVITE -> Messages.send(player, "teams.accept.no-invite");
                case INVITER_OFFLINE -> Messages.send(player, "teams.add.target-offline");
                case TARGET_ALREADY_IN_TEAM -> Messages.send(player, "teams.generic.already-in-team");
                case TEAM_GONE -> Messages.send(player, "teams.generic.not-found");
                case ERROR -> Messages.send(player, "teams.generic.errorx");
            }
        });
    }

    private void handleDeny(Player player) {
        java.util.UUID ownerUuid = teamManager.denyInvite(player);
        if (ownerUuid == null) {
            Messages.send(player, "teams.accept.no-invitex");
            return;
        }
        Messages.send(player, "teams.deny.success");
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null) {
            Messages.send(owner, "teams.invite-refused", java.util.Map.of("player", player.getName()), true);
        }
    }

    private void handleLeave(Player player) {
        teamManager.leaveTeam(player, success -> {
            if (Boolean.TRUE.equals(success)) {
                Messages.send(player, "teams.leave.success");
            } else {
                Messages.send(player, "teams.generic.not-in-team");
            }
        });
    }

    private void handleDisband(Player player) {
        teamManager.getTeamAsync(player.getUniqueId(), team -> {
            if (team == null) {
                Messages.send(player, "teams.generic.not-in-teamx");
                return;
            }
            if (!team.isOwner(player.getUniqueId())) {
                Messages.send(player, "teams.disband.owner-only");
                return;
            }
            teamManager.leaveTeam(player, success -> Messages.send(player, "teams.disbanded", java.util.Map.of(), true));
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
                Messages.send(player, "teams.not-found-by-name", java.util.Map.of("team", queriedName), true);
            } else {
                Messages.send(player, "teams.generic.not-in-teamxx");
            }
            return;
        }

        Player ownerOnline = Bukkit.getPlayer(team.getOwnerUuid());
        String ownerName = ownerOnline != null ? ownerOnline.getName() : team.getMembers().getOrDefault(team.getOwnerUuid(), "?");

        Messages.send(player, "teams.info-header", java.util.Map.of("team", team.getName()), true);
        Messages.send(player, "teams.info-owner", java.util.Map.of("owner", ownerName), true);
        String members = team.getMembers().values().stream().collect(Collectors.joining("&7, &e"));
        Messages.send(player, "teams.info-members", java.util.Map.of("count", team.size(), "members", members), true);
    }

    private void sendUsage(Player player) {
        Messages.send(player, "teams.command.usage");
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
        String prefix = plugin.getMessages().getString("prefix");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
