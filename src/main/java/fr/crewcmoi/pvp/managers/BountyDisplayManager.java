package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.MoneyFormat;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BountyDisplayManager {

    
    
    private static final String DISPLAY_TAG = "crewcmoi_bounty_display";

    private final Main plugin;

    private final Map<UUID, TextDisplay> displays = new ConcurrentHashMap<>();

    
    private final Map<UUID, Double> lastAmounts = new ConcurrentHashMap<>();

    private BukkitTask followTask;

    public BountyDisplayManager(Main plugin) {
        this.plugin = plugin;
    }

    public void start() {

        
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntitiesByClass(TextDisplay.class)) {
                if (entity.getScoreboardTags().contains(DISPLAY_TAG)) {
                    entity.remove();
                }
            }
        }

        if (followTask != null) {
            return;
        }
        int intervalTicks = Math.max(1, plugin.getConfig().getInt("bounty.display.follow-interval-ticks", 2));
        followTask = Bukkit.getScheduler().runTaskTimer(plugin, this::followTick, 0L, intervalTicks);
    }

    public void stop() {
        if (followTask != null) {
            followTask.cancel();
            followTask = null;
        }
        for (TextDisplay display : displays.values()) {
            if (display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
        lastAmounts.clear();
    }

    private void followTick() {
        for (Map.Entry<UUID, TextDisplay> entry : displays.entrySet()) {
            Player player = Bukkit.getPlayer(entry.getKey());
            TextDisplay display = entry.getValue();
            if (player == null || !player.isOnline() || !display.isValid()) {
                continue;
            }
            display.teleport(offsetLocation(player));
        }
    }

    private Location offsetLocation(Player player) {
        double heightOffset = plugin.getConfig().getDouble("bounty.display.height-offset", 0.35);
        return player.getEyeLocation().add(0, heightOffset, 0);
    }

    public void update(UUID playerUuid, double total) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player == null || !player.isOnline()) {
            return;
        }

        if (total <= 0) {
            remove(playerUuid);
            return;
        }

        Double last = lastAmounts.get(playerUuid);
        TextDisplay display = displays.get(playerUuid);

        if (display == null || !display.isValid()) {
            display = player.getWorld().spawn(offsetLocation(player), TextDisplay.class, entity -> {
                entity.addScoreboardTag(DISPLAY_TAG);
                entity.setBillboard(Display.Billboard.CENTER);
                entity.setPersistent(false);
                entity.setShadowed(true);
                entity.setSeeThrough(false);
                entity.setBackgroundColor(org.bukkit.Color.fromARGB(0, 0, 0, 0));
                entity.setAlignment(TextDisplay.TextAlignment.CENTER);
            });
            displays.put(playerUuid, display);
            last = null;
        }

        if (last == null || last != total) {
            display.text(Component.text(MoneyFormat.format(total), NamedTextColor.GOLD));
            lastAmounts.put(playerUuid, total);
        }
    }

    public void remove(UUID playerUuid) {
        TextDisplay display = displays.remove(playerUuid);
        lastAmounts.remove(playerUuid);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
}
