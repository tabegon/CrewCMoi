package fr.crewcmoi.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.HomeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Gère la sauvegarde et la récupération du home (unique) de chaque joueur.
 * Les accès base de données sont effectués de façon asynchrone.
 */
public class HomeManager {

    private final Main plugin;
    private final DatabaseManager databaseManager;

    public HomeManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    /**
     * Définit (ou remplace) le home du joueur à sa position actuelle.
     */
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

    /**
     * Récupère le home du joueur de façon asynchrone puis exécute le callback (sur le
     * thread principal) avec le résultat, ou null si le joueur n'a pas de home.
     */
    public void getHome(Player player, java.util.function.Consumer<HomeData> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            HomeData home = databaseManager.getHome(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> callback.accept(home));
        });
    }
}
