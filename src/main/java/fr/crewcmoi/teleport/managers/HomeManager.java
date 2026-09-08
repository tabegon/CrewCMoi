package fr.crewcmoi.teleport.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.managers.DatabaseManager;
import fr.crewcmoi.teleport.database.HomeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class HomeManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    public HomeManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public void setHome(Player player, Runnable onSuccess) {
        Location loc = player.getLocation();
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        double x = loc.getX();
        double y = loc.getY();
        double z = loc.getZ();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            databaseManager.setHome(player.getUniqueId(), world, x, y, z, yaw, pitch);
            if (onSuccess != null) {
                Bukkit.getScheduler().runTask(plugin, onSuccess);
            }
        });
    }

    public void getHome(Player player, java.util.function.Consumer<HomeData> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HomeData home = databaseManager.getHome(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(home));
        });
    }
}
