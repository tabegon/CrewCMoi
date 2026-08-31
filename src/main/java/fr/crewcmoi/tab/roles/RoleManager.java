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

/**
 * Détermine le rôle affiché pour chaque joueur, et gère les attributions
 * manuelles (/rank set), persistées dans un fichier roles.yml séparé (pas dans
 * la base SQLite du reste du plugin, pour rester totalement autonome).
 *
 * Un joueur a un rôle déterminé, dans l'ordre de priorité suivant :
 *  0. S'il est en "mode incognito" (voir /staff, StaffModeManager) : Player,
 *     quel que soit son rôle réel — le temps de ce mode, il apparaît comme un
 *     joueur normal dans le tab (voir getRole vs getRealRole) ;
 *  1. Sinon, rôle attribué manuellement via /rank set (persisté dans roles.yml) ;
 *  2. Sinon, le plus haut rôle (voir Role#getWeight) dont il a la permission
 *     associée (par défaut "crew.role.<id>", personnalisable en config.yml) ;
 *  3. Sinon Role.getDefault() (PLAYER).
 *
 * Les préfixes/couleurs et permissions de chaque rôle sont personnalisables
 * dans config.yml (section "roles"), voir getPrefix/getPermission.
 */
public class RoleManager {

    private final Main plugin;
    private final File rolesFile;
    private YamlConfiguration rolesConfig;

    private final Map<UUID, Role> manualRoles = new HashMap<>();
    private final Set<UUID> incognito = new HashSet<>();

    public RoleManager(Main plugin) {
        this.plugin = plugin;
        this.rolesFile = new File(plugin.getDataFolder(), "roles.yml");
        load();
    }

    // ===================== Attribution manuelle (persistée dans roles.yml) =====================

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
                // Ligne invalide (UUID mal formé) dans roles.yml : on l'ignore simplement.
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

    /**
     * Attribue manuellement un rôle à un joueur (persisté dans roles.yml). Reste
     * actif tant qu'il n'est pas retiré via clearRole, même après reconnexion.
     * Voir RoleCommand ("/rank set <joueur> <rôle>").
     */
    public void setRole(UUID uuid, Role role) {
        manualRoles.put(uuid, role);
        save();
    }

    /**
     * Retire l'attribution manuelle d'un joueur : son rôle redevient déterminé
     * automatiquement par ses permissions (voir getRole).
     */
    public void clearRole(UUID uuid) {
        if (manualRoles.remove(uuid) != null) {
            rolesConfig.set("players." + uuid, null);
            save();
        }
    }

    /**
     * Rôle assigné manuellement à ce joueur (/rank set), ou null s'il n'en a pas
     * (son rôle est alors déterminé par ses permissions).
     */
    public Role getManualRole(UUID uuid) {
        return manualRoles.get(uuid);
    }

    // ===================== Mode incognito (/staff) =====================

    /**
     * Active/désactive le mode incognito d'un joueur : tant qu'il est actif,
     * {@link #getRole(Player)} renvoie toujours Player, quel que soit son rôle
     * réel. Utilisé par StaffModeManager (/staff) pour permettre à un membre du
     * staff de se faire passer pour un joueur normal dans le tab.
     */
    public void setIncognito(UUID uuid, boolean value) {
        if (value) {
            incognito.add(uuid);
        } else {
            incognito.remove(uuid);
        }
    }

    public boolean isIncognito(UUID uuid) {
        return incognito.contains(uuid);
    }

    // ===================== Détection du rôle =====================

    /**
     * Détermine le rôle AFFICHÉ d'un joueur en ligne (voir priorité en tête de
     * classe) : Player si le mode incognito (/staff) est actif, sinon son rôle
     * réel (voir getRealRole).
     */
    public Role getRole(Player player) {
        if (incognito.contains(player.getUniqueId())) {
            return Role.getDefault();
        }
        return getRealRole(player);
    }

    /**
     * Détermine le rôle RÉEL d'un joueur, en ignorant le mode incognito (/staff).
     * Utilisé pour vérifier qu'un joueur a bien un rôle staff (Fonda/Admin/Dev/Mod)
     * avant de l'autoriser à utiliser /staff, même s'il est déjà en incognito.
     */
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

    // ===================== Configuration (préfixes/permissions) =====================

    /**
     * Préfixe affiché pour ce rôle (couleurs '&' déjà traduites en '§'),
     * personnalisable via config.yml : roles.<id>.prefix. Retombe sur le
     * préfixe par défaut du rôle (voir Role) si absent de la config.
     */
    public String getPrefix(Role role) {
        String raw = plugin.getConfig().getString("roles." + role.getId() + ".prefix", role.getDefaultPrefix());
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    /**
     * Nœud de permission utilisé pour détecter automatiquement ce rôle (ex: via
     * un plugin de permissions, ou directement l'OP pour crew.role.admin),
     * personnalisable via config.yml : roles.<id>.permission. Par défaut
     * "crew.role.<id>".
     */
    public String getPermission(Role role) {
        return plugin.getConfig().getString("roles." + role.getId() + ".permission", "crew.role." + role.getId());
    }
}
