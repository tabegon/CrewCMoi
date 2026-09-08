package fr.crewcmoi.teleport.commands;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SpawnCommand implements CommandExecutor {

    private final Main plugin;

    public SpawnCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            Messages.send(sender, "teleport.spawn.player-only");
            return true;
        }

        Player player = (Player) sender;
        FileConfiguration config = plugin.getConfig();

        String worldName = config.getString("spawn.world", "world");
        World world = Bukkit.getWorld(worldName);

        if (world == null) {
            Messages.send(player, "teleport.spawn.world-not-found", java.util.Map.of("world", worldName), true);
            return true;
        }

        double x = config.getDouble("spawn.x", 0.5);
        double y = config.getDouble("spawn.y", 100.0);
        double z = config.getDouble("spawn.z", 0.5);
        float yaw = (float) config.getDouble("spawn.yaw", 0.0);
        float pitch = (float) config.getDouble("spawn.pitch", 0.0);

        Location spawnLocation = new Location(world, x, y, z, yaw, pitch);
        player.teleport(spawnLocation);

        Messages.send(player, "teleport.spawn.teleported", java.util.Map.of(), true);

        return true;
    }
}
