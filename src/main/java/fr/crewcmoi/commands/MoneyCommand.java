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

import fr.crewcmoi.utils.MoneyFormat;
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

    public MoneyCommand(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crew.admin")) {
            sendMessage(sender, "&cᴠᴏᴜꜱ ɴ'ᴀᴠᴇᴢ ᴘᴀꜱ ʟᴀ ᴘᴇʀᴍɪꜱꜱɪᴏɴ ᴅ'ᴜᴛɪʟɪꜱᴇʀ ᴄᴇᴛᴛᴇ ᴄᴏᴍᴍᴀɴᴅᴇ.");
            return true;
        }

        if (args.length < 3) {
            sendMessage(sender, "&cᴜꜱᴀɢᴇ : /ᴍᴏɴᴇʏ <ᴀᴅᴅ|ʀᴇᴍᴏᴠᴇ|ꜱᴇᴛ> <ᴊᴏᴜᴇᴜʀ> <ᴍᴏɴᴛᴀɴᴛ>");
            return true;
        }

        String action = args[0].toLowerCase();
        String targetName = args[1];
        double amount;

        try {
            amount = MoneyFormat.parse(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "&cᴍᴏɴᴛᴀɴᴛ ɪɴᴠᴀʟɪᴅᴇ : " + args[2]);
            return true;
        }

        if (amount < 0) {
            sendMessage(sender, "&cʟᴇ ᴍᴏɴᴛᴀɴᴛ ᴅᴏɪᴛ ᴇᴛʀᴇ ᴘᴏꜱɪᴛɪꜰ.");
            return true;
        }

        PlayerData data = economyManager.getPlayerDataByName(targetName);
        if (data == null) {
            sendMessage(sender, "&cᴄᴇ ᴊᴏᴜᴇᴜʀ ɴ'ᴇxɪꜱᴛᴇ ᴘᴀꜱ ᴏᴜ ɴ'ᴀ ᴊᴀᴍᴀɪꜱ ʀᴇᴊᴏɪɴᴛ ʟᴇ ꜱᴇʀᴠᴇᴜʀ.");
            return true;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        switch (action) {
            case "add":
                economyManager.deposit(data.getUuid(), amount);
                sendMessage(sender, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ᴀᴊᴏᴜᴛᴇ &e" + MoneyFormat.format(amount) + currency + "&a ᴀ &e" + data.getName() + "&a.");
                notifyTarget(data, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ʀᴇᴄᴜ &e" + MoneyFormat.format(amount) + currency + "&a !");
                break;

            case "remove":
                boolean success = economyManager.withdraw(data.getUuid(), amount);
                if (!success) {
                    sendMessage(sender, "&c" + data.getName() + " n'a pas assez d'argent pour retirer ce montant.");
                    return true;
                }
                sendMessage(sender, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ʀᴇᴛɪʀᴇ &e" + MoneyFormat.format(amount) + currency + "&a ᴀ &e" + data.getName() + "&a.");
                notifyTarget(data, "&cᴏɴ ᴠᴏᴜꜱ ᴀ ʀᴇᴛɪʀᴇ &e" + MoneyFormat.format(amount) + currency + "&c.");
                break;

            case "set":
                economyManager.setBalance(data.getUuid(), amount);
                sendMessage(sender, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ᴅᴇꜰɪɴɪ ʟᴇ ꜱᴏʟᴅᴇ ᴅᴇ &e" + data.getName() + "&a ᴀ &e" + MoneyFormat.format(amount) + currency + "&a.");
                notifyTarget(data, "&eᴠᴏᴛʀᴇ ꜱᴏʟᴅᴇ ᴀ ᴇᴛᴇ ᴅᴇꜰɪɴɪ ᴀ " + MoneyFormat.format(amount) + currency + ".");
                break;

            default:
                sendMessage(sender, "&cᴀᴄᴛɪᴏɴ ɪɴᴄᴏɴɴᴜᴇ. ᴜᴛɪʟɪꜱᴇᴢ ᴀᴅᴅ, ʀᴇᴍᴏᴠᴇ ᴏᴜ ꜱᴇᴛ.");
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
