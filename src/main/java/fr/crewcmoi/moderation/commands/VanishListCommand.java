package fr.crewcmoi.moderation.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.moderation.managers.VanishManager;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class VanishListCommand implements CommandExecutor {

    private final VanishManager vanishManager;

    public VanishListCommand(VanishManager vanishManager) {
        this.vanishManager = vanishManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("crew.vanish.list")) {
            Messages.send(sender, "moderation.vanish-list.no-permission");
            return true;
        }

        List<String> names = new ArrayList<>();
        for (UUID uuid : vanishManager.getVanishedPlayers()) {
            Player player = Bukkit.getPlayer(uuid);
            names.add(player != null ? player.getName() : uuid.toString());
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);

        Messages.send(sender, "moderation.vanish-list.separator");
        Messages.send(sender, "moderation.vanish-list.header", java.util.Map.of("count", names.size()));
        if (names.isEmpty()) {
            Messages.send(sender, "moderation.vanish-list.empty");
        } else {
            Messages.send(sender, "moderation.vanish-list.players", java.util.Map.of("players", String.join("§7, §a", names)));
        }
        Messages.send(sender, "moderation.vanish-list.separator");
        return true;
    }
}
