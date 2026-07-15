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
            sendMessage(sender, "&cCette commande doit être exécutée par un joueur.");
            return true;
        }

        if (args.length == 0) {
            sendUsage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player, args);
            case "add" -> handleAdd(player, args);
            case "leave" -> handleLeave(player);
            case "disband" -> handleDisband(player);
            case "info" -> handleInfo(player, args);
            default -> sendUsage(player);
        }

        return true;
    }

    private void handleCreate(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "&cUsage : /team create <nom>");
            return;
        }

        teamManager.createTeam(player, args[1], result -> {
            switch (result) {
                case SUCCESS -> sendMessage(player, "&aVotre équipe &e" + args[1] + "&a a été créée !");
                case ALREADY_IN_TEAM -> sendMessage(player, "&cVous faites déjà partie d'une équipe. Quittez-la avec &f/team leave&c.");
                case NAME_TAKEN -> sendMessage(player, "&cCe nom d'équipe est déjà pris.");
                case INVALID_NAME -> sendMessage(player, "&cNom invalide (3 à 16 caractères, lettres/chiffres/_ uniquement).");
                case ERROR -> sendMessage(player, "&cUne erreur est survenue lors de la création de l'équipe.");
            }
        });
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 2) {
            sendMessage(player, "&cUsage : /team add <joueur>");
            return;
        }

        String targetName = args[1];
        teamManager.addMember(player, targetName, result -> {
            switch (result) {
                case SUCCESS -> {
                    sendMessage(player, "&a" + targetName + " a rejoint votre équipe !");
                    Player online = Bukkit.getPlayerExact(targetName);
                    if (online != null) {
                        sendMessage(online, "&aVous avez rejoint l'équipe de &e" + player.getName() + "&a !");
                    }
                }
                case NOT_OWNER -> sendMessage(player, "&cSeul le propriétaire de l'équipe peut ajouter des membres.");
                case NO_TEAM -> sendMessage(player, "&cVous ne faites partie d'aucune équipe. Créez-en une avec &f/team create <nom>&c.");
                case TARGET_ALREADY_IN_TEAM -> sendMessage(player, "&c" + targetName + " fait déjà partie d'une équipe.");
                case TARGET_NOT_FOUND -> sendMessage(player, "&cCe joueur n'existe pas ou n'a jamais rejoint le serveur.");
                case ERROR -> sendMessage(player, "&cUne erreur est survenue.");
            }
        });
    }

    private void handleLeave(Player player) {
        teamManager.leaveTeam(player, success -> {
            if (Boolean.TRUE.equals(success)) {
                sendMessage(player, "&aVous avez quitté votre équipe.");
            } else {
                sendMessage(player, "&cVous ne faites partie d'aucune équipe.");
            }
        });
    }

    private void handleDisband(Player player) {
        teamManager.getTeamAsync(player.getUniqueId(), team -> {
            if (team == null) {
                sendMessage(player, "&cVous ne faites partie d'aucune équipe.");
                return;
            }
            if (!team.isOwner(player.getUniqueId())) {
                sendMessage(player, "&cSeul le propriétaire de l'équipe peut la dissoudre. Utilisez &f/team leave&c pour la quitter.");
                return;
            }
            teamManager.leaveTeam(player, success -> sendMessage(player, "&aVotre équipe a été dissoute."));
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
                sendMessage(player, "&cAucune équipe nommée &e" + queriedName + "&c n'existe.");
            } else {
                sendMessage(player, "&cVous ne faites partie d'aucune équipe.");
            }
            return;
        }

        Player ownerOnline = Bukkit.getPlayer(team.getOwnerUuid());
        String ownerName = ownerOnline != null ? ownerOnline.getName() : team.getMembers().getOrDefault(team.getOwnerUuid(), "?");

        sendMessage(player, "&6&l--- Équipe " + team.getName() + " ---");
        sendMessage(player, "&7Propriétaire : &e" + ownerName);
        String members = team.getMembers().values().stream().collect(Collectors.joining("&7, &e"));
        sendMessage(player, "&7Membres (&e" + team.size() + "&7) : &e" + members);
    }

    private void sendUsage(Player player) {
        sendMessage(player, "&cUsage : /team <create|add|leave|disband|info>");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String sub : List.of("create", "add", "leave", "disband", "info")) {
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
