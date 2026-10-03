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
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Deque;
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
    private final Deque<UUID> waitingQueue = new ArrayDeque<>();

    public LootboxKeyTeleportListener(Main plugin) {
        this.plugin = plugin;
        long interval = Math.max(1L, plugin.getConfig().getLong("lootbox-teleport.queue-check-ticks", 10L));
        Bukkit.getScheduler().runTaskTimer(plugin, this::processWaitingQueue, interval, interval);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        waitingQueue.remove(event.getPlayer().getUniqueId());
        recentTeleports.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onRightClick(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        // On ne traite que l'item réellement utilisé pour ce clic.
        // Cela évite notamment de consommer une clé de l'autre main.
        if (!isConfiguredKey(event.getItem())) {
            return;
        }

        // Un clic peut générer deux événements (main/off-hand). On évite une double téléportation.
        long now = System.currentTimeMillis();
        long last = recentTeleports.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 750L) {
            event.setCancelled(true);
            return;
        }
        event.setCancelled(true);

        // La clé est consommée uniquement après une téléportation réussie.
        // Ainsi, une erreur de configuration, une destination occupée ou un
        // échec de téléportation ne fait jamais perdre la clé au joueur.
        try {
            if (teleportToAvailableDestination(player)) {
                consumeKey(event);
                recentTeleports.put(player.getUniqueId(), now);
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Lootbox : erreur pendant la téléportation, clé conservée : " + ex.getMessage());
        }
    }


    private void consumeKey(PlayerInteractEvent event) {
        org.bukkit.inventory.ItemStack item = event.getItem();
        if (item == null) return;

        int amount = item.getAmount();
        if (amount <= 1) {
            if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
                event.getPlayer().getInventory().setItemInOffHand(null);
            } else {
                event.getPlayer().getInventory().setItemInMainHand(null);
            }
            return;
        }

        item.setAmount(amount - 1);
        if (event.getHand() == org.bukkit.inventory.EquipmentSlot.OFF_HAND) {
            event.getPlayer().getInventory().setItemInOffHand(item);
        } else {
            event.getPlayer().getInventory().setItemInMainHand(item);
        }
    }

    private boolean teleportToAvailableDestination(Player player) {
        ConfigurationSection destinations = plugin.getConfig().getConfigurationSection("lootbox-teleport.destinations");
        if (destinations == null) {
            send(player, "configuration-error", "&cAucune destination de téléportation de lootbox n'est configurée.");
            return false;
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
                return true;
            }
        }

        UUID uuid = player.getUniqueId();
        if (!waitingQueue.contains(uuid)) {
            waitingQueue.addLast(uuid);
        }

        int position = 1;
        for (UUID queued : waitingQueue) {
            if (queued.equals(uuid)) break;
            position++;
        }

        String message = plugin.getConfig().getString(
                "lootbox-teleport.messages.queued",
                "&eLes deux salles sont occupées. &7Vous êtes ajouté à la file d'attente (&f#%position%&7)."
        ).replace("%position%", String.valueOf(position));
        player.sendMessage(Messages.color(message));
        return false;
    }

    private void processWaitingQueue() {
        while (!waitingQueue.isEmpty()) {
            UUID uuid = waitingQueue.peekFirst();
            Player player = Bukkit.getPlayer(uuid);

            if (player == null || !player.isOnline()) {
                waitingQueue.removeFirst();
                continue;
            }

            Location destination = findAvailableDestination(player);
            if (destination == null) {
                return;
            }

            if (!hasConfiguredKey(player)) {
                waitingQueue.removeFirst();
                send(player, "queue-no-key", "&cVous n'avez plus la clé de lootbox, vous avez été retiré de la file d'attente.");
                continue;
            }

            if (!player.teleport(destination)) {
                return;
            }

            waitingQueue.removeFirst();
            consumeConfiguredKey(player);

            String destinationName = findDestinationName(destination);
            send(player, "teleported-from-queue", "&dUne salle de lootbox vient de se libérer ! &7Vous êtes téléporté vers &5%destination%&7."
                    .replace("%destination%", destinationName));
        }
    }

    private Location findAvailableDestination(Player player) {
        ConfigurationSection destinations = plugin.getConfig().getConfigurationSection("lootbox-teleport.destinations");
        if (destinations == null) return null;

        for (String id : destinations.getKeys(false)) {
            String path = "lootbox-teleport.destinations." + id;
            Location destination = getLocation(path);
            if (destination == null) continue;
            if (!isOccupied(destination, player, path)) return destination;
        }
        return null;
    }

    private String findDestinationName(Location destination) {
        ConfigurationSection destinations = plugin.getConfig().getConfigurationSection("lootbox-teleport.destinations");
        if (destinations == null) return "la salle de lootbox";
        for (String id : destinations.getKeys(false)) {
            String path = "lootbox-teleport.destinations." + id;
            Location configured = getLocation(path);
            if (configured != null && configured.getWorld() != null && destination.getWorld() != null
                    && configured.getWorld().getUID().equals(destination.getWorld().getUID())
                    && configured.distanceSquared(destination) < 0.01D) {
                return plugin.getConfig().getString(path + ".name", id);
            }
        }
        return "la salle de lootbox";
    }

    private boolean hasConfiguredKey(Player player) {
        for (org.bukkit.inventory.ItemStack item : player.getInventory().getContents()) {
            if (isConfiguredKey(item)) return true;
        }
        return false;
    }

    private void consumeConfiguredKey(Player player) {
        org.bukkit.inventory.PlayerInventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getSize(); slot++) {
            org.bukkit.inventory.ItemStack item = inventory.getItem(slot);
            if (!isConfiguredKey(item)) continue;
            int amount = item.getAmount();
            if (amount <= 1) inventory.setItem(slot, null);
            else item.setAmount(amount - 1);
            return;
        }
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
