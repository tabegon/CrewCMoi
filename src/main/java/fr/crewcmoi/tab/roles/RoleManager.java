package fr.crewcmoi.tab.roles;

import fr.crewcmoi.Main;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

public class RoleManager {

    private final Main plugin;
    private final File rolesFile;
    private YamlConfiguration rolesConfig;

    private final Map<UUID, Role> manualRoles = new HashMap<>();
    private final Set<UUID> staffModeActive = new HashSet<>();

    public RoleManager(Main plugin) {
        this.plugin = plugin;
        this.rolesFile = new File(plugin.getDataFolder(), "roles.yml");
        load();
    }

    private void load() {
        if (!rolesFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                rolesFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.WARNING, "Impossible de créer roles.yml", e);
            }
        }
        rolesConfig = YamlConfiguration.loadConfiguration(rolesFile);
        manualRoles.clear();

        ConfigurationSection section = rolesConfig.getConfigurationSection("players");
        if (section == null) {
            return;
        }
        for (String uuidStr : section.getKeys(false)) {
            Role role = Role.fromId(section.getString(uuidStr));
            if (role == null) {
                continue;
            }
            try {
                manualRoles.put(UUID.fromString(uuidStr), role);
            } catch (IllegalArgumentException ignored) {
                
            }
        }
    }

    private void save() {
        for (Map.Entry<UUID, Role> entry : manualRoles.entrySet()) {
            rolesConfig.set("players." + entry.getKey(), entry.getValue().getId());
        }
        try {
            rolesConfig.save(rolesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Impossible de sauvegarder roles.yml", e);
        }
    }

    public void setRole(UUID uuid, Role role) {
        manualRoles.put(uuid, role);
        save();
    }

    public void clearRole(UUID uuid) {
        if (manualRoles.remove(uuid) != null) {
            rolesConfig.set("players." + uuid, null);
            save();
        }
    }

    public Role getManualRole(UUID uuid) {
        return manualRoles.get(uuid);
    }

    

    public void setStaffModeActive(UUID uuid, boolean active) {
        if (active) {
            staffModeActive.add(uuid);
        } else {
            staffModeActive.remove(uuid);
        }
    }

    public boolean isStaffModeActive(UUID uuid) {
        return staffModeActive.contains(uuid);
    }

    

    public Role getRole(Player player) {
        Role real = getRealRole(player);
        if (real.isStaffRole() && !staffModeActive.contains(player.getUniqueId())) {
            return Role.VIP;
        }
        return real;
    }

    public Role getRealRole(Player player) {
        Role manual = manualRoles.get(player.getUniqueId());
        if (manual != null) {
            return manual;
        }

        Role best = null;
        for (Role role : Role.values()) {
            if (player.hasPermission(getPermission(role)) && (best == null || role.getWeight() < best.getWeight())) {
                best = role;
            }
        }
        return best != null ? best : Role.getDefault();
    }

    

    public String getPrefix(Role role) {
        String raw = plugin.getConfig().getString("roles." + role.getId() + ".prefix", role.getDefaultPrefix());
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public String getPermission(Role role) {
        return plugin.getConfig().getString("roles." + role.getId() + ".permission", "crew.role." + role.getId());
    }
}
