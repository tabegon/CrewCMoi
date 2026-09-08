package fr.crewcmoi.moderation.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.moderation.managers.StaffModeManager;
import fr.crewcmoi.moderation.managers.VanishManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class VanishCommand implements CommandExecutor {

    private final StaffModeManager staffModeManager;
    private final VanishManager vanishManager;

    public VanishCommand(StaffModeManager staffModeManager, VanishManager vanishManager) {
        this.staffModeManager = staffModeManager;
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "moderation.vanish.player-only");
            return true;
        }

        if (!staffModeManager.isActive(player.getUniqueId())) {
            Messages.send(player, "moderation.vanish.staff-required");
            return true;
        }

        boolean nowVanished = vanishManager.toggle(player);
        if (nowVanished) {
            Messages.send(player, "moderation.vanish.enabled");
        } else {
            Messages.send(player, "moderation.vanish.disabled");
        }
        return true;
    }
}
