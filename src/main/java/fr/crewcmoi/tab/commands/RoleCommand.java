package fr.crewcmoi.tab.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.managers.TabListManager;
import fr.crewcmoi.tab.roles.Role;
import fr.crewcmoi.tab.roles.RoleManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Commande /rank :
 *  - /rank                          : affiche votre propre rôle.
 *  - /rank <joueur>                 : affiche le rôle d'un autre joueur.
 *  - /rank list                     : liste tous les rôles disponibles (dans l'ordre du tab).
 *  - /rank set <joueur> <rôle>      : (admin) attribue manuellement un rôle à un joueur.
 *  - /rank clear <joueur>           : (admin) retire l'attribution manuelle (retombe sur les permissions).
 */
public class RoleCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final RoleManager roleManager;
    private final TabListManager tabListManager;

    public RoleCommand(Main plugin, RoleManager roleManager, TabListManager tabListManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.tabListManager = tabListManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "&cPrécisez un joueur : /rank <joueur>");
                return true;
            }
            Role role = roleManager.getRole((Player) sender);
            sendMessage(sender, "&7Votre rôle actuel : " + roleManager.getPrefix(role) + "&r&7.");
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sendMessage(sender, "&7Rôles disponibles :");
            for (Role role : Role.values()) {
                sendMessage(sender, " " + roleManager.getPrefix(role) + "&7(" + role.getId() + ")");
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("set")) {
            if (!sender.hasPermission("crew.rank.set")) {
                sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
                return true;
            }
            if (args.length < 3) {
                sendMessage(sender, "&cUsage : /rank set <joueur> <" + rolesIdsJoined() + ">");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            if (!target.hasPlayedBefore() && !target.isOnline()) {
                sendMessage(sender, "&cCe joueur n'existe pas ou n'a jamais rejoint le serveur.");
                return true;
            }
            Role role = Role.fromId(args[2]);
            if (role == null) {
                sendMessage(sender, "&cRôle inconnu. Rôles valides : " + rolesIdsJoined());
                return true;
            }

            roleManager.setRole(target.getUniqueId(), role);
            sendMessage(sender, "&7" + target.getName() + " a désormais le rôle " + roleManager.getPrefix(role) + "&r&7.");

            if (target.isOnline()) {
                tabListManager.applyRole((Player) target);
            }
            return true;
        }

        if (args[0].equalsIgnoreCase("clear")) {
            if (!sender.hasPermission("crew.rank.set")) {
                sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
                return true;
            }
            if (args.length < 2) {
                sendMessage(sender, "&cUsage : /rank clear <joueur>");
                return true;
            }
            OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
            roleManager.clearRole(target.getUniqueId());
            sendMessage(sender, "&7Attribution manuelle retirée pour " + target.getName() + " (retombe sur ses permissions).");

            if (target.isOnline()) {
                tabListManager.applyRole((Player) target);
            }
            return true;
        }

        // /rank <joueur> : affiche le rôle d'un autre joueur
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (!target.isOnline()) {
            sendMessage(sender, "&cCe joueur n'est pas en ligne.");
            return true;
        }
        Role role = roleManager.getRole((Player) target);
        sendMessage(sender, "&7Rôle de &e" + target.getName() + "&7 : " + roleManager.getPrefix(role) + "&r&7.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("list");
            if (sender.hasPermission("crew.rank.set")) {
                completions.add("set");
                completions.add("clear");
            }
            for (Player online : Bukkit.getOnlinePlayers()) {
                completions.add(online.getName());
            }
            return filter(completions, args[0]);
        }

        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("clear"))) {
            for (Player online : Bukkit.getOnlinePlayers()) {
                completions.add(online.getName());
            }
            return filter(completions, args[1]);
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            for (Role role : Role.values()) {
                completions.add(role.getId());
            }
            return filter(completions, args[2]);
        }

        return completions;
    }

    private List<String> filter(List<String> options, String partial) {
        String lower = partial.toLowerCase();
        return options.stream().filter(o -> o.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }

    private String rolesIdsJoined() {
        StringBuilder sb = new StringBuilder();
        for (Role role : Role.values()) {
            if (sb.length() > 0) {
                sb.append('|');
            }
            sb.append(role.getId());
        }
        return sb.toString();
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
