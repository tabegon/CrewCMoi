package fr.crewcmoi.economie.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.economie.managers.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.crewcmoi.other.utils.MoneyFormat;
import java.util.ArrayList;
import java.util.List;

public class PayCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final EconomyManager economyManager;

    public PayCommand(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "economy.pay.player-only");
            return true;
        }

        if (args.length < 2) {
            Messages.send(sender, "economy.pay.usage");
            return true;
        }

        String targetName = args[0];
        double amount;

        try {
            amount = MoneyFormat.parse(args[1]);
        } catch (NumberFormatException e) {
            Messages.send(sender, "economy.pay.invalid-amount", java.util.Map.of("amount", args[1]));
            return true;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            Messages.send(sender, "economy.pay.positive-amount");
            return true;
        }

        PlayerData targetData = economyManager.getPlayerDataByName(targetName);
        if (targetData == null) {
            Messages.send(sender, "general.player-not-found");
            return true;
        }

        if (targetData.getUuid().equals(player.getUniqueId())) {
            Messages.send(sender, "economy.pay.self");
            return true;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol");

        if (!economyManager.has(player.getUniqueId(), amount)) {
            Messages.send(sender, "economy.pay.not-enough-money", java.util.Map.of("amount", MoneyFormat.format(amount) + currency));
            return true;
        }

        boolean withdrawn = economyManager.withdraw(player.getUniqueId(), amount);
        if (!withdrawn) {
            Messages.send(sender, "economy.pay.not-enough-money", java.util.Map.of("amount", MoneyFormat.format(amount) + currency));
            return true;
        }

        economyManager.deposit(targetData.getUuid(), amount);

        Messages.send(sender, "economy.pay.sent", java.util.Map.of("amount", MoneyFormat.format(amount) + currency, "player", targetData.getName()));

        Player online = Bukkit.getPlayer(targetData.getUuid());
        if (online != null && online.isOnline()) {
            Messages.send(online, "economy.pay.received", java.util.Map.of("amount", MoneyFormat.format(amount) + currency, "player", player.getName()));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(partial)
                        && !(sender instanceof Player p && p.getUniqueId().equals(online.getUniqueId()))) {
                    completions.add(online.getName());
                }
            }
        } else if (args.length == 2) {
            completions.add("100");
        }

        return completions;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}
