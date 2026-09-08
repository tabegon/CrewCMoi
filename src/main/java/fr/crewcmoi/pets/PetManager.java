package fr.crewcmoi.pets;
import fr.crewcmoi.other.utils.Messages;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PetManager {

    private static final double FOLLOW_DISTANCE_SQUARED = 2.25; 
    private static final double TELEPORT_DISTANCE_SQUARED = 400; 
    private static final double MAX_TARGET_DISTANCE_SQUARED = 900; 
    private static final double WALK_SPEED = 1.2;

    private final Main plugin;
    private final File file;
    private FileConfiguration data;
    private final Map<UUID, UUID> activePets = new HashMap<>(); 
    private final Map<UUID, UUID> petTargets = new HashMap<>(); 
    private BukkitTask petTask;

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

        clearPersistedActivePets();
        startPetTask();
    }

    public void shutdown() {
        if (petTask != null) petTask.cancel();
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

    public void assignTarget(UUID ownerId, LivingEntity victim) {
        UUID petId = activePets.get(ownerId);
        if (petId != null) petTargets.put(petId, victim.getUniqueId());
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

    public String getRecallItemId() {
        return plugin.getConfig().getString("pets.recall-item-id");
    }

    public boolean isRecallItem(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return false;
        String id = getRecallItemId();
        if (id.isBlank()) return false;
        CustomStack custom = CustomStack.byItemStack(stack);
        return custom != null && id.equalsIgnoreCase(custom.getNamespacedID());
    }

    public boolean recall(Player player) {
        UUID petEntityId = activePets.get(player.getUniqueId());
        if (petEntityId == null) return false;

        Entity petEntity = Bukkit.getEntity(petEntityId);
        if (petEntity == null || !petEntity.isValid()) {
            activePets.remove(player.getUniqueId());
            clearActivePet(player.getUniqueId());
            return false;
        }

        petTargets.remove(petEntityId);
        if (petEntity instanceof Mob mob) {
            mob.setTarget(null);
            mob.getPathfinder().stopPathfinding();
        }
        petEntity.teleport(petLocation(player));
        return true;
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
            Messages.send(player, "pets.mythicmob-not-configured", java.util.Map.of("pet", petId));
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
            Messages.send(player, "pets.summon-failed");
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
                Messages.send(player, "pets.mythicmob-summon-failed", java.util.Map.of("pet", petId));
                return;
            }

            activePets.put(player.getUniqueId(), found.getUniqueId());
            setActivePet(player.getUniqueId(), petId);
            found.getPersistentDataContainer().set(
                    new org.bukkit.NamespacedKey(plugin, "crew_pet_owner"),
                    PersistentDataType.STRING,
                    player.getUniqueId().toString()
            );
            Messages.send(player, "pets.activated", java.util.Map.of("pet", petId));
        }, 1L);
    }

    public void deactivate(Player player, boolean message) {
        if (player == null) return;
        UUID playerId = player.getUniqueId();
        UUID entityId = activePets.remove(playerId);
        if (entityId != null) {
            petTargets.remove(entityId);
            Entity entity = Bukkit.getEntity(entityId);
            if (entity != null && entity.isValid()) entity.remove();
        }
        clearActivePet(playerId);
        if (message) Messages.send(player, "pets.deactivated");
    }

    public void handleQuit(Player player) {
        deactivate(player, false);
    }

    private void startPetTask() {
        petTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Map.Entry<UUID, UUID> entry : new HashMap<>(activePets).entrySet()) {
                Player player = Bukkit.getPlayer(entry.getKey());
                Entity petEntity = Bukkit.getEntity(entry.getValue());
                if (player == null || !player.isOnline() || petEntity == null || !petEntity.isValid()) {
                    if (player != null) clearActivePet(player.getUniqueId());
                    activePets.remove(entry.getKey());
                    petTargets.remove(entry.getValue());
                    continue;
                }

                LivingEntity currentTarget = resolveTarget(petEntity.getUniqueId(), player);

                if (petEntity instanceof Mob mob) {
                    if (currentTarget != null) {

                        if (!currentTarget.equals(mob.getTarget())) {
                            mob.setTarget(currentTarget);
                        }
                    } else if (mob.getTarget() != null) {

                        mob.setTarget(null);
                    }
                }

                if (currentTarget != null) {
                    
                    continue;
                }

                if (!petEntity.getWorld().equals(player.getWorld())) {
                    petEntity.teleport(petLocation(player));
                    continue;
                }

                Location target = petLocation(player);
                double distanceSquared = petEntity.getLocation().distanceSquared(target);

                if (distanceSquared > TELEPORT_DISTANCE_SQUARED) {
                    
                    petEntity.teleport(target);
                } else if (petEntity instanceof Mob mob) {
                    if (distanceSquared > FOLLOW_DISTANCE_SQUARED) {
                        
                        mob.getPathfinder().moveTo(target, WALK_SPEED);
                    } else {
                        
                        mob.getPathfinder().stopPathfinding();
                    }
                }
            }
        }, 1L, 2L);
    }

    private LivingEntity resolveTarget(UUID petId, Player owner) {
        UUID targetId = petTargets.get(petId);
        if (targetId == null) return null;

        Entity target = Bukkit.getEntity(targetId);
        if (!(target instanceof LivingEntity living) || !living.isValid() || living.isDead()) {
            petTargets.remove(petId);
            return null;
        }
        if (!living.getWorld().equals(owner.getWorld())
                || living.getLocation().distanceSquared(owner.getLocation()) > MAX_TARGET_DISTANCE_SQUARED) {
            petTargets.remove(petId);
            return null;
        }
        return living;
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
