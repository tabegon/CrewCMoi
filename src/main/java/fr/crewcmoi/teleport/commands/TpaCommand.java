package fr.crewcmoi.teleport.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.managers.CombatManager;
import fr.crewcmoi.teleport.managers.TeleportManager;
import fr.crewcmoi.teleport.utils.TeleportMessages;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Commande /tpa <joueur> : demande à être téléporté vers un autre joueur.
 * Le demandeur se téléportera vers la cible une fois que celle-ci aura accepté.
 */
public class TpaCommand implements CommandExecutor {

    private final Main plugin;
    private final TeleportManager teleportManager;
    private final CombatManager combatManager;

    public TpaCommand(Main plugin, TeleportManager teleportManager, CombatManager combatManager) {
        this.plugin = plugin;
        this.teleportManager = teleportManager;
        this.combatManager = combatManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, "server.tpa-8660550");
            return true;
        }

        Player requester = (Player) sender;

        if (args.length != 1) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("tpa.usage-tpa", "&cUsage : /tpa <joueur>")));
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

        if (combatManager != null && combatManager.isInCombat(requester)) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix", "") +
                            plugin.getMessages().getString("combat-log.action-blocked", "&cVous ne pouvez pas faire ça en combat !")
                                    .replace("{seconds}", String.valueOf(combatManager.getRemainingSeconds(requester)))));
            return true;
        }

        teleportManager.createRequest(requester, target, TeleportManager.RequestType.TPA);

        requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix", "") +
                        plugin.getMessages().getString("tpa.sent", "&aDemande de téléportation envoyée à &e{player}&a.")
                                .replace("{player}", target.getName())));

        TeleportMessages.sendRequestReceived(plugin, target, requester, "tpa.received");

        return true;
    }
}
