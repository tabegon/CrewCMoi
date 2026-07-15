package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.gui.BountyGuiManager;
import fr.crewcmoi.managers.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Commande /bounty :
 *  - /bounty                       : ouvre la GUI listant les primes actives.
 *  - /bounty add <joueur> <montant> : place une prime sur un joueur (le montant est débité immédiatement).
 */
public class BountyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final BountyManager bountyManager;
    private final BountyGuiManager bountyGuiManager;
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    public BountyCommand(Main plugin, BountyManager bountyManager, BountyGuiManager bountyGuiManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.bountyGuiManager = bountyGuiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sendMessage(sender, "&cᴄᴇᴛᴛᴇ ᴄᴏᴍᴍᴀɴᴅᴇ ᴅᴏɪᴛ ᴇᴛʀᴇ ᴇxᴇᴄᴜᴛᴇᴇ ᴘᴀʀ ᴜɴ ᴊᴏᴜᴇᴜʀ.");
            return true;
        }

        if (args.length == 0) {
            bountyGuiManager.open(player, 0);
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            handleAdd(player, args);
            return true;
        }

        sendMessage(player, "&cᴜꜱᴀɢᴇ : /ʙᴏᴜɴᴛʏ &7ᴏᴜ&c /ʙᴏᴜɴᴛʏ ᴀᴅᴅ <ᴊᴏᴜᴇᴜʀ> <ᴍᴏɴᴛᴀɴᴛ>");
        return true;
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "&cᴜꜱᴀɢᴇ : /ʙᴏᴜɴᴛʏ ᴀᴅᴅ <ᴊᴏᴜᴇᴜʀ> <ᴍᴏɴᴛᴀɴᴛ>");
            return;
        }

        String targetName = args[1];
        double amount;
        try {
            amount = Double.parseDouble(args[2].replace(",", "."));
        } catch (NumberFormatException e) {
            sendMessage(player, "&cᴍᴏɴᴛᴀɴᴛ ɪɴᴠᴀʟɪᴅᴇ : " + args[2]);
            return;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        bountyManager.addPlayerBounty(player, targetName, amount, result -> {
            switch (result) {
                case SUCCESS -> {
                    sendMessage(player, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ᴘʟᴀᴄᴇ ᴜɴᴇ ᴘʀɪᴍᴇ ᴅᴇ &e" + format.format(amount) + currency + "&a ꜱᴜʀ &e" + targetName + "&a !");
                    Player online = Bukkit.getPlayerExact(targetName);
                    if (online != null) {
                        sendMessage(online, "&c" + player.getName() + " ᴀ ᴘʟᴀᴄᴇ ᴜɴᴇ ᴘʀɪᴍᴇ ᴅᴇ &e" + format.format(amount) + currency + "&c ꜱᴜʀ ᴠᴏᴜꜱ !");
                    }
                }
                case INVALID_AMOUNT -> sendMessage(player, "&cʟᴇ ᴍᴏɴᴛᴀɴᴛ ᴅᴏɪᴛ ᴇᴛʀᴇ ᴜɴ ɴᴏᴍʙʀᴇ ᴘᴏꜱɪᴛɪꜰ.");
                case SELF_TARGET -> sendMessage(player, "&cᴠᴏᴜꜱ ɴᴇ ᴘᴏᴜᴠᴇᴢ ᴘᴀꜱ ᴘʟᴀᴄᴇʀ ᴅᴇ ᴘʀɪᴍᴇ ꜱᴜʀ ᴠᴏᴜꜱ-ᴍᴇᴍᴇ.");
                case NOT_ENOUGH_MONEY -> sendMessage(player, "&cᴠᴏᴜꜱ ɴ'ᴀᴠᴇᴢ ᴘᴀꜱ ᴀꜱꜱᴇᴢ ᴅ'ᴀʀɢᴇɴᴛ ᴘᴏᴜʀ ᴘʟᴀᴄᴇʀ &e" + format.format(amount) + currency + "&c.");
                case TARGET_NOT_FOUND -> sendMessage(player, "&cᴄᴇ ᴊᴏᴜᴇᴜʀ ɴ'ᴇxɪꜱᴛᴇ ᴘᴀꜱ ᴏᴜ ɴ'ᴀ ᴊᴀᴍᴀɪꜱ ʀᴇᴊᴏɪɴᴛ ʟᴇ ꜱᴇʀᴠᴇᴜʀ.");
            }
        });
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("add".startsWith(partial)) {
                completions.add("add");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
            String partial = args[1].toLowerCase();
            for (Player online : Bukkit.getOnlinePlayers()) {
                if (online.getName().toLowerCase().startsWith(partial)
                        && !(sender instanceof Player p && p.getUniqueId().equals(online.getUniqueId()))) {
                    completions.add(online.getName());
                }
            }
        } else if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
            completions.add("100");
        }

        return completions;
    }

    private void sendMessage(CommandSender sender, String message) {
        String prefix = plugin.getMessages().getString("prefix", "");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
