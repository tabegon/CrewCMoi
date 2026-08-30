package fr.crewcmoi.tab.staffmode;

import fr.crewcmoi.Main;
import fr.crewcmoi.tab.managers.TabListManager;
import fr.crewcmoi.tab.roles.Role;
import fr.crewcmoi.tab.roles.RoleManager;
import fr.crewcmoi.utils.ItemSerialization;
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

/**
 * Gère la commande /staff : bascule un membre du staff (Fonda/Admin/Dev/Mod,
 * voir Role#isStaffRole) entre deux états, en échangeant à chaque fois :
 *  - son inventaire (contenu + armure + main secondaire) ;
 *  - son rôle affiché dans le tab (vrai rôle <-> Player, voir
 *    RoleManager#setIncognito).
 * <p>
 * État "normal" (par défaut) : inventaire habituel du joueur, tab affiche son
 * vrai rôle (Fonda/Admin/Dev/Mod).
 * <p>
 * État "incognito" (après /staff) : inventaire dédié au staff (vide la première
 * fois, puis conservé tel quel d'une utilisation à l'autre — sers-t'en comme
 * d'une trousse à outils d'investigation), tab affiche "Player" pour se fondre
 * parmi les joueurs normaux.
 * <p>
 * Les deux inventaires sont persistés dans staffmode.yml (indépendant de la
 * base SQLite du reste du plugin), donc conservés d'une session à l'autre.
 * Si tu veux inverser le sens du toggle (inventaire staff + vrai rôle affiché
 * d'un côté, inventaire normal + Player de l'autre), il suffit d'inverser les
 * deux blocs dans {@link #toggle(Player)}.
 */
public class StaffModeManager {

    private static final int INVENTORY_SIZE = 36;
    private static final int ARMOR_SIZE = 4;
    private static final int SNAPSHOT_SIZE = INVENTORY_SIZE + ARMOR_SIZE + 1; // + main secondaire

    public enum ToggleResult {
        NOT_ALLOWED,
        NOW_STAFF,
        NOW_NORMAL
    }

    private final Main plugin;
    private final RoleManager roleManager;
    private final TabListManager tabListManager;

    private final File file;
    private YamlConfiguration config;
    private final Set<UUID> active = new HashSet<>();

    public StaffModeManager(Main plugin, RoleManager roleManager, TabListManager tabListManager) {
        this.plugin = plugin;
        this.roleManager = roleManager;
        this.tabListManager = tabListManager;
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
                // UUID mal formé dans staffmode.yml : on ignore simplement cette entrée.
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

    /**
     * Est-ce que ce joueur est actuellement en mode /staff (incognito) ? À
     * utiliser, par exemple, pour réappliquer l'état correct à la connexion
     * (voir StaffModeListener).
     */
    public boolean isActive(UUID uuid) {
        return active.contains(uuid);
    }

    /**
     * Bascule le joueur entre ses deux inventaires (normal <-> staff) et son
     * affichage dans le tab (vrai rôle <-> Player). Ne fait rien et renvoie
     * NOT_ALLOWED si le joueur n'a pas un vrai rôle de staff (Fonda/Admin/Dev/Mod).
     */
    public ToggleResult toggle(Player player) {
        Role realRole = roleManager.getRealRole(player);
        if (!realRole.isStaffRole()) {
            return ToggleResult.NOT_ALLOWED;
        }

        UUID uuid = player.getUniqueId();
        boolean wasActive = active.contains(uuid);

        if (!wasActive) {
            // Passage en mode staff (incognito) : on sauvegarde l'inventaire normal,
            // puis on charge l'inventaire staff (vide la toute première fois).
            saveSnapshot(uuid, "normal", takeSnapshot(player));
            applySnapshot(player, loadSnapshot(uuid, "staff"));
            active.add(uuid);
            roleManager.setIncognito(uuid, true);
        } else {
            // Retour au mode normal : on sauvegarde l'inventaire staff, puis on
            // restaure l'inventaire normal.
            saveSnapshot(uuid, "staff", takeSnapshot(player));
            applySnapshot(player, loadSnapshot(uuid, "normal"));
            active.remove(uuid);
            roleManager.setIncognito(uuid, false);
        }

        config.set("players." + uuid + ".active", !wasActive);
        save();

        // Réapplique immédiatement le préfixe/tri dans le tab avec le nouveau rôle.
        tabListManager.applyRole(player);

        return wasActive ? ToggleResult.NOW_NORMAL : ToggleResult.NOW_STAFF;
    }

    // ===================== Snapshot d'inventaire =====================

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
            // Aucun inventaire sauvegardé (ex : première utilisation de /staff) :
            // le joueur repart avec un inventaire vide plutôt qu'une erreur.
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
