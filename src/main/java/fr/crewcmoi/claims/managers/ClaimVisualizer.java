package fr.crewcmoi.claims.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClaimVisualizer {
    private final Main plugin;
    private final ClaimManager claimManager;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public ClaimVisualizer(Main plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    public boolean toggle(Player player) {
        BukkitTask existing = activeTasks.remove(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
            return false;
        }
        int radius = plugin.getConfig().getInt("claims.see-radius", 5);
        int durationSeconds = plugin.getConfig().getInt("claims.see-duration", 10);
        long maxTicks = Math.max(20L, durationSeconds * 20L);
        final BukkitTask[] taskRef = new BukkitTask[1];
        taskRef[0] = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            long elapsed;
            @Override public void run() {
                if (!player.isOnline() || elapsed >= maxTicks) {
                    BukkitTask self = activeTasks.remove(player.getUniqueId());
                    if (self != null) self.cancel();
                    return;
                }
                showClaims(player, radius);
                elapsed += 20;
            }
        }, 0L, 20L);
        activeTasks.put(player.getUniqueId(), taskRef[0]);
        return true;
    }

    public void stop(Player player) {
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    private void showClaims(Player player, int radius) {
        List<ClaimData> nearby = claimManager.getNearbyClaims(player.getLocation(), radius);
        double baseY = player.getLocation().getY();
        for (ClaimData claim : nearby) {
            Color color = claim.getOwnerUuid().equals(player.getUniqueId()) ? Color.LIME
                    : claim.getTrusted().contains(player.getUniqueId()) ? Color.YELLOW : Color.RED;
            drawChunkCorners(player, claim.getChunkX(), claim.getChunkZ(), baseY, color);
        }
    }

    private void drawChunkCorners(Player player, int chunkX, int chunkZ, double baseY, Color color) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 16;
        int maxZ = minZ + 16;
        Particle.DustOptions dust = new Particle.DustOptions(color, 1.2f);
        int[][] corners = {{minX, minZ}, {maxX, minZ}, {minX, maxZ}, {maxX, maxZ}};
        for (int[] corner : corners) {
            for (double dy = 0; dy <= 3; dy += 0.5) {
                player.spawnParticle(Particle.DUST, new Location(player.getWorld(), corner[0], baseY + dy, corner[1]), 1, 0, 0, 0, 0, dust);
            }
        }
        double[] edgeHeights = {baseY + 0.2, baseY + 1.6};
        for (double edgeY : edgeHeights) {
            for (int offset = 0; offset <= 16; offset += 2) {
                player.spawnParticle(Particle.DUST, new Location(player.getWorld(), minX + offset, edgeY, minZ), 1, 0, 0, 0, 0, dust);
                player.spawnParticle(Particle.DUST, new Location(player.getWorld(), minX + offset, edgeY, maxZ), 1, 0, 0, 0, 0, dust);
                player.spawnParticle(Particle.DUST, new Location(player.getWorld(), minX, edgeY, minZ + offset), 1, 0, 0, 0, 0, dust);
                player.spawnParticle(Particle.DUST, new Location(player.getWorld(), maxX, edgeY, minZ + offset), 1, 0, 0, 0, 0, dust);
            }
        }
    }
}
