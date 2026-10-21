package fr.crewcmoi.tab.roles;

import fr.crewcmoi.Main;
import org.bukkit.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
        synchronizeLuckPermsWeights();
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

        // Les rangs sont désormais gérés par LuckPerms.
        // L'ancien système players: de roles.yml est volontairement ignoré
        // afin qu'un ancien rang manuel ne puisse plus écraser le groupe LuckPerms.
        manualRoles.clear();
    }


    private void synchronizeLuckPermsWeights() {
        if (!Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) return;
        for (Role role : Role.values()) {
            String group = plugin.getConfig().getString(
                    "roles." + role.getId() + ".luckperms-group", role.getId()).trim();
            int weight = plugin.getConfig().getInt(
                    "roles." + role.getId() + ".luckperms-weight", role.getWeight());
            if (group.isEmpty()) group = role.getId();

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "lp creategroup " + group);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp group " + group + " setweight " + weight);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                    "lp group " + group + " permission set " + getPermission(role) + " true");
            if (!group.equalsIgnoreCase("default")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                        "lp group " + group + " parent add default");
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

    public boolean setRole(UUID uuid, Role role) {
        if (role == null) return false;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name == null || name.isBlank()) return false;

        String newGroup = plugin.getConfig().getString(
                "roles." + role.getId() + ".luckperms-group", role.getId()).trim();
        if (newGroup.isEmpty() || !newGroup.matches("[A-Za-z0-9_-]+")) return false;

        boolean added = Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(), "lp user " + name + " parent set " + newGroup);

        if (added) {
            manualRoles.remove(uuid);
            rolesConfig.set("players." + uuid, null);
            save();
        }
        return added;
    }

    public boolean clearRole(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        if (name == null || name.isBlank()) return false;

        String defaultGroup = plugin.getConfig().getString("roles.player.luckperms-group", "default").trim();
        if (defaultGroup.isEmpty() || !defaultGroup.matches("[A-Za-z0-9_-]+")) defaultGroup = "default";
        boolean reset = Bukkit.dispatchCommand(
                Bukkit.getConsoleSender(), "lp user " + name + " parent set " + defaultGroup);
        if (!reset) return false;

        manualRoles.remove(uuid);
        rolesConfig.set("players." + uuid, null);
        save();
        return true;
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
            if (player.hasPermission(getPermission(role))
                    && (best == null || role.getPriority() > best.getPriority())) {
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
