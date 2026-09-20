package fr.crewcmoi.economie.listeners;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Téléporte le joueur avec la clé ItemsAdder configurée.
 *
 * Le plugin ne consomme, ne crée et ne gère aucune clé ou récompense :
 * la gestion des lootbox reste entièrement externe.
 */
public class LootboxKeyTeleportListener implements Listener {
    private final Main plugin;
    private final Map<UUID, Long> recentTeleports = new HashMap<>();

    public LootboxKeyTeleportListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        if (!isConfiguredKey(event.getItem()) && !isConfiguredKey(player.getInventory().getItemInMainHand())
                && !isConfiguredKey(player.getInventory().getItemInOffHand())) {
            return;
        }

        // Un clic peut générer deux événements (main/off-hand). On évite une double téléportation.
        long now = System.currentTimeMillis();
        long last = recentTeleports.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 750L) {
            event.setCancelled(true);
            return;
        }
        recentTeleports.put(player.getUniqueId(), now);

        event.setCancelled(true);
        teleportToAvailableDestination(player);
    }

    private void teleportToAvailableDestination(Player player) {
        ConfigurationSection destinations = plugin.getConfig().getConfigurationSection("lootbox-teleport.destinations");
        if (destinations == null) {
            send(player, "configuration-error", "&cAucune destination de téléportation de lootbox n'est configurée.");
            return;
        }

        for (String id : destinations.getKeys(false)) {
            String path = "lootbox-teleport.destinations." + id;
            Location destination = getLocation(path);
            if (destination == null) {
                plugin.getLogger().warning("Lootbox : destination invalide : " + id);
                continue;
            }

            if (isOccupied(destination, player, path)) {
                continue;
            }

            if (player.teleport(destination)) {
                String name = plugin.getConfig().getString(path + ".name", id);
                send(player, "teleported", "&dVous avez été téléporté vers &5%destination%&d."
                        .replace("%destination%", name));
                return;
            }
        }

        send(player, "all-occupied", "&cTous les emplacements de lootbox sont actuellement occupés.");
    }

    private boolean isOccupied(Location center, Player except, String path) {
        double radius = Math.max(0.1D, plugin.getConfig().getDouble(path + ".occupancy-radius", 4.0D));
        double radiusSquared = radius * radius;
        World world = center.getWorld();
        if (world == null) return true;

        for (Player other : world.getPlayers()) {
            if (other.equals(except)) continue;
            if (other.getLocation().distanceSquared(center) <= radiusSquared) {
                return true;
            }
        }
        return false;
    }

    private Location getLocation(String path) {
        String worldName = plugin.getConfig().getString(path + ".world", "").trim();
        if (worldName.isEmpty()) return null;

        World world = Bukkit.getWorld(worldName);
        if (world == null) return null;

        return new Location(
                world,
                plugin.getConfig().getDouble(path + ".x"),
                plugin.getConfig().getDouble(path + ".y"),
                plugin.getConfig().getDouble(path + ".z"),
                (float) plugin.getConfig().getDouble(path + ".yaw", 0.0D),
                (float) plugin.getConfig().getDouble(path + ".pitch", 0.0D)
        );
    }

    private boolean isConfiguredKey(org.bukkit.inventory.ItemStack item) {
        if (item == null || item.getType().isAir()) return false;

        String configuredId = plugin.getConfig().getString("lootbox-teleport.key-itemsadder-id", "").trim();
        if (configuredId.isEmpty()) return false;

        CustomStack custom = CustomStack.byItemStack(item);
        return custom != null && configuredId.equalsIgnoreCase(custom.getNamespacedID());
    }

    private void send(Player player, String key, String fallback) {
        String message = plugin.getConfig().getString("lootbox-teleport.messages." + key, fallback);
        player.sendMessage(Messages.color(message));
    }
}
