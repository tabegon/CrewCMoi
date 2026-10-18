package fr.crewcmoi.teleport.commands;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.Messages;
import fr.crewcmoi.teleport.managers.TeleportManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpAcceptBulkCommand implements CommandExecutor {
    private final TeleportManager teleportManager;
    private final boolean teamOnly;

    public TpAcceptBulkCommand(Main plugin, TeleportManager teleportManager, boolean teamOnly) {
        this.teleportManager = teleportManager;
        this.teamOnly = teamOnly;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "teleport.tpaccept.player-only");
            return true;
        }
        if (args.length > 0) {
            player.sendMessage(org.bukkit.ChatColor.RED + "Usage : /" + label);
            return true;
        }
        teleportManager.acceptAll(player, teamOnly);
        return true;
    }
}
