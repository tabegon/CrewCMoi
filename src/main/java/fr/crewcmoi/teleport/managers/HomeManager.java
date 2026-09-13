package fr.crewcmoi.teleport.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.managers.DatabaseManager;
import fr.crewcmoi.teleport.database.HomeData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class HomeManager {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    public HomeManager(Main plugin, DatabaseManager databaseManager) { this.plugin=plugin; this.databaseManager=databaseManager; }

    public int getMaxHomes(UUID uuid) { return databaseManager.getHomeSlots(uuid); }
    public List<HomeData> getHomes(UUID uuid) { return databaseManager.getHomes(uuid); }
    public List<HomeData> getAllHomes() { return databaseManager.getAllHomes(); }
    public int getHomeCount(UUID uuid) { return databaseManager.getHomes(uuid).size(); }

    public void setHome(Player player, String name, Consumer<Boolean> callback) {
        String world = player.getWorld().getName(); Location loc=player.getLocation();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<HomeData> homes=databaseManager.getHomes(player.getUniqueId());
            boolean exists=homes.stream().anyMatch(h -> h.getName().equalsIgnoreCase(name));
            if(!exists && homes.size() >= databaseManager.getHomeSlots(player.getUniqueId())) { Bukkit.getScheduler().runTask(plugin,()->callback.accept(false)); return; }
            databaseManager.setHome(player.getUniqueId(), name, world, loc.getX(),loc.getY(),loc.getZ(),loc.getYaw(),loc.getPitch());
            Bukkit.getScheduler().runTask(plugin,()->callback.accept(true));
        });
    }

    public void getHome(Player player, String name, Consumer<HomeData> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin,()->{ HomeData h=databaseManager.getHome(player.getUniqueId(),name); Bukkit.getScheduler().runTask(plugin,()->callback.accept(h)); });
    }

    public void purchaseSlot(Player player, double price, Consumer<Boolean> callback) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int slots=databaseManager.getHomeSlots(player.getUniqueId());
            Bukkit.getScheduler().runTask(plugin, () -> {
                if(!plugin.getEconomyManager().withdraw(player.getUniqueId(),price)){ callback.accept(false); return; }
                databaseManager.setHomeSlots(player.getUniqueId(),slots+1); callback.accept(true);
            });
        });
    }
}
