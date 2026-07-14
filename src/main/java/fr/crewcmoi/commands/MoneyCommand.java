package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.PlayerData;
import fr.crewcmoi.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Commande admin /money <add|remove|set> <joueur> <montant>
 * Permission : economy.admin
 */
public class MoneyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    public MoneyCommand(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crew.admin")) {
            sendMessage(sender, "&cVous n'avez pas la permission d'utiliser cette commande.");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "&cUsage : /money <add|remove|set> <joueur> <montant>");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        double amount;

        try {
            amount = Double.parseDouble(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "&cMontant invalide : " + args[2]);
            return true;
        }

        if (amount < 0) {
            sendMessage(sender, "&cLe montant doit être positif.");
            return true;
        }

        PlayerData data = economyManager.getPlayerDataByName(targetName);
        if (data == null) {
            sendMessage(sender, "&cCe joueur n'existe pas ou n'a jamais rejoint le serveur.");
            return true;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");

        switch (action) {
            case "add":
                economyManager.deposit(data.getUuid(), amount);
                sendMessage(sender, "&aVous avez ajouté &e" + format.format(amount) + currency + "&a à &e" + data.getName() + "&a.");
                notifyTarget(data, "&aVous avez reçu &e" + format.format(amount) + currency + "&a !");
                break;

            case "remove":
                boolean success = economyManager.withdraw(data.getUuid(), amount);
                if (!success) {
                    sendMessage(sender, "&c" + data.getName() + " n'a pas assez d'argent pour retirer ce montant.");
                    return true;
                }
                sendMessage(sender, "&aVous avez retiré &e" + format.format(amount) + currency + "&a à &e" + data.getName() + "&a.");
                notifyTarget(data, "&cOn vous a retiré &e" + format.format(amount) + currency + "&c.");
                break;

            case "set":
                economyManager.setBalance(data.getUuid(), amount);
                sendMessage(sender, "&aVous avez défini le solde de &e" + data.getName() + "&a à &e" + format.format(amount) + currency + "&a.");
                notifyTarget(data, "&eVotre solde a été défini à " + format.format(amount) + currency + ".");
                break;

            default:
                sendMessage(sender, "&cAction inconnue. Utilisez add, remove ou set.");
                break;
        }

        return true;
    }

    private void notifyTarget(PlayerData data, String message) {
        Player online = Bukkit.getPlayer(data.getUuid());
        if (online != null && online.isOnline()) {
            sendMessage(online, message);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("crew.admin")) {
            return new ArrayList<>();
        }

        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (String action : Arrays.asList("add", "remove", "set")) {
                if (action.startsWith(partial)) {
                    completions.add(action);
                }
            }
        } else if (args.length == 2) {
            String partial = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(partial)) {
                    completions.add(online.getName());
                }
            }
        } else if (args.length == 3) {
            completions.add("100");
        }

        return completions;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
