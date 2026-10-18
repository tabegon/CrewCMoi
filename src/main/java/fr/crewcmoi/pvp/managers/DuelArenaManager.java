package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.other.utils.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class DuelArenaManager {
    private static final int BLOCKS_PER_TICK = 4000;
    private final Main plugin;
    private final File arenasFile;
    private final Arena[] arenas = {null, new Arena(1), new Arena(2)};
    private int activeArena = 1;

    private static final class Arena {
        final int id;
        String worldName;
        int minX, minY, minZ, maxX, maxY, maxZ;
        String[] savedBlocks;
        Location corner1, corner2;
        Arena(int id) {
            this.id = id;
        }
    }

    public DuelArenaManager(Main plugin) {
        this.plugin = plugin;
        this.arenasFile = new File(plugin.getDataFolder(), "arenas.yml");
        Path legacy = plugin.getDataFolder().toPath().resolve("duel-arena.snapshot");
        Path first = snapshotPath(arenas[1]);
        if (Files.exists(legacy) && !Files.exists(first)) {
            try { Files.copy(legacy, first); }
            catch (IOException exception) { plugin.getLogger().warning("Impossible de migrer l'ancienne sauvegarde de l'arène : " + exception.getMessage()); }
        }
        loadArena(arenas[1]);
        loadArena(arenas[2]);
    }

    public void setActiveArena(int id) { if (id == 1 || id == 2) activeArena = id; }
    public int getActiveArena() { return activeArena; }
    private Arena active() { return arenas[activeArena]; }

    private void loadArena(Arena arena) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(arenasFile);
        String base = "arenas." + arena.id;
        if (yaml.contains(base + ".world")) {
            arena.worldName = yaml.getString(base + ".world");
            arena.minX = yaml.getInt(base + ".min-x"); arena.minY = yaml.getInt(base + ".min-y"); arena.minZ = yaml.getInt(base + ".min-z");
            arena.maxX = yaml.getInt(base + ".max-x"); arena.maxY = yaml.getInt(base + ".max-y"); arena.maxZ = yaml.getInt(base + ".max-z");
        } else if (arena.id == 1 && plugin.getConfig().contains("duel.arena.region.world")) {
            arena.worldName = plugin.getConfig().getString("duel.arena.region.world");
            arena.minX = plugin.getConfig().getInt("duel.arena.region.minX"); arena.minY = plugin.getConfig().getInt("duel.arena.region.minY"); arena.minZ = plugin.getConfig().getInt("duel.arena.region.minZ");
            arena.maxX = plugin.getConfig().getInt("duel.arena.region.maxX"); arena.maxY = plugin.getConfig().getInt("duel.arena.region.maxY"); arena.maxZ = plugin.getConfig().getInt("duel.arena.region.maxZ");
        }
        loadSnapshot(arena);
    }

    public void setCorner(int index, Location location) {
        Arena arena = active();
        if (index == 1) arena.corner1 = location.getBlock().getLocation(); else arena.corner2 = location.getBlock().getLocation();
        if (arena.corner1 == null || arena.corner2 == null || arena.corner1.getWorld() == null || !arena.corner1.getWorld().equals(arena.corner2.getWorld())) return;
        arena.worldName = arena.corner1.getWorld().getName();
        arena.minX = Math.min(arena.corner1.getBlockX(), arena.corner2.getBlockX()); arena.minY = Math.min(arena.corner1.getBlockY(), arena.corner2.getBlockY()); arena.minZ = Math.min(arena.corner1.getBlockZ(), arena.corner2.getBlockZ());
        arena.maxX = Math.max(arena.corner1.getBlockX(), arena.corner2.getBlockX()); arena.maxY = Math.max(arena.corner1.getBlockY(), arena.corner2.getBlockY()); arena.maxZ = Math.max(arena.corner1.getBlockZ(), arena.corner2.getBlockZ());
        saveRegion(arena);
        arena.savedBlocks = null;
        try { Files.deleteIfExists(snapshotPath(arena)); } catch (IOException ignored) { }
    }

    private void saveRegion(Arena arena) {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(arenasFile);
        String base = "arenas." + arena.id;
        yaml.set(base + ".world", arena.worldName); yaml.set(base + ".min-x", arena.minX); yaml.set(base + ".min-y", arena.minY); yaml.set(base + ".min-z", arena.minZ);
        yaml.set(base + ".max-x", arena.maxX); yaml.set(base + ".max-y", arena.maxY); yaml.set(base + ".max-z", arena.maxZ);
        try { plugin.getDataFolder().mkdirs(); yaml.save(arenasFile); }
        catch (IOException exception) { plugin.getLogger().warning("Impossible de sauvegarder arenas.yml : " + exception.getMessage()); }
    }

    private Path snapshotPath(Arena arena) { return plugin.getDataFolder().toPath().resolve("arena-" + arena.id + ".snapshot"); }
    private boolean defined(Arena a) { return a.worldName != null && !a.worldName.isBlank(); }
    public boolean isRegionDefined() { return defined(active()); }
    public boolean hasSnapshot() { return active().savedBlocks != null; }

    public boolean isInsideArena(Location location) {
        if (location == null || location.getWorld() == null) return false;
        for (int id = 1; id <= 2; id++) {
            Arena a = arenas[id];
            if (!defined(a) || !location.getWorld().getName().equals(a.worldName)) continue;
            int x = location.getBlockX(), y = location.getBlockY(), z = location.getBlockZ();
            if (x >= a.minX && x <= a.maxX && y >= a.minY && y <= a.maxY && z >= a.minZ && z <= a.maxZ) return true;
        }
        return false;
    }

    public boolean saveSnapshot(CommandSender sender) {
        Arena a = active();
        if (!defined(a)) { Messages.send(sender, "moderation.duelarena.corners-required"); return false; }
        World world = Bukkit.getWorld(a.worldName);
        if (world == null) { Messages.send(sender, "moderation.duelarena.world-not-loaded", java.util.Map.of("world", a.worldName)); return false; }
        int sx = a.maxX-a.minX+1, sy = a.maxY-a.minY+1, sz = a.maxZ-a.minZ+1;
        List<String> lines = new ArrayList<>(Math.max(0, sx*sy*sz));
        for (int x=a.minX; x<=a.maxX; x++) for (int y=a.minY; y<=a.maxY; y++) for (int z=a.minZ; z<=a.maxZ; z++) lines.add(world.getBlockAt(x,y,z).getBlockData().getAsString());
        a.savedBlocks = lines.toArray(new String[0]);
        try {
            Path file = snapshotPath(a); Files.createDirectories(file.getParent());
            List<String> content = new ArrayList<>(lines.size()+2); content.add(a.worldName); content.add(a.minX+" "+a.minY+" "+a.minZ+" "+a.maxX+" "+a.maxY+" "+a.maxZ); content.addAll(lines);
            Files.write(file, content, StandardCharsets.UTF_8);
            Messages.send(sender, "moderation.duelarena.saved", java.util.Map.of("blocks", a.savedBlocks.length));
            sender.sendMessage("§7Sauvegarde enregistrée dans arena-" + a.id + ".snapshot");
            return true;
        } catch (IOException exception) { plugin.getLogger().warning("Impossible de sauvegarder arena-"+a.id+".snapshot : "+exception.getMessage()); sender.sendMessage("§cErreur lors de la sauvegarde de l'arène."); return false; }
    }

    private void loadSnapshot(Arena a) {
        Path file = snapshotPath(a);
        if (!Files.exists(file)) return;
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (lines.size()<2 || !defined(a) || !lines.get(0).equals(a.worldName)) return;
            String[] b=lines.get(1).split(" "); if (b.length!=6) return;
            int[] bounds=new int[6]; for(int i=0;i<6;i++) bounds[i]=Integer.parseInt(b[i]);
            if(bounds[0]!=a.minX||bounds[1]!=a.minY||bounds[2]!=a.minZ||bounds[3]!=a.maxX||bounds[4]!=a.maxY||bounds[5]!=a.maxZ) return;
            a.savedBlocks=lines.subList(2,lines.size()).toArray(new String[0]);
        } catch(IOException|NumberFormatException exception) { plugin.getLogger().warning("Impossible de charger arena-"+a.id+".snapshot : "+exception.getMessage()); }
    }

    public void resetArena() { resetArena(activeArena); }

    public void resetArena(int arenaId) {
        if (arenaId < 1 || arenaId > 2) return;
        Arena a = arenas[arenaId];
        if (!defined(a) || a.savedBlocks == null) return;
        World world = Bukkit.getWorld(a.worldName); if (world == null) return;
        int sy=a.maxY-a.minY+1, sz=a.maxZ-a.minZ+1; int[] cursor={0}; BukkitTask[] task={null};
        task[0]=Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            int done=0;
            while(done<BLOCKS_PER_TICK && cursor[0]<a.savedBlocks.length) {
                int i=cursor[0]; int x=a.minX+i/(sy*sz); int y=a.minY+(i/sz)%sy; int z=a.minZ+i%sz;
                BlockData data=Bukkit.createBlockData(a.savedBlocks[i]); world.getBlockAt(x,y,z).setBlockData(data,false); cursor[0]++; done++;
            }
            if(cursor[0]>=a.savedBlocks.length) task[0].cancel();
        },1L,1L);
    }
}
