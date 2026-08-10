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
import java.util.List;

/**
 * Commande /pay <joueur> <montant> : permet à un joueur de transférer
 * une partie de son solde à un autre joueur (en ligne ou hors ligne).
 */
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
            sendMessage(sender, "&cᴄᴇᴛᴛᴇ ᴄᴏᴍᴍᴀɴᴅᴇ ᴅᴏɪᴛ ᴇᴛʀᴇ ᴇxᴇᴄᴜᴛᴇᴇ ᴘᴀʀ ᴜɴ ᴊᴏᴜᴇᴜʀ.");
            return true;
        }

        if (args.length < 2) {
            sendMessage(sender, "&cᴜꜱᴀɢᴇ : /ᴘᴀʏ <ᴊᴏᴜᴇᴜʀ> <ᴍᴏɴᴛᴀɴᴛ>");
            return true;
        }

        String targetName = args[0];
        double amount;

        try {
            amount = MoneyFormat.parse(args[1]);
        } catch (NumberFormatException e) {
            sendMessage(sender, "&cᴍᴏɴᴛᴀɴᴛ ɪɴᴠᴀʟɪᴅᴇ : " + args[1]);
            return true;
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            sendMessage(sender, "&cʟᴇ ᴍᴏɴᴛᴀɴᴛ ᴅᴏɪᴛ ᴇᴛʀᴇ ᴜɴ ɴᴏᴍʙʀᴇ ᴘᴏꜱɪᴛɪꜰ.");
            return true;
        }

        PlayerData targetData = economyManager.getPlayerDataByName(targetName);
        if (targetData == null) {
            sendMessage(sender, "&cᴄᴇ ᴊᴏᴜᴇᴜʀ ɴ'ᴇxɪꜱᴛᴇ ᴘᴀꜱ ᴏᴜ ɴ'ᴀ ᴊᴀᴍᴀɪꜱ ʀᴇᴊᴏɪɴᴛ ʟᴇ ꜱᴇʀᴠᴇᴜʀ.");
            return true;
        }

        if (targetData.getUuid().equals(player.getUniqueId())) {
            sendMessage(sender, "&cᴠᴏᴜꜱ ɴᴇ ᴘᴏᴜᴠᴇᴢ ᴘᴀꜱ ᴠᴏᴜꜱ ᴘᴀʏᴇʀ ᴠᴏᴜꜱ-ᴍᴇᴍᴇ.");
            return true;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        if (!economyManager.has(player.getUniqueId(), amount)) {
            sendMessage(sender, "&cᴠᴏᴜꜱ ɴ'ᴀᴠᴇᴢ ᴘᴀꜱ ᴀꜱꜱᴇᴢ ᴅ'ᴀʀɢᴇɴᴛ ᴘᴏᴜʀ ᴇɴᴠᴏʏᴇʀ &e" + MoneyFormat.format(amount) + currency + "&c.");
            return true;
        }

        boolean withdrawn = economyManager.withdraw(player.getUniqueId(), amount);
        if (!withdrawn) {
            sendMessage(sender, "&cᴠᴏᴜꜱ ɴ'ᴀᴠᴇᴢ ᴘᴀꜱ ᴀꜱꜱᴇᴢ ᴅ'ᴀʀɢᴇɴᴛ ᴘᴏᴜʀ ᴇɴᴠᴏʏᴇʀ &e" + MoneyFormat.format(amount) + currency + "&c.");
            return true;
        }

        economyManager.deposit(targetData.getUuid(), amount);

        sendMessage(sender, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ᴇɴᴠᴏʏᴇ &e" + MoneyFormat.format(amount) + currency + "&a ᴀ &e" + targetData.getName() + "&a.");

        Player online = Bukkit.getPlayer(targetData.getUuid());
        if (online != null && online.isOnline()) {
            sendMessage(online, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ʀᴇᴄᴜ &e" + MoneyFormat.format(amount) + currency + "&a ᴅᴇ ʟᴀ ᴘᴀʀᴛ ᴅᴇ &e" + player.getName() + "&a !");
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
