package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.TeamData;
import fr.crewcmoi.managers.TeamManager;
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
            sendMessage(sender, "&cᴄᴇᴛᴛᴇ ᴄᴏᴍᴍᴀɴᴅᴇ ᴅᴏɪᴛ ᴇᴛʀᴇ ᴇxᴇᴄᴜᴛᴇᴇ ᴘᴀʀ ᴜɴ ᴊᴏᴜᴇᴜʀ.");
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
            sendMessage(player, "&cᴜꜱᴀɢᴇ : /ᴛᴇᴀᴍ ᴄʀᴇᴀᴛᴇ <ɴᴏᴍ>");
            return;
        }

        teamManager.createTeam(player, args[1], result -> {
            switch (result) {
                case SUCCESS -> sendMessage(player, "&aᴠᴏᴛʀᴇ ᴇǫᴜɪᴘᴇ &e" + args[1] + "&a ᴀ ᴇᴛᴇ ᴄʀᴇᴇᴇ !");
                case ALREADY_IN_TEAM -> sendMessage(player, "&cᴠᴏᴜꜱ ꜰᴀɪᴛᴇꜱ ᴅᴇᴊᴀ ᴘᴀʀᴛɪᴇ ᴅ'ᴜɴᴇ ᴇǫᴜɪᴘᴇ. ǫᴜɪᴛᴛᴇᴢ-ʟᴀ ᴀᴠᴇᴄ &f/ᴛᴇᴀᴍ ʟᴇᴀᴠᴇ&c.");
                case NAME_TAKEN -> sendMessage(player, "&cᴄᴇ ɴᴏᴍ ᴅ'ᴇǫᴜɪᴘᴇ ᴇꜱᴛ ᴅᴇᴊᴀ ᴘʀɪꜱ.");
                case INVALID_NAME -> sendMessage(player, "&cɴᴏᴍ ɪɴᴠᴀʟɪᴅᴇ (3 ᴀ 16 ᴄᴀʀᴀᴄᴛᴇʀᴇꜱ, ʟᴇᴛᴛʀᴇꜱ/ᴄʜɪꜰꜰʀᴇꜱ/_ ᴜɴɪǫᴜᴇᴍᴇɴᴛ).");
                case ERROR -> sendMessage(player, "&cᴜɴᴇ ᴇʀʀᴇᴜʀ ᴇꜱᴛ ꜱᴜʀᴠᴇɴᴜᴇ ʟᴏʀꜱ ᴅᴇ ʟᴀ ᴄʀᴇᴀᴛɪᴏɴ ᴅᴇ ʟ'ᴇǫᴜɪᴘᴇ.");
            }
        });
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "&cᴜꜱᴀɢᴇ : /ᴛᴇᴀᴍ ᴀᴅᴅ <ᴊᴏᴜᴇᴜʀ>");
            return;
        }

        String targetName = args[1];
        teamManager.inviteMember(player, targetName, result -> {
            switch (result) {
                case SUCCESS -> {
                    sendMessage(player, "&aɪɴᴠɪᴛᴀᴛɪᴏɴ ᴇɴᴠᴏʏᴇᴇ ᴀ &e" + targetName + "&a. ᴇɴ ᴀᴛᴛᴇɴᴛᴇ ᴅᴇ ꜱᴀ ʀᴇᴘᴏɴꜱᴇ...");
                    Player online = Bukkit.getPlayerExact(targetName);
                    if (online != null) {
                        sendMessage(online, "&e" + player.getName() + "&a ᴠᴏᴜꜱ ᴀ ɪɴᴠɪᴛᴇ ᴅᴀɴꜱ ꜱᴏɴ ᴇǫᴜɪᴘᴇ !");
                        sendMessage(online, "&aᴛᴀᴘᴇᴢ &f/ᴛᴇᴀᴍ ᴀᴄᴄᴇᴘᴛ&a ᴘᴏᴜʀ ᴀᴄᴄᴇᴘᴛᴇʀ ᴏᴜ &f/ᴛᴇᴀᴍ ᴅᴇɴʏ&a ᴘᴏᴜʀ ʀᴇꜰᴜꜱᴇʀ (60ꜱ).");
                    }
                }
                case NOT_OWNER -> sendMessage(player, "&cꜱᴇᴜʟ ʟᴇ ᴘʀᴏᴘʀɪᴇᴛᴀɪʀᴇ ᴅᴇ ʟ'ᴇǫᴜɪᴘᴇ ᴘᴇᴜᴛ ɪɴᴠɪᴛᴇʀ ᴅᴇꜱ ᴍᴇᴍʙʀᴇꜱ.");
                case NO_TEAM -> sendMessage(player, "&cᴠᴏᴜꜱ ɴᴇ ꜰᴀɪᴛᴇꜱ ᴘᴀʀᴛɪᴇ ᴅ'ᴀᴜᴄᴜɴᴇ ᴇǫᴜɪᴘᴇ. ᴄʀᴇᴇᴢ-ᴇɴ ᴜɴᴇ ᴀᴠᴇᴄ &f/ᴛᴇᴀᴍ ᴄʀᴇᴀᴛᴇ <ɴᴏᴍ>&c.");
                case TARGET_ALREADY_IN_TEAM -> sendMessage(player, "&c" + targetName + " ꜰᴀɪᴛ ᴅᴇᴊᴀ ᴘᴀʀᴛɪᴇ ᴅ'ᴜɴᴇ ᴇǫᴜɪᴘᴇ.");
                case TARGET_NOT_FOUND -> sendMessage(player, "&cᴄᴇ ᴊᴏᴜᴇᴜʀ ɴ'ᴇxɪꜱᴛᴇ ᴘᴀꜱ ᴏᴜ ɴ'ᴀ ᴊᴀᴍᴀɪꜱ ʀᴇᴊᴏɪɴᴛ ʟᴇ ꜱᴇʀᴠᴇᴜʀ.");
                case TARGET_NOT_ONLINE -> sendMessage(player, "&c" + targetName + " ᴅᴏɪᴛ ᴇᴛʀᴇ ᴇɴ ʟɪɢɴᴇ ᴘᴏᴜʀ ʀᴇᴄᴇᴠᴏɪʀ ᴜɴᴇ ɪɴᴠɪᴛᴀᴛɪᴏɴ.");
                case ALREADY_INVITED -> sendMessage(player, "&c" + targetName + " ᴀ ᴅᴇᴊᴀ ᴜɴᴇ ɪɴᴠɪᴛᴀᴛɪᴏɴ ᴇɴ ᴀᴛᴛᴇɴᴛᴇ.");
                case ERROR -> sendMessage(player, "&cᴜɴᴇ ᴇʀʀᴇᴜʀ ᴇꜱᴛ ꜱᴜʀᴠᴇɴᴜᴇ.");
            }
        });
    }

    private void handleAccept(Player player) {
        teamManager.acceptInvite(player, result -> {
            switch (result) {
                case SUCCESS -> sendMessage(player, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ʀᴇᴊᴏɪɴᴛ ʟ'ᴇǫᴜɪᴘᴇ !");
                case NO_PENDING_INVITE -> sendMessage(player, "&cᴠᴏᴜꜱ ɴ'ᴀᴠᴇᴢ ᴀᴜᴄᴜɴᴇ ɪɴᴠɪᴛᴀᴛɪᴏɴ ᴇɴ ᴀᴛᴛᴇɴᴛᴇ.");
                case INVITER_OFFLINE -> sendMessage(player, "&cʟᴇ ᴊᴏᴜᴇᴜʀ ᴠᴏᴜꜱ ᴀʏᴀɴᴛ ɪɴᴠɪᴛᴇ ɴ'ᴇꜱᴛ ᴘʟᴜꜱ ᴇɴ ʟɪɢɴᴇ.");
                case TARGET_ALREADY_IN_TEAM -> sendMessage(player, "&cᴠᴏᴜꜱ ꜰᴀɪᴛᴇꜱ ᴅᴇᴊᴀ ᴘᴀʀᴛɪᴇ ᴅ'ᴜɴᴇ ᴇǫᴜɪᴘᴇ.");
                case TEAM_GONE -> sendMessage(player, "&cᴄᴇᴛᴛᴇ ᴇǫᴜɪᴘᴇ ɴ'ᴇxɪꜱᴛᴇ ᴘʟᴜꜱ.");
                case ERROR -> sendMessage(player, "&cᴜɴᴇ ᴇʀʀᴇᴜʀ ᴇꜱᴛ ꜱᴜʀᴠᴇɴᴜᴇ.");
            }
        });
    }

    private void handleDeny(Player player) {
        java.util.UUID ownerUuid = teamManager.denyInvite(player);
        if (ownerUuid == null) {
            sendMessage(player, "&cᴠᴏᴜꜱ ɴ'ᴀᴠᴇᴢ ᴀᴜᴄᴜɴᴇ ɪɴᴠɪᴛᴀᴛɪᴏɴ ᴇɴ ᴀᴛᴛᴇɴᴛᴇ.");
            return;
        }
        sendMessage(player, "&aɪɴᴠɪᴛᴀᴛɪᴏɴ ʀᴇꜰᴜꜱᴇᴇ.");
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (owner != null) {
            sendMessage(owner, "&c" + player.getName() + " ᴀ ʀᴇꜰᴜꜱᴇ ᴠᴏᴛʀᴇ ɪɴᴠɪᴛᴀᴛɪᴏɴ.");
        }
    }

    private void handleLeave(Player player) {
        teamManager.leaveTeam(player, success -> {
            if (Boolean.TRUE.equals(success)) {
                sendMessage(player, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ǫᴜɪᴛᴛᴇ ᴠᴏᴛʀᴇ ᴇǫᴜɪᴘᴇ.");
            } else {
                sendMessage(player, "&cᴠᴏᴜꜱ ɴᴇ ꜰᴀɪᴛᴇꜱ ᴘᴀʀᴛɪᴇ ᴅ'ᴀᴜᴄᴜɴᴇ ᴇǫᴜɪᴘᴇ.");
            }
        });
    }

    private void handleDisband(Player player) {
        teamManager.getTeamAsync(player.getUniqueId(), team -> {
            if (team == null) {
                sendMessage(player, "&cᴠᴏᴜꜱ ɴᴇ ꜰᴀɪᴛᴇꜱ ᴘᴀʀᴛɪᴇ ᴅ'ᴀᴜᴄᴜɴᴇ ᴇǫᴜɪᴘᴇ.");
                return;
            }
            if (!team.isOwner(player.getUniqueId())) {
                sendMessage(player, "&cꜱᴇᴜʟ ʟᴇ ᴘʀᴏᴘʀɪᴇᴛᴀɪʀᴇ ᴅᴇ ʟ'ᴇǫᴜɪᴘᴇ ᴘᴇᴜᴛ ʟᴀ ᴅɪꜱꜱᴏᴜᴅʀᴇ. ᴜᴛɪʟɪꜱᴇᴢ &f/ᴛᴇᴀᴍ ʟᴇᴀᴠᴇ&c ᴘᴏᴜʀ ʟᴀ ǫᴜɪᴛᴛᴇʀ.");
                return;
            }
            teamManager.leaveTeam(player, success -> sendMessage(player, "&aᴠᴏᴛʀᴇ ᴇǫᴜɪᴘᴇ ᴀ ᴇᴛᴇ ᴅɪꜱꜱᴏᴜᴛᴇ."));
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
                sendMessage(player, "&cᴀᴜᴄᴜɴᴇ ᴇǫᴜɪᴘᴇ ɴᴏᴍᴍᴇᴇ &e" + queriedName + "&c ɴ'ᴇxɪꜱᴛᴇ.");
            } else {
                sendMessage(player, "&cᴠᴏᴜꜱ ɴᴇ ꜰᴀɪᴛᴇꜱ ᴘᴀʀᴛɪᴇ ᴅ'ᴀᴜᴄᴜɴᴇ ᴇǫᴜɪᴘᴇ.");
            }
            return;
        }

        Player ownerOnline = Bukkit.getPlayer(team.getOwnerUuid());
        String ownerName = ownerOnline != null ? ownerOnline.getName() : team.getMembers().getOrDefault(team.getOwnerUuid(), "?");

        sendMessage(player, "&6&l--- ᴇǫᴜɪᴘᴇ " + team.getName() + " ---");
        sendMessage(player, "&7ᴘʀᴏᴘʀɪᴇᴛᴀɪʀᴇ : &e" + ownerName);
        String members = team.getMembers().values().stream().collect(Collectors.joining("&7, &e"));
        sendMessage(player, "&7ᴍᴇᴍʙʀᴇꜱ (&e" + team.size() + "&7) : &e" + members);
    }

    private void sendUsage(Player player) {
        sendMessage(player, "&cᴜꜱᴀɢᴇ : /ᴛᴇᴀᴍ <ᴄʀᴇᴀᴛᴇ|ᴀᴅᴅ|ᴀᴄᴄᴇᴘᴛ|ᴅᴇɴʏ|ʟᴇᴀᴠᴇ|ᴅɪꜱʙᴀɴᴅ|ɪɴꜰᴏ>");
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
