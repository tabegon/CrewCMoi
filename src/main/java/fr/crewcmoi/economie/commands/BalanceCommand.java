package fr.crewcmoi.economie.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.economie.managers.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class BalanceCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final EconomyManager economyManager;

    public BalanceCommand(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String currency = plugin.getConfig().getString("economy.currency-symbol");

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                Messages.send(sender, "economy.balance.usage");
                return true;
            }

            Player player = (Player) sender;
            double balance = economyManager.getBalance(player.getUniqueId());
            Messages.send(sender, "economy.balance.self", java.util.Map.of("amount", balance + currency));
            return true;
        }

        String targetName = args[0];
        PlayerData data = economyManager.getPlayerDataByName(targetName);

        if (data == null) {
            Messages.send(sender, "general.player-not-found");
            return true;
        }

        Messages.send(sender, "economy.balance.other", java.util.Map.of("player", data.getName(), "amount", data.getBalance() + currency));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        String partial = args[0].toLowerCase();
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getName().toLowerCase().startsWith(partial)) {
                completions.add(online.getName());
            }
        }
        return completions;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
