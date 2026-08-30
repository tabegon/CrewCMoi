package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.database.BountyEntry;
import fr.crewcmoi.database.PlayerData;
import fr.crewcmoi.pvp.managers.BountyManager;
import fr.crewcmoi.pvp.managers.MalusEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.crewcmoi.utils.MoneyFormat;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Commande /info : tableau de bord d'informations sur un joueur.
 *  - /info bounty <joueur> : prime totale/serveur, palier de malchance (Unluck)
 *    actuel et nombre de coeurs retirés par le malus de prime serveur.
 */
public class InfoCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final BountyManager bountyManager;
    private final MalusEffectManager malusEffectManager;
    private final DecimalFormat percentFormat = new DecimalFormat("#,##0.00");

    public InfoCommand(Main plugin, BountyManager bountyManager, MalusEffectManager malusEffectManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
        this.malusEffectManager = malusEffectManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2 || !args[0].equalsIgnoreCase("bounty")) {
            sendMessage(sender, "&cᴜꜱᴀɢᴇ : /ɪɴꜰᴏ ʙᴏᴜɴᴛʏ <ᴊᴏᴜᴇᴜʀ>");
            return true;
        }

        String targetName = args[1];
        UUID targetUuid;
        String resolvedName;

        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            targetUuid = online.getUniqueId();
            resolvedName = online.getName();
        } else {
            PlayerData data = plugin.getEconomyManager().getPlayerDataByName(targetName);
            if (data == null) {
                sendMessage(sender, "&cᴄᴇ ᴊᴏᴜᴇᴜʀ ɴ'ᴇxɪꜱᴛᴇ ᴘᴀꜱ ᴏᴜ ɴ'ᴀ ᴊᴀᴍᴀɪꜱ ʀᴇᴊᴏɪɴᴛ ʟᴇ ꜱᴇʀᴠᴇᴜʀ.");
                return true;
            }
            targetUuid = data.getUuid();
            resolvedName = data.getName();
        }

        List<BountyEntry> entries = bountyManager.getBounties(targetUuid);
        double total = entries.stream().mapToDouble(BountyEntry::getAmount).sum();
        double serverTotal = entries.stream()
                .filter(BountyEntry::isServerBounty)
                .mapToDouble(BountyEntry::getAmount)
                .sum();

        int tier = malusEffectManager.computeReductionLevel(serverTotal);
        double reductionPercent = malusEffectManager.getReductionPercentForLevel(tier) * 100.0;
        int heartsRemoved = malusEffectManager.computeHeartsRemoved(serverTotal);
        double baseMaxHealth = malusEffectManager.getBaseMaxHealth();
        double currentMaxHealth = Math.max(2.0, baseMaxHealth - (heartsRemoved * 2.0));
        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        sendMessage(sender, "&8&m----------------------------------------");
        sendMessage(sender, "&4&lᴛᴀʙʟᴇᴀᴜ ᴅᴇ ʙᴏʀᴅ &7- &e" + resolvedName);
        sendMessage(sender, "&7ᴘʀɪᴍᴇ ᴛᴏᴛᴀʟᴇ : &a" + MoneyFormat.format(total) + currency);
        sendMessage(sender, "&7ᴅᴏɴᴛ ᴘʀɪᴍᴇ ꜱᴇʀᴠᴇᴜʀ : &a" + MoneyFormat.format(serverTotal) + currency);
        sendMessage(sender, "&7ᴘᴀʟɪᴇʀ ᴅᴇ ᴍᴀʟᴄʜᴀɴᴄᴇ (ᴜɴʟᴜᴄᴋ) : " + tierLabel(tier)
                + (tier > 0 ? " &7(&c-" + percentFormat.format(reductionPercent) + "%&7 ᴅᴇ ᴅᴇɢᴀᴛꜱ ɪɴꜰʟɪɢᴇꜱ)" : ""));
        sendMessage(sender, "&7ᴄᴏᴇᴜʀꜱ ʀᴇᴛɪʀᴇꜱ : &c" + heartsRemoved + " &7/ &e" + malusEffectManager.getMaxHeartsRemoved());
        sendMessage(sender, "&7ᴠɪᴇ ᴍᴀx ᴀᴄᴛᴜᴇʟʟᴇ : &e" + (int) (currentMaxHealth / 2) + " &7♥ &8(ꜱᴜʀ " + (int) (baseMaxHealth / 2) + ")");
        sendMessage(sender, "&8&m----------------------------------------");
        return true;
    }

    private String tierLabel(int tier) {
        return switch (tier) {
            case 1 -> "&e&lᴛɪᴇʀ 1";
            case 2 -> "&6&lᴛɪᴇʀ 2";
            case 3 -> "&4&lᴛɪᴇʀ 3";
            default -> "&7ᴀᴜᴄᴜɴ";
        };
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            String partial = args[0].toLowerCase();
            if ("bounty".startsWith(partial)) {
                completions.add("bounty");
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("bounty")) {
            String partial = args[1].toLowerCase();
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
