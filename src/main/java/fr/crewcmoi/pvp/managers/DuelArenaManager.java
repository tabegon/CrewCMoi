package fr.crewcmoi.pvp.managers;
import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitTask;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DuelArenaManager {

    
    private static final int BLOCKS_PER_TICK = 4000;

    private final Main plugin;
    private final Path snapshotFile;

    private String worldName;
    private int minX, minY, minZ, maxX, maxY, maxZ;
    private String[] savedBlocks; 

    public DuelArenaManager(Main plugin) {
        this.plugin = plugin;
        this.snapshotFile = plugin.getDataFolder().toPath().resolve("duel-arena.snapshot");
        loadCorners();
        loadSnapshotFromDisk();
    }

    

    private void loadCorners() {
        this.worldName = plugin.getConfig().getString("duel.arena.region.world");
        this.minX = plugin.getConfig().getInt("duel.arena.region.minX");
        this.minY = plugin.getConfig().getInt("duel.arena.region.minY");
        this.minZ = plugin.getConfig().getInt("duel.arena.region.minZ");
        this.maxX = plugin.getConfig().getInt("duel.arena.region.maxX");
        this.maxY = plugin.getConfig().getInt("duel.arena.region.maxY");
        this.maxZ = plugin.getConfig().getInt("duel.arena.region.maxZ");
    }

    private Location corner1;
    private Location corner2;

    public void setCorner(int index, Location location) {
        if (index == 1) {
            corner1 = location.getBlock().getLocation();
        } else {
            corner2 = location.getBlock().getLocation();
        }

        if (corner1 == null || corner2 == null) {
            return;
        }
        if (corner1.getWorld() == null || !corner1.getWorld().equals(corner2.getWorld())) {
            return;
        }

        this.worldName = corner1.getWorld().getName();
        this.minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        this.minY = Math.min(corner1.getBlockY(), corner2.getBlockY());
        this.minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        this.maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        this.maxY = Math.max(corner1.getBlockY(), corner2.getBlockY());
        this.maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());

        plugin.getConfig().set("duel.arena.region.world", worldName);
        plugin.getConfig().set("duel.arena.region.minX", minX);
        plugin.getConfig().set("duel.arena.region.minY", minY);
        plugin.getConfig().set("duel.arena.region.minZ", minZ);
        plugin.getConfig().set("duel.arena.region.maxX", maxX);
        plugin.getConfig().set("duel.arena.region.maxY", maxY);
        plugin.getConfig().set("duel.arena.region.maxZ", maxZ);
        plugin.saveConfig();

        savedBlocks = null;
        try {
            Files.deleteIfExists(snapshotFile);
        } catch (IOException ignored) {
            
        }
    }

    public boolean isRegionDefined() {
        return worldName != null;
    }

    public boolean isInsideArena(Location location) {
        if (!isRegionDefined() || location.getWorld() == null
                || !location.getWorld().getName().equals(worldName)) {
            return false;
        }
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    

    public boolean hasSnapshot() {
        return savedBlocks != null;
    }

    public boolean saveSnapshot(CommandSender sender) {
        if (!isRegionDefined()) {
            Messages.send(sender, "moderation.duelarena.corners-required");
            return false;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Messages.send(sender, "moderation.duelarena.world-not-loaded", java.util.Map.of("world", worldName));
            return false;
        }

        int sizeX = maxX - minX + 1;
        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;
        List<String> lines = new ArrayList<>(sizeX * sizeY * sizeZ);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    lines.add(world.getBlockAt(x, y, z).getBlockData().getAsString());
                }
            }
        }

        savedBlocks = lines.toArray(new String[0]);
        writeSnapshotToDisk(lines);

        Messages.send(sender, "moderation.duelarena.saved", java.util.Map.of("blocks", savedBlocks.length));
        return true;
    }

    private void writeSnapshotToDisk(List<String> lines) {
        try {
            Files.createDirectories(snapshotFile.getParent());
            List<String> content = new ArrayList<>(lines.size() + 2);
            content.add(worldName);
            content.add(minX + " " + minY + " " + minZ + " " + maxX + " " + maxY + " " + maxZ);
            content.addAll(lines);
            Files.write(snapshotFile, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            plugin.getLogger().warning("Impossible d'écrire la sauvegarde de l'arène de duel : " + exception.getMessage());
        }
    }

    private void loadSnapshotFromDisk() {
        if (!Files.exists(snapshotFile)) {
            return;
        }
        try {
            List<String> content = Files.readAllLines(snapshotFile, StandardCharsets.UTF_8);
            if (content.size() < 2) {
                return;
            }
            String savedWorld = content.get(0);
            String[] bounds = content.get(1).split(" ");
            if (bounds.length != 6 || !savedWorld.equals(worldName)) {
                
                return;
            }
            int savedMinX = Integer.parseInt(bounds[0]);
            int savedMinY = Integer.parseInt(bounds[1]);
            int savedMinZ = Integer.parseInt(bounds[2]);
            int savedMaxX = Integer.parseInt(bounds[3]);
            int savedMaxY = Integer.parseInt(bounds[4]);
            int savedMaxZ = Integer.parseInt(bounds[5]);
            if (savedMinX != minX || savedMinY != minY || savedMinZ != minZ
                    || savedMaxX != maxX || savedMaxY != maxY || savedMaxZ != maxZ) {
                return;
            }
            savedBlocks = content.subList(2, content.size()).toArray(new String[0]);
        } catch (IOException | NumberFormatException exception) {
            plugin.getLogger().warning("Impossible de charger la sauvegarde de l'arène de duel : " + exception.getMessage());
        }
    }

    

    

    public void resetArena() {
        if (!isRegionDefined() || savedBlocks == null) {
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return;
        }

        int sizeY = maxY - minY + 1;
        int sizeZ = maxZ - minZ + 1;

        int[] cursor = {0}; 
        BukkitTask[] taskHolder = new BukkitTask[1];
        taskHolder[0] = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int processed = 0;
            while (processed < BLOCKS_PER_TICK && cursor[0] < savedBlocks.length) {
                int index = cursor[0];
                int x = minX + index / (sizeY * sizeZ);
                int y = minY + (index / sizeZ) % sizeY;
                int z = minZ + index % sizeZ;

                BlockData data = Bukkit.createBlockData(savedBlocks[index]);
                world.getBlockAt(x, y, z).setBlockData(data, false);

                cursor[0]++;
                processed++;
            }
            if (cursor[0] >= savedBlocks.length) {
                taskHolder[0].cancel();
            }
        }, 1L, 1L);
    }
}
