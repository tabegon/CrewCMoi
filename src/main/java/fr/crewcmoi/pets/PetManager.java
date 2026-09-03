package fr.crewcmoi.pets;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PetManager {

    private final Main plugin;
    private final File file;
    private FileConfiguration data;
    private final Map<UUID, UUID> activePets = new HashMap<>();
    private BukkitTask followTask;

    public PetManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "pets.yml");
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Impossible de créer pets.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(file);
        // Les entités MythicMobs ne survivent pas au redémarrage du serveur :
        // aucun pet ne doit donc rester affiché comme actif après un restart.
        clearPersistedActivePets();
        startFollowTask();
    }

    public void shutdown() {
        if (followTask != null) followTask.cancel();
        for (UUID playerId : new HashSet<>(activePets.keySet())) {
            deactivate(Bukkit.getPlayer(playerId), false);
        }
        save();
    }

    public Set<String> getConfiguredPets() {
        ConfigurationSectionWrapper wrapper = new ConfigurationSectionWrapper(plugin.getConfig());
        return wrapper.getKeys();
    }

    public String getItemId(String petId) {
        return plugin.getConfig().getString("pets.list." + petId + ".itemsadder-id", "");
    }

    public String getMythicMob(String petId) {
        return plugin.getConfig().getString("pets.list." + petId + ".mythic-mob", petId);
    }

    public int getSlot(String petId, int index) {
        return plugin.getConfig().getInt("pets.list." + petId + ".slot", 10 + index);
    }

    public boolean owns(UUID playerId, String petId) {
        return data.getBoolean("players." + playerId + ".pets." + petId, false);
    }

    public void grant(UUID playerId, String petId) {
        data.set("players." + playerId + ".pets." + petId, true);
        save();
    }

    public String getActivePet(UUID playerId) {
        return data.getString("players." + playerId + ".active", null);
    }

    public void setActivePet(UUID playerId, String petId) {
        data.set("players." + playerId + ".active", petId);
        save();
    }

    public void clearActivePet(UUID playerId) {
        data.set("players." + playerId + ".active", null);
        save();
    }

    public UUID getActiveEntity(UUID playerId) {
        return activePets.get(playerId);
    }

    public ItemStack createPetItem(String petId) {
        String id = getItemId(petId);
        if (id.isBlank()) return null;
        CustomStack custom = CustomStack.getInstance(id);
        if (custom == null) return null;
        return custom.getItemStack().clone();
    }

    public boolean isPetItem(ItemStack stack, String petId) {
        if (stack == null || stack.getType().isAir()) return false;
        String id = getItemId(petId);
        if (id.isBlank()) return false;
        CustomStack custom = CustomStack.byItemStack(stack);
        return custom != null && id.equalsIgnoreCase(custom.getNamespacedID());
    }

    public void toggle(Player player, String petId) {
        if (!owns(player.getUniqueId(), petId)) return;

        String active = getActivePet(player.getUniqueId());
        if (petId.equalsIgnoreCase(active)) {
            deactivate(player, true);
            return;
        }

        if (active != null) {
            deactivate(player, false);
        }
        summon(player, petId);
    }

    private void summon(Player player, String petId) {
        String mythicMob = getMythicMob(petId);
        if (mythicMob.isBlank()) {
            player.sendMessage("§cLe MythicMob du pet §e" + petId + "§c n'est pas configuré.");
            return;
        }

        Location loc = petLocation(player);
        Set<UUID> before = new HashSet<>();
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 4, 4, 4)) {
            before.add(entity.getUniqueId());
        }

        String world = loc.getWorld().getName();
        String command = "mm mobs spawn -s " + mythicMob + ":1 1 " +
                world + "," + loc.getX() + "," + loc.getY() + "," + loc.getZ();
        boolean dispatched = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
        if (!dispatched) {
            player.sendMessage("§cImpossible d'invoquer le pet.");
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Entity found = null;
            double best = Double.MAX_VALUE;
            for (Entity entity : loc.getWorld().getNearbyEntities(loc, 5, 5, 5)) {
                if (before.contains(entity.getUniqueId())) continue;
                double distance = entity.getLocation().distanceSquared(loc);
                if (distance < best && entity.isValid()) {
                    best = distance;
                    found = entity;
                }
            }

            if (found == null) {
                player.sendMessage("§cMythicMobs n'a pas réussi à invoquer le pet §e" + petId + "§c.");
                return;
            }

            activePets.put(player.getUniqueId(), found.getUniqueId());
            setActivePet(player.getUniqueId(), petId);
            found.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "crew_pet_owner"),
                    PersistentDataType.STRING,
                    player.getUniqueId().toString()
            );
            player.sendMessage("§aPet §e" + petId + " §aactivé !");
        }, 1L);
    }

    public void deactivate(Player player, boolean message) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        UUID entityId = activePets.remove(playerId);
        if (entityId != null) {
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) entity.remove();
        }
        clearActivePet(playerId);
        if (message) player.sendMessage("§cPet désactivé.");
    }

    public void handleQuit(Player player) {
        deactivate(player, false);
    }

    private void startFollowTask() {
        followTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, UUID> entry : new HashMap<>(activePets).entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                Entity pet = Bukkit.getEntity(entry.getValue());
                if (player == null || !player.isOnline() || pet == null || !pet.isValid()) {
                    if (player != null) clearActivePet(player.getUniqueId());
                    activePets.remove(entry.getKey());
                    continue;
                }
                if (!pet.getWorld().equals(player.getWorld())) {
                    pet.teleport(petLocation(player));
                    continue;
                }
                Location target = petLocation(player);
                if (pet.getLocation().distanceSquared(target) > 0.25) {
                    pet.teleport(target);
                }
            }
        }, 1L, 2L);
    }

    private Location petLocation(Player player) {
        Location p = player.getLocation().clone();
        org.bukkit.util.Vector dir = p.getDirection().setY(0);
        if (dir.lengthSquared() < 0.01) dir = new org.bukkit.util.Vector(0, 0, 1);
        dir.normalize();
        org.bukkit.util.Vector right = new org.bukkit.util.Vector(dir.getZ(), 0, -dir.getX()).multiply(1.35);
        return p.add(right).add(0, 0.15, 0);
    }


    private void clearPersistedActivePets() {
        org.bukkit.configuration.ConfigurationSection players = data.getConfigurationSection("players");
        if (players == null) return;
        for (String playerId : players.getKeys(false)) {
            data.set("players." + playerId + ".active", null);
        }
        save();
    }

    private void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Impossible de sauvegarder pets.yml: " + e.getMessage());
        }
    }

    private static class ConfigurationSectionWrapper {
        private final FileConfiguration config;
        ConfigurationSectionWrapper(FileConfiguration config) { this.config = config; }
        Set<String> getKeys() {
            org.bukkit.configuration.ConfigurationSection section = config.getConfigurationSection("pets.list");
            return section == null ? Collections.emptySet() : section.getKeys(false);
        }
    }
}
