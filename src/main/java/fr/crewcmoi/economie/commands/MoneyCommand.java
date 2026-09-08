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
import java.util.Arrays;
import java.util.List;

public class MoneyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final EconomyManager economyManager;

    public MoneyCommand(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crew.admin")) {
            Messages.send(sender, "economy.money.no-permission");
            return true;
        }

        if (args.length < 3) {
            Messages.send(sender, "economy.money.usage");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        double amount;

        try {
            amount = MoneyFormat.parse(args[2]);
        } catch (NumberFormatException e) {
            Messages.send(sender, "economy.money.invalid-amount", java.util.Map.of("amount", args[2]));
            return true;
        }

        if (amount < 0) {
            Messages.send(sender, "economy.money.positive-amount");
            return true;
        }

        PlayerData data = economyManager.getPlayerDataByName(targetName);
        if (data == null) {
            Messages.send(sender, "general.player-not-found");
            return true;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol");

        switch (action) {
            case "add":
                economyManager.deposit(data.getUuid(), amount);
                Messages.send(sender, "economy.money.add-success", java.util.Map.of("amount", MoneyFormat.format(amount) + currency, "player", data.getName()));
                notifyTarget(data, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ʀᴇᴄᴜ &e" + MoneyFormat.format(amount) + currency + "&a !");
                break;

            case "remove":
                boolean success = economyManager.withdraw(data.getUuid(), amount);
                if (!success) {
                    Messages.send(sender, "economy.money.remove-not-enough", java.util.Map.of("player", data.getName()));
                    return true;
                }
                Messages.send(sender, "economy.money.remove-success", java.util.Map.of("amount", MoneyFormat.format(amount) + currency, "player", data.getName()));
                notifyTarget(data, "&cᴏɴ ᴠᴏᴜꜱ ᴀ ʀᴇᴛɪʀᴇ &e" + MoneyFormat.format(amount) + currency + "&c.");
                break;

            case "set":
                economyManager.setBalance(data.getUuid(), amount);
                Messages.send(sender, "economy.money.set-success", java.util.Map.of("amount", MoneyFormat.format(amount) + currency, "player", data.getName()));
                notifyTarget(data, "&eᴠᴏᴛʀᴇ ꜱᴏʟᴅᴇ ᴀ ᴇᴛᴇ ᴅᴇꜰɪɴɪ ᴀ " + MoneyFormat.format(amount) + currency + ".");
                break;

            default:
                Messages.send(sender, "economy.money.unknown-action");
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
