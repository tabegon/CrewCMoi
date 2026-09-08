package fr.crewcmoi.moderation.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.managers.TabListManager;
import fr.crewcmoi.tab.roles.Role;
import fr.crewcmoi.tab.roles.RoleManager;
import fr.crewcmoi.other.utils.ItemSerialization;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class StaffModeManager {

    private static final int INVENTORY_SIZE = 36;
    private static final int ARMOR_SIZE = 4;
    private static final int SNAPSHOT_SIZE = INVENTORY_SIZE + ARMOR_SIZE + 1; 

    public enum ToggleResult {
        NOT_ALLOWED,
        NOW_STAFF,
        NOW_NORMAL
    }

    private final Main plugin;
    private final RoleManager roleManager;
    private final TabListManager tabListManager;
    private final VanishManager vanishManager;

    private final File file;
    private YamlConfiguration config;
    private final Set<UUID> active = new HashSet<>();

    public StaffModeManager(Main plugin, RoleManager roleManager, TabListManager tabListManager, VanishManager vanishManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.tabListManager = tabListManager;
        this.vanishManager = vanishManager;
        this.file = new File(plugin.getDataFolder(), "staffmode.yml");
        load();
    }

    private void load() {
        if (!file.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Impossible de créer staffmode.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
        active.clear();

        ConfigurationSection section = config.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String uuidStr : section.getKeys(false)) {
            if (!section.getBoolean(uuidStr + ".active", false)) {
                continue;
            }
            try {
                active.add(UUID.fromString(uuidStr));
            } catch (IllegalArgumentException ignored) {
                
            }
        }
    }

    private void save() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder staffmode.yml", e);
        }
    }

    public boolean isActive(UUID uuid) {
        return active.contains(uuid);
    }

    public ToggleResult toggle(Player player) {
        Role realRole = roleManager.getRealRole(player);
        if (!realRole.isStaffRole()) {
            return ToggleResult.NOT_ALLOWED;
        }

        UUID uuid = player.getUniqueId();
        boolean wasActive = active.contains(uuid);

        if (!wasActive) {

            
            saveSnapshot(uuid, "normal", takeSnapshot(player));
            applySnapshot(player, loadSnapshot(uuid, "staff"));
            active.add(uuid);
            roleManager.setStaffModeActive(uuid, true);
        } else {

            saveSnapshot(uuid, "staff", takeSnapshot(player));
            applySnapshot(player, loadSnapshot(uuid, "normal"));
            active.remove(uuid);
            roleManager.setStaffModeActive(uuid, false);

            
            vanishManager.forceUnvanish(player);
        }

        config.set("players." + uuid + ".active", !wasActive);
        save();

        tabListManager.applyRole(player);

        return wasActive ? ToggleResult.NOW_NORMAL : ToggleResult.NOW_STAFF;
    }

    private ItemStack[] takeSnapshot(Player player) {
        PlayerInventory inventory = player.getInventory();
        ItemStack[] snapshot = new ItemStack[SNAPSHOT_SIZE];

        ItemStack[] contents = inventory.getContents();
        System.arraycopy(contents, 0, snapshot, 0, Math.min(INVENTORY_SIZE, contents.length));

        ItemStack[] armor = inventory.getArmorContents();
        System.arraycopy(armor, 0, snapshot, INVENTORY_SIZE, Math.min(ARMOR_SIZE, armor.length));

        snapshot[SNAPSHOT_SIZE - 1] = inventory.getItemInOffHand();
        return snapshot;
    }

    private void applySnapshot(Player player, ItemStack[] snapshot) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();
        inventory.setArmorContents(new ItemStack[ARMOR_SIZE]);
        inventory.setItemInOffHand(null);

        if (snapshot == null) {

            return;
        }

        ItemStack[] contents = new ItemStack[INVENTORY_SIZE];
        System.arraycopy(snapshot, 0, contents, 0, INVENTORY_SIZE);
        inventory.setContents(contents);

        ItemStack[] armor = new ItemStack[ARMOR_SIZE];
        System.arraycopy(snapshot, INVENTORY_SIZE, armor, 0, ARMOR_SIZE);
        inventory.setArmorContents(armor);

        inventory.setItemInOffHand(snapshot[SNAPSHOT_SIZE - 1]);
    }

    private void saveSnapshot(UUID uuid, String key, ItemStack[] snapshot) {
        try {
            config.set("players." + uuid + "." + key + "-inventory", ItemSerialization.toBase64(snapshot));
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder l'inventaire " + key + " de " + uuid, e);
        }
    }

    private ItemStack[] loadSnapshot(UUID uuid, String key) {
        String data = config.getString("players." + uuid + "." + key + "-inventory");
        if (data == null) {
            return null;
        }
        try {
            return ItemSerialization.itemArrayFromBase64(data);
        } catch (IOException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de charger l'inventaire " + key + " de " + uuid, e);
            return null;
        }
    }
}
