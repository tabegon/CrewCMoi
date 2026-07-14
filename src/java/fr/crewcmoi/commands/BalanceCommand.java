package fr.crewcmoi.commands;

import fr.economy.Main;
import fr.economy.data.PlayerData;
import fr.economy.managers.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande /balance (alias /bal) : affiche son propre solde ou celui d'un autre joueur.
 */
public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    public BalanceCommand(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String currency = plugin.getConfig().getString("economy.currency-symbol", "$");

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "&cCette commande doit être exécutée par un joueur, ou précisez un pseudo : /balance <joueur>");
                return true;
            }

            Player player = (Player) sender;
            double balance = economyManager.getBalance(player.getUniqueId());
            sendMessage(sender, "&7Votre solde : &a" + format.format(balance) + currency);
            return true;
        }

        // /balance <joueur>
        if (!sender.hasPermission("economy.balance.others")) {
            sendMessage(sender, "&cVous n'avez pas la permission de voir le solde d'un autre joueur.");
            return true;
        }

        String targetName = args[0];
        PlayerData data = economyManager.getPlayerDataByName(targetName);

        if (data == null) {
            sendMessage(sender, "&cCe joueur n'existe pas ou n'a jamais rejoint le serveur.");
            return true;
        }

        sendMessage(sender, "&7Solde de &e" + data.getName() + "&7 : &a" + format.format(data.getBalance()) + currency);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("economy.balance.others")) {
            String partial = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(partial)) {
                    completions.add(online.getName());
                }
            }
        }
        return completions;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
