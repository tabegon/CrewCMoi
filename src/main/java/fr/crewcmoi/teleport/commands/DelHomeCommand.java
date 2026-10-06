package fr.crewcmoi.teleport.commands;

import fr.crewcmoi.other.utils.Messages;
import fr.crewcmoi.teleport.database.HomeData;
import fr.crewcmoi.teleport.managers.HomeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DelHomeCommand implements CommandExecutor, TabCompleter {
    private final HomeManager manager;

    public DelHomeCommand(HomeManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            Messages.send(sender, "teleport.delhome.player-only");
            return true;
        }
        if (args.length != 1) {
            Messages.send(player, "teleport.delhome.usage");
            return true;
        }
        String name = args[0];
        manager.deleteHome(player, name, deleted -> Messages.send(player,
                deleted ? "teleport.delhome.success" : "teleport.delhome.not-found",
                Map.of("name", name)));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player player) || args.length != 1) {
            return Collections.emptyList();
        }
        String prefix = args[0].toLowerCase();
        List<String> result = new ArrayList<>();
        for (HomeData home : manager.getHomes(player.getUniqueId())) {
            if (home.getName().toLowerCase().startsWith(prefix)) {
                result.add(home.getName());
            }
        }
        return result;
    }
}
