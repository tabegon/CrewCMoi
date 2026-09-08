package fr.crewcmoi.claims.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.claims.gui.ClaimAdminGuiManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ClaimAdminCommand implements CommandExecutor {

    private final ClaimAdminGuiManager guiManager;

    public ClaimAdminCommand(ClaimAdminGuiManager guiManager) {
        this.guiManager = guiManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "general.player-only");
            return true;
        }
        if (!player.hasPermission("crew.claim.admin")) {
            Messages.send(player, "general.no-permission");
            return true;
        }
        guiManager.open(player);
        return true;
    }
}
