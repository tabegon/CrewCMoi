package fr.crewcmoi.moderation.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.database.BountyEntry;
import fr.crewcmoi.other.database.PlayerData;
import fr.crewcmoi.pvp.managers.BountyManager;
import fr.crewcmoi.pvp.managers.MalusEffectManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.crewcmoi.other.utils.MoneyFormat;

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
            Messages.send(sender, "server.info-045b8ea");
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
                Messages.send(sender, "server.info-48408b8");
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

        Messages.send(sender, "server.info-f213575");
        Messages.send(sender, "server.info-title", java.util.Map.of("player", resolvedName));
        Messages.send(sender, "server.info-total-bounty", java.util.Map.of("amount", MoneyFormat.format(total) + currency));
        Messages.send(sender, "server.info-server-bounty", java.util.Map.of("amount", MoneyFormat.format(serverTotal) + currency));
        Messages.send(sender, "server.info-unluck-tier", java.util.Map.of(
                "tier", tierLabel(tier),
                "reduction", tier > 0 ? " &7(&c-" + percentFormat.format(reductionPercent) + "%&7 ᴅᴇ ᴅᴇɢᴀᴛꜱ ɪɴꜰʟɪɢᴇꜱ)" : ""), true);
        Messages.send(sender, "server.info-hearts", java.util.Map.of("removed", heartsRemoved, "max", malusEffectManager.getMaxHeartsRemoved()));
        Messages.send(sender, "server.info-max-health", java.util.Map.of("current", (int)(currentMaxHealth/2), "base", (int)(baseMaxHealth/2)));
        Messages.send(sender, "server.info-f213575x");
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
