package fr.crewcmoi.pvp.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.gui.BountyGuiManager;
import fr.crewcmoi.pvp.gui.BountyReviewGuiManager;
import fr.crewcmoi.pvp.managers.BountyManager;
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
 * Commande /bounty :
 *  - /bounty                       : ouvre la GUI listant les primes actives.
 *  - /bounty add <joueur> <montant> [raison] : place une prime sur un joueur (le montant est débité immédiatement).
 *  - /bounty review                          : (admins/op) ouvre la GUI de validation des raisons de primes en attente.
 */
public class BountyCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final BountyManager bountyManager;
    private final BountyGuiManager bountyGuiManager;
    private final BountyReviewGuiManager bountyReviewGuiManager;

    public BountyCommand(Main plugin, BountyManager bountyManager, BountyGuiManager bountyGuiManager,
                          BountyReviewGuiManager bountyReviewGuiManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.bountyGuiManager = bountyGuiManager;
        this.bountyReviewGuiManager = bountyReviewGuiManager;
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

        if (args[0].equalsIgnoreCase("review")) {
            if (!player.isOp() && !player.hasPermission("crew.admin")) {
                sendMessage(player, "&cᴘᴇʀᴍɪꜱꜱɪᴏɴ ʀᴇꜰᴜꜱᴇᴇ.");
                return true;
            }
            bountyReviewGuiManager.open(player, 0);
            return true;
        }

        sendMessage(player, "&cᴜꜱᴀɢᴇ : /ʙᴏᴜɴᴛʏ &7ᴏᴜ&c /ʙᴏᴜɴᴛʏ ᴀᴅᴅ <ᴊᴏᴜᴇᴜʀ> <ᴍᴏɴᴛᴀɴᴛ> [ʀᴀɪꜱᴏɴ]");
        return true;
    }

    private void handleAdd(Player player, String[] args) {
        if (args.length < 3) {
            sendMessage(player, "&cᴜꜱᴀɢᴇ : /ʙᴏᴜɴᴛʏ ᴀᴅᴅ <ᴊᴏᴜᴇᴜʀ> <ᴍᴏɴᴛᴀɴᴛ> [ʀᴀɪꜱᴏɴ]");
            return;
        }

        String targetName = args[1];
        double amount;
        try {
            amount = MoneyFormat.parse(args[2]);
        } catch (NumberFormatException e) {
            sendMessage(player, "&cᴍᴏɴᴛᴀɴᴛ ɪɴᴠᴀʟɪᴅᴇ : " + args[2]);
            return;
        }

        // Raison optionnelle : tout ce qui suit le montant, rejoint en une seule chaîne.
        String reason = null;
        if (args.length > 3) {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
        }
        String finalReason = reason;

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        bountyManager.addPlayerBounty(player, targetName, amount, finalReason, result -> {
            switch (result) {
                case SUCCESS -> {
                    sendMessage(player, "&aᴠᴏᴜꜱ ᴀᴠᴇᴢ ᴘʟᴀᴄᴇ ᴜɴᴇ ᴘʀɪᴍᴇ ᴅᴇ &e" + MoneyFormat.format(amount) + currency + "&a ꜱᴜʀ &e" + targetName + "&a !");
                    Player online = Bukkit.getPlayerExact(targetName);
                    if (online != null) {
                        String reasonSuffix = (finalReason != null && !finalReason.isBlank())
                                ? " &7(&e" + finalReason + "&7)"
                                : "";
                        sendMessage(online, "&c" + player.getName() + " ᴀ ᴘʟᴀᴄᴇ ᴜɴᴇ ᴘʀɪᴍᴇ ᴅᴇ &e" + MoneyFormat.format(amount) + currency + "&c ꜱᴜʀ ᴠᴏᴜꜱ !" + reasonSuffix);
                    }
                }
                case INVALID_AMOUNT -> sendMessage(player, "&cʟᴇ ᴍᴏɴᴛᴀɴᴛ ᴅᴏɪᴛ ᴇᴛʀᴇ ᴜɴ ɴᴏᴍʙʀᴇ ᴘᴏꜱɪᴛɪꜰ.");
                case SELF_TARGET -> sendMessage(player, "&cᴠᴏᴜꜱ ɴᴇ ᴘᴏᴜᴠᴇᴢ ᴘᴀꜱ ᴘʟᴀᴄᴇʀ ᴅᴇ ᴘʀɪᴍᴇ ꜱᴜʀ ᴠᴏᴜꜱ-ᴍᴇᴍᴇ.");
                case NOT_ENOUGH_MONEY -> sendMessage(player, "&cᴠᴏᴜꜱ ɴ'ᴀᴠᴇᴢ ᴘᴀꜱ ᴀꜱꜱᴇᴢ ᴅ'ᴀʀɢᴇɴᴛ ᴘᴏᴜʀ ᴘʟᴀᴄᴇʀ &e" + MoneyFormat.format(amount) + currency + "&c.");
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
            if ("review".startsWith(partial) && sender instanceof Player p && (p.isOp() || p.hasPermission("crew.admin"))) {
                completions.add("review");
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
        } else if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            completions.add("raison");
        }

        return completions;
    }

    private void sendMessage(CommandSender sender, String message) {
        String prefix = plugin.getMessages().getString("prefix", "");
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + message));
    }
}
