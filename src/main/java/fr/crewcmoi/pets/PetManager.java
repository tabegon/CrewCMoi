package fr.crewcmoi.pets;
import fr.crewcmoi.other.utils.Messages;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
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

public class PetManager implements org.bukkit.event.Listener {

    private static final double FOLLOW_DISTANCE_SQUARED = 2.25; 
    private static final double TELEPORT_DISTANCE_SQUARED = 400; 
    private static final double MAX_TARGET_DISTANCE_SQUARED = 900; 
    private static final double WALK_SPEED = 1.2;

    private final Main plugin;
    private final File file;
    private FileConfiguration data;
    private final Map<UUID, UUID> activePets = new HashMap<>(); 
    private final Map<UUID, UUID> petTargets = new HashMap<>();
    private final Map<UUID, Location> lastPathTargets = new HashMap<>();
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
        org.bukkit.configuration.ConfigurationSection section = plugin.getConfig().getConfigurationSection("pets.list");
        if (section == null) {
            plugin.getLogger().warning("Aucun pet n'est configuré dans pets.list.");
            return Collections.emptySet();
        }
        return section.getKeys(false);
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

    public Set<String> getOwnedPets(UUID playerId) {
        org.bukkit.configuration.ConfigurationSection section =
                data.getConfigurationSection("players." + playerId + ".pets");
        if (section == null) return new LinkedHashSet<>();
        Set<String> owned = new LinkedHashSet<>();
        for (String petId : section.getKeys(false)) {
            if (section.getBoolean(petId, false)) owned.add(petId);
        }
        return owned;
    }

    public boolean removeOwnedPet(UUID playerId, String petId) {
        if (!owns(playerId, petId) || getRemovalLockRemainingSeconds(playerId, petId) > 0L) return false;
        Player player = Bukkit.getPlayer(playerId);
        if (petId.equalsIgnoreCase(getActivePet(playerId)) && player != null) {
            deactivate(player, false);
        }
        data.set("players." + playerId + ".pets." + petId, null);
        save();
        return true;
    }

    public int resetOwnedPets(UUID playerId, int amount) {
        Set<String> owned = getOwnedPets(playerId);
        int removed = 0;
        for (String petId : owned) {
            if (removed >= amount) break;
            if (removeOwnedPet(playerId, petId)) removed++;
        }
        return removed;
    }

    public long getRemovalLockRemainingSeconds(UUID playerId, String petId) {
        long until = data.getLong("players." + playerId + ".removal-locks." + petId, 0L);
        long remaining = until - System.currentTimeMillis();
        if (remaining <= 0L) {
            if (until > 0L) {
                data.set("players." + playerId + ".removal-locks." + petId, null);
                save();
            }
            return 0L;
        }
        return (remaining + 999L) / 1000L;
    }

    private void startRemovalLock(UUID playerId, String petId) {
        long seconds = Math.max(0L, plugin.getConfig().getLong("pets.removal-lock-seconds", 30L));
        if (seconds <= 0L) return;
        data.set("players." + playerId + ".removal-locks." + petId,
                System.currentTimeMillis() + seconds * 1000L);
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
        String id = normalizeItemsAdderId(getItemId(petId));
        if (id.isBlank()) {
            plugin.getLogger().warning("Pet '" + petId + "' : aucun itemsadder-id n'est configuré.");
            return null;
        }

        CustomStack custom = findCustomStack(id);
        if (custom == null) {
            plugin.getLogger().warning("Pet : impossible de charger l'item ItemsAdder '" + id + "'. Vérifie que cet ID correspond bien à un custom item (et non à un custom entity) et que ItemsAdder a terminé son chargement.");
            return null;
        }

        ItemStack item = custom.getItemStack();
        return item == null ? null : item.clone();
    }

    /**
     * Résout un custom item ItemsAdder de façon tolérante.
     * ItemsAdder accepte normalement directement namespace:id, mais on garde
     * un second essai sans namespace pour les anciennes configurations.
     */
    private CustomStack findCustomStack(String id) {
        try {
            CustomStack custom = CustomStack.getInstance(id);
            if (custom != null) return custom;

            int separator = id.indexOf(':');
            if (separator > 0 && separator < id.length() - 1) {
                custom = CustomStack.getInstance(id.substring(separator + 1));
                if (custom != null) return custom;
            }
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Pet : erreur lors de l'accès à l'API ItemsAdder pour '" + id + "' : " + throwable.getClass().getSimpleName() + " - " + throwable.getMessage());
        }
        return null;
    }

    private String normalizeItemsAdderId(String id) {
        if (id == null) return "";
        return id.trim().toLowerCase(Locale.ROOT);
    }

    @org.bukkit.event.EventHandler
    public void onItemsAdderLoad(ItemsAdderLoadDataEvent event) {
        // ItemsAdder charge ses registries de manière asynchrone. On ne tente
        // pas d'accéder à Bukkit depuis l'événement async : on bascule sur le
        // thread serveur pour confirmer que les IDs des pets sont disponibles.
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String petId : getConfiguredPets()) {
                String id = normalizeItemsAdderId(getItemId(petId));
                if (!id.isBlank() && findCustomStack(id) == null) {
                    plugin.getLogger().warning("Pet '" + petId + "' : ItemsAdder est chargé, mais l'item '" + id + "' n'est pas présent dans son registre.");
                }
            }
        });
    }

    public boolean isPetItem(ItemStack stack, String petId) {
        if (stack == null || stack.getType().isAir()) return false;
        String id = normalizeItemsAdderId(getItemId(petId));
        if (id.isBlank()) return false;
        CustomStack custom = CustomStack.byItemStack(stack);
        if (custom == null) return false;
        String actual = normalizeItemsAdderId(custom.getNamespacedID());
        return id.equals(actual);
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
        lastPathTargets.remove(petEntityId);
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
            long remaining = getRemovalLockRemainingSeconds(player.getUniqueId(), petId);
            if (remaining > 0L) {
                Messages.send(player, "pets.cooldown-cannot-remove",
                        java.util.Map.of("seconds", String.valueOf(remaining)));
                return;
            }
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
            startRemovalLock(player.getUniqueId(), petId);
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
            lastPathTargets.remove(entityId);
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
                    lastPathTargets.remove(entry.getValue());
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
                    lastPathTargets.remove(petEntity.getUniqueId());
                    petEntity.teleport(petLocation(player));
                    continue;
                }

                Location target = petLocation(player);
                double distanceSquared = petEntity.getLocation().distanceSquared(target);

                if (distanceSquared > TELEPORT_DISTANCE_SQUARED) {
                    lastPathTargets.remove(petEntity.getUniqueId());
                    petEntity.teleport(target);
                } else if (petEntity instanceof Mob mob) {
                    UUID petId = petEntity.getUniqueId();
                    if (distanceSquared > FOLLOW_DISTANCE_SQUARED) {
                        Location previousTarget = lastPathTargets.get(petId);
                        if (previousTarget == null || !previousTarget.getWorld().equals(target.getWorld())
                                || previousTarget.distanceSquared(target) >= 1.0) {
                            mob.getPathfinder().moveTo(target, WALK_SPEED);
                            lastPathTargets.put(petId, target.clone());
                        }
                    } else if (lastPathTargets.remove(petId) != null) {
                        mob.getPathfinder().stopPathfinding();
                    }
                }
            }
        }, 1L, 10L);
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
