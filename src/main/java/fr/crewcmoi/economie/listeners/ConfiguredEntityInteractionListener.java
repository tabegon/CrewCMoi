package fr.crewcmoi.economie.listeners;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class ConfiguredEntityInteractionListener implements Listener {
    private final Main plugin;
    private final Random random = new Random();
    private final Map<UUID, TextDisplay> displays = new HashMap<>();
    private final Map<UUID, Long> teleportCooldowns = new HashMap<>();
    private BukkitTask displayTask;

    public ConfiguredEntityInteractionListener(Main plugin) {
        this.plugin = plugin;
        this.displayTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateDisplays, 10L, 10L);
        logConfiguration("random-teleport");
        logConfiguration("rules-display");
    }

    private void logConfiguration(String interaction) {
        String path = "npcs.entity-interactions." + interaction + ".";
        plugin.getLogger().info("Interaction PNJ '" + interaction + "' : UUID configuré="
                + plugin.getConfig().getString(path + "uuid", "<absent>")
                + ", monde=" + plugin.getConfig().getString(path + "world", "world"));
    }

    /** Clic droit sur une entité quelconque (Interaction, armor stand, villageois...) identifiée par son UUID. */
    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Entity entity = event.getRightClicked();
        // Les PNJ Citizens sont traités par ConfiguredNpcInteractionListener (évite un double déclenchement).
        if (entity.hasMetadata("NPC")) return;
        if (handleInteraction(event.getPlayer(), null, entity.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Déclenche le RTP ou l'affichage des règles si l'un des UUID correspond à la config.
     *
     * @param citizensUuid UUID Citizens du PNJ, ou null si ce n'est pas un PNJ
     * @return true si le clic correspondait à une interaction configurée
     */
    public boolean handleInteraction(Player player, UUID citizensUuid, UUID entityUuid) {
        String teleportUuid = plugin.getConfig().getString("npcs.entity-interactions.random-teleport.uuid", "039089ae-ccb1-4bc5-8082-047c1278b12e");
        if (matchesUuid(teleportUuid, citizensUuid, entityUuid)) {
            randomTeleport(player);
            return true;
        }

        String rulesUuid = plugin.getConfig().getString("npcs.entity-interactions.rules-display.uuid", "6e0df616-acc1-44e9-bfef-73ebcf678ec1");
        if (matchesUuid(rulesUuid, citizensUuid, entityUuid)) {
            showRules(player);
            return true;
        }

        plugin.getLogger().fine("Clic non associé à une interaction configurée : UUID Citizens=" + citizensUuid
                + ", UUID entité=" + entityUuid);
        return false;
    }

    private boolean matchesUuid(String configuredUuid, UUID citizensUuid, UUID entityUuid) {
        if (configuredUuid == null || configuredUuid.isBlank()) return false;
        String configured = configuredUuid.trim();
        return (citizensUuid != null && configured.equalsIgnoreCase(citizensUuid.toString()))
                || (entityUuid != null && configured.equalsIgnoreCase(entityUuid.toString()));
    }

    private void randomTeleport(Player player) {
        String path = "npcs.entity-interactions.random-teleport.";
        long now = System.currentTimeMillis();
        long cooldown = plugin.getConfig().getLong(path + "cooldown-seconds", 3L) * 1000L;
        long last = teleportCooldowns.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < cooldown) return;
        teleportCooldowns.put(player.getUniqueId(), now);

        String worldName = plugin.getConfig().getString(path + "world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Le monde de téléportation est indisponible.");
            return;
        }

        int sizeX = Math.max(1, plugin.getConfig().getInt(path + "size-x", 5000));
        int sizeZ = Math.max(1, plugin.getConfig().getInt(path + "size-z", 5000));
        int centerX = plugin.getConfig().getInt(path + "center-x", 0);
        int centerZ = plugin.getConfig().getInt(path + "center-z", 0);
        int minY = plugin.getConfig().getInt(path + "min-y", world.getMinHeight());
        int attempts = Math.max(1, plugin.getConfig().getInt(path + "max-attempts", 40));

        for (int i = 0; i < attempts; i++) {
            int x = centerX + random.nextInt(sizeX) - sizeX / 2;
            int z = centerZ + random.nextInt(sizeZ) - sizeZ / 2;
            int y = world.getHighestBlockYAt(x, z);
            if (y < minY || y + 2 >= world.getMaxHeight()) continue;
            if (!world.getBlockAt(x, y, z).getType().isSolid()) continue;
            if (!world.getBlockAt(x, y + 1, z).isPassable() || !world.getBlockAt(x, y + 2, z).isPassable()) continue;
            Location destination = new Location(world, x + 0.5, y + 1.0, z + 0.5, player.getLocation().getYaw(), player.getLocation().getPitch());
            player.teleport(destination);
            player.sendMessage(ChatColor.GREEN + "Téléportation aléatoire effectuée.");
            return;
        }
        player.sendMessage(ChatColor.RED + "Aucun emplacement sûr n'a été trouvé. Réessaie.");
    }

    private void showRules(Player player) {
        String path = "npcs.entity-interactions.rules-display.";
        World world = Bukkit.getWorld(plugin.getConfig().getString(path + "world", "world"));
        if (world == null) {
            player.sendMessage(ChatColor.RED + "Le monde des règles est indisponible.");
            return;
        }
        TextDisplay previous = displays.remove(player.getUniqueId());
        if (previous != null && previous.isValid()) previous.remove();

        Location location = new Location(world,
                plugin.getConfig().getDouble(path + "x", -42.5),
                plugin.getConfig().getDouble(path + "y", 73.50),
                plugin.getConfig().getDouble(path + "z", -83.5));
        TextDisplay display = world.spawn(location, TextDisplay.class, textDisplay -> {
            String text = String.join("\n", plugin.getConfig().getStringList(path + "text").stream()
                    .map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());
            textDisplay.setText(text.isEmpty() ? ChatColor.GOLD + "RÈGLES DU SERVEUR" : text);
            textDisplay.setBillboard(TextDisplay.Billboard.FIXED);
            textDisplay.setLineWidth(Math.max(100, plugin.getConfig().getInt(path + "line-width", 280)));
            textDisplay.setSeeThrough(true);
            textDisplay.setShadowed(true);
            textDisplay.setDefaultBackground(false);
            textDisplay.setPersistent(false);
        });
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.hideEntity(plugin, display);
        }
        player.showEntity(plugin, display);
        displays.put(player.getUniqueId(), display);
    }

    private void updateDisplays() {
        double maxDistance = Math.max(1.0, plugin.getConfig().getDouble("npcs.entity-interactions.rules-display.hide-distance", 15.0));
        displays.entrySet().removeIf(entry -> {
            Player player = Bukkit.getPlayer(entry.getKey());
            TextDisplay display = entry.getValue();
            if (player == null || !player.isOnline() || !display.isValid()
                    || !player.getWorld().equals(display.getWorld())
                    || player.getLocation().distanceSquared(display.getLocation()) > maxDistance * maxDistance) {
                if (display.isValid()) display.remove();
                return true;
            }
            return false;
        });
    }

    public void shutdown() {
        if (displayTask != null) displayTask.cancel();
        displays.values().forEach(display -> { if (display.isValid()) display.remove(); });
        displays.clear();
    }
}
