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
            Messages.send(sender, "teleport.tpahere.player-only");
            return true;
        }

        Player requester = (Player) sender;

        if (args.length != 1) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("tpa.usage-tpahere")));
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || !target.isOnline()) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("general.player-not-found")));
            return true;
        }

        if (target.getUniqueId().equals(requester.getUniqueId())) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("tpa.self")));
            return true;
        }

        if (combatManager != null && combatManager.isInCombat(target)) {
            requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                    plugin.getMessages().getString("prefix") +
                            plugin.getMessages().getString("tpa.target-in-combat")
                                    .replace("{player}", target.getName())));
            return true;
        }

        teleportManager.createRequest(requester, target, TeleportManager.RequestType.TPAHERE);

        requester.sendMessage(ChatColor.translateAlternateColorCodes('&',
                plugin.getMessages().getString("prefix") +
                        plugin.getMessages().getString("tpa.sent-here")
                                .replace("{player}", target.getName())));

        TeleportMessages.sendRequestReceived(plugin, target, requester, "tpa.received-here");

        return true;
    }
}
