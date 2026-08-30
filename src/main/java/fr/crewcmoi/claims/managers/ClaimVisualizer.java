package fr.crewcmoi.claims.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère l'affichage (via particules) des bordures des claims aux alentours d'un joueur,
 * déclenché par /claim see. Vert = vos claims, jaune = claims où vous êtes de confiance,
 * rouge = claims d'autres joueurs.
 */
public class ClaimVisualizer {

    private final Main plugin;
    private final ClaimManager claimManager;
    private final Map<UUID, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public ClaimVisualizer(Main plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    /**
     * Active ou désactive l'affichage des claims pour ce joueur.
     * Retourne true si l'affichage vient d'être activé, false s'il vient d'être désactivé.
     */
    public boolean toggle(Player player) {
        BukkitTask existing = activeTasks.remove(player.getUniqueId());
        if (existing != null) {
            existing.cancel();
            return false;
        }

        int radius = plugin.getConfig().getInt("claims.see-radius", 5);
        int durationSeconds = plugin.getConfig().getInt("claims.see-duration", 10);
        long maxTicks = durationSeconds * 20L;

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            long elapsed = 0;

            @Override
            public void run() {
                if (!player.isOnline() || elapsed >= maxTicks) {
                    BukkitTask self = activeTasks.remove(player.getUniqueId());
                    if (self != null) {
                        self.cancel();
                    }
                    return;
                }
                showClaims(player, radius);
                elapsed += 10;
            }
        }, 0L, 10L);

        activeTasks.put(player.getUniqueId(), task);
        return true;
    }

    public void stop(Player player) {
        BukkitTask task = activeTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    private void showClaims(Player player, int radius) {
        List<ClaimData> nearby = claimManager.getNearbyClaims(player.getLocation(), radius);
        World world = player.getWorld();
        double baseY = player.getLocation().getY();

        for (ClaimData claim : nearby) {
            Color color;
            if (claim.getOwnerUuid().equals(player.getUniqueId())) {
                color = Color.LIME;
            } else if (claim.getTrusted().contains(player.getUniqueId())) {
                color = Color.YELLOW;
            } else {
                color = Color.RED;
            }
            drawChunkCorners(world, claim.getChunkX(), claim.getChunkZ(), baseY, color);
        }
    }

    private void drawChunkCorners(World world, int chunkX, int chunkZ, double baseY, Color color) {
        int minX = chunkX << 4;
        int minZ = chunkZ << 4;
        int maxX = minX + 16;
        int maxZ = minZ + 16;

        Particle.DustOptions dust = new Particle.DustOptions(color, 1.4f);

        // Coins verticaux : une colonne de particules à chaque coin du chunk, du sol jusqu'à
        // bien au-dessus de la tête, pour bien repérer les angles même de loin.
        int[][] corners = {
                {minX, minZ},
                {maxX, minZ},
                {minX, maxZ},
                {maxX, maxZ}
        };
        for (int[] corner : corners) {
            for (double dy = -1; dy <= 4; dy += 0.15) {
                Location loc = new Location(world, corner[0], baseY + dy, corner[1]);
                world.spawnParticle(Particle.DUST, loc, 2, 0.05, 0.05, 0.05, 0, dust);
            }
        }

        // Bordures horizontales : les 4 arêtes du chunk, dessinées à deux hauteurs (sol et
        // niveau des yeux) pour visualiser l'intégralité du contour, pas seulement ses coins.
        double step = 0.25;
        double[] edgeHeights = {baseY + 0.2, baseY + 1.6};
        for (double edgeY : edgeHeights) {
            for (double x = minX; x <= maxX; x += step) {
                spawnEdgeParticle(world, x, edgeY, minZ, dust);
                spawnEdgeParticle(world, x, edgeY, maxZ, dust);
            }
            for (double z = minZ; z <= maxZ; z += step) {
                spawnEdgeParticle(world, minX, edgeY, z, dust);
                spawnEdgeParticle(world, maxX, edgeY, z, dust);
            }
        }
    }

    private void spawnEdgeParticle(World world, double x, double y, double z, Particle.DustOptions dust) {
        world.spawnParticle(Particle.DUST, new Location(world, x, y, z), 1, 0, 0, 0, 0, dust);
    }
}
