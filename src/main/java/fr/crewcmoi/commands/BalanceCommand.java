package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.PlayerData;
import fr.crewcmoi.managers.EconomyManager;
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
        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sendMessage(sender, "&cᴄᴇᴛᴛᴇ ᴄᴏᴍᴍᴀɴᴅᴇ ᴅᴏɪᴛ ᴇᴛʀᴇ ᴇxᴇᴄᴜᴛᴇᴇ ᴘᴀʀ ᴜɴ ᴊᴏᴜᴇᴜʀ, ᴏᴜ ᴘʀᴇᴄɪꜱᴇᴢ ᴜɴ ᴘꜱᴇᴜᴅᴏ : /ʙᴀʟᴀɴᴄᴇ <ᴊᴏᴜᴇᴜʀ>");
                return true;
            }

            Player player = (Player) sender;
            double balance = economyManager.getBalance(player.getUniqueId());
            sendMessage(sender, "&7ᴠᴏᴛʀᴇ ꜱᴏʟᴅᴇ : &a" + format.format(balance) + currency);
            return true;
        }

        // /balance <joueur>
        if (!sender.hasPermission("crew.balance.others")) {
            sendMessage(sender, "&cᴠᴏᴜꜱ ɴ'ᴀᴠᴇᴢ ᴘᴀꜱ ʟᴀ ᴘᴇʀᴍɪꜱꜱɪᴏɴ ᴅᴇ ᴠᴏɪʀ ʟᴇ ꜱᴏʟᴅᴇ ᴅ'ᴜɴ ᴀᴜᴛʀᴇ ᴊᴏᴜᴇᴜʀ.");
            return true;
        }

        String targetName = args[0];
        PlayerData data = economyManager.getPlayerDataByName(targetName);

        if (data == null) {
            sendMessage(sender, "&cᴄᴇ ᴊᴏᴜᴇᴜʀ ɴ'ᴇxɪꜱᴛᴇ ᴘᴀꜱ ᴏᴜ ɴ'ᴀ ᴊᴀᴍᴀɪꜱ ʀᴇᴊᴏɪɴᴛ ʟᴇ ꜱᴇʀᴠᴇᴜʀ.");
            return true;
        }

        sendMessage(sender, "&7ꜱᴏʟᴅᴇ ᴅᴇ &e" + data.getName() + "&7 : &a" + format.format(data.getBalance()) + currency);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1 && sender.hasPermission("crew.balance.others")) {
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
