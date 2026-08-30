package fr.crewcmoi.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.CombatManager;
import fr.crewcmoi.managers.TeleportManager;
import fr.crewcmoi.utils.TeleportMessages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /tpahere <joueur> : demande à ce qu'un autre joueur soit téléporté jusqu'à soi.
 * La cible se téléportera vers le demandeur une fois qu'elle aura accepté.
 */
public class TpaHereCommand implements CommandExecutor {

    private final Main plugin;
    private final TeleportManager teleportManager;
    private final CombatManager combatManager;

    public TpaHereCommand(Main plugin, TeleportManager teleportManager, CombatManager combatManager) {
        this.plugin = plugin;
        this.teleportManager = teleportManager;
        this.combatManager = combatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande doit être exécutée par un joueur.");
            return true;
        }

        Player requester = (Player) sender;

        if (args.length != 1) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("tpa.usage-tpahere", "&cUsage : /tpahere <joueur>")));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("general.player-not-found", "&cCe joueur n'existe pas ou n'est pas en ligne.")));
            return true;
        }

        if (target.getUniqueId().equals(requester.getUniqueId())) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("tpa.self", "&cVous ne pouvez pas vous téléporter à vous-même.")));
            return true;
        }

        // C'est la cible qui va se déplacer : on vérifie donc son statut de combat à elle.
        if (combatManager != null && combatManager.isInCombat(target)) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("tpa.target-in-combat", "&c{player} est en combat et ne peut pas être téléporté.")
                                    .replace("{player}", target.getName())));
            return true;
        }

        teleportManager.createRequest(requester, target, TeleportManager.RequestType.TPAHERE);

        requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") +
                        plugin.getMessages().getString("tpa.sent-here", "&aDemande de téléportation envoyée à &e{player}&a.")
                                .replace("{player}", target.getName())));

        TeleportMessages.sendRequestReceived(plugin, target, requester, "tpa.received-here");

        return true;
    }
}
