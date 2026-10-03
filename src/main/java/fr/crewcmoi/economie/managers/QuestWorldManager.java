package fr.crewcmoi.economie.managers;

import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Gère la pool de seeds du monde quest et les montagnes associées à chaque seed.
 *
 * Le plugin ne recherche jamais de biome à la volée : les coordonnées des
 * montagnes sont préparées dans config.yml pour chaque seed validée.
 */
public final class QuestWorldManager {
    private final Main plugin;
    private QuestSeedProfile activeProfile;
    private World questWorld;

    public QuestWorldManager(Main plugin) {
        this.plugin = plugin;
    }

    /**
     * Charge le monde quest. Si le monde n'existe pas encore, une seed de la
     * pool est choisie aléatoirement et utilisée pour créer le monde.
     */
    public void initialize() {
        String worldName = plugin.getConfig().getString("quest-world.name", "quest");
        questWorld = Bukkit.getWorld(worldName);

        if (questWorld == null) {
            questWorld = createQuestWorld(worldName);
        }

        if (questWorld == null) {
            plugin.getLogger().severe("Impossible de charger le monde quest.");
            return;
        }

        activeProfile = findProfile(questWorld.getSeed());
        if (activeProfile == null) {
            plugin.getLogger().warning("La seed du monde quest (" + questWorld.getSeed()
                    + ") n'est pas présente dans quest-world.seed-pool. "
                    + "Les téléportations vers les montagnes sont désactivées.");
            return;
        }

        applyWorldBorder();
        plugin.getLogger().info("Monde quest actif : seed " + activeProfile.seed
                + " avec " + activeProfile.mountains.size() + " montagne(s) configurée(s).");
    }

    private World createQuestWorld(String worldName) {
        List<QuestSeedProfile> profiles = loadProfiles();
        if (profiles.isEmpty()) {
            plugin.getLogger().severe("Aucune seed n'est configurée dans quest-world.seed-pool.");
            return null;
        }

        QuestSeedProfile selected = profiles.get(ThreadLocalRandom.current().nextInt(profiles.size()));
        plugin.getLogger().info("Création du monde quest avec la seed sélectionnée : " + selected.seed);

        WorldCreator creator = new WorldCreator(worldName).seed(selected.seed);
        World world = Bukkit.createWorld(creator);
        if (world == null) {
            return null;
        }

        activeProfile = selected;
        applyWorldBorder(world);
        return world;
    }

    private QuestSeedProfile findProfile(long seed) {
        for (QuestSeedProfile profile : loadProfiles()) {
            if (profile.seed == seed) {
                return profile;
            }
        }
        return null;
    }

    private List<QuestSeedProfile> loadProfiles() {
        List<QuestSeedProfile> profiles = new ArrayList<>();
        ConfigurationSection pool = plugin.getConfig().getConfigurationSection("quest-world.seed-pool");
        if (pool == null) {
            return profiles;
        }

        for (String key : pool.getKeys(false)) {
            ConfigurationSection section = pool.getConfigurationSection(key);
            if (section == null || !section.contains("seed")) {
                continue;
            }

            long seed = section.getLong("seed");
            List<MountainLocation> mountains = new ArrayList<>();
            List<?> rawMountains = section.getList("mountains");
            if (rawMountains != null) {
                for (Object raw : rawMountains) {
                    if (!(raw instanceof java.util.Map<?, ?> map)) {
                        continue;
                    }
                    Number x = number(map.get("x"));
                    Number y = number(map.get("y"));
                    Number z = number(map.get("z"));
                    if (x != null && y != null && z != null) {
                        mountains.add(new MountainLocation(x.doubleValue(), y.doubleValue(), z.doubleValue()));
                    }
                }
            }

            profiles.add(new QuestSeedProfile(seed, mountains));
        }
        return profiles;
    }

    private Number number(Object value) {
        return value instanceof Number number ? number : null;
    }

    /** Retourne une montagne aléatoire de la seed active. */
    public Location getRandomMountain() {
        if (questWorld == null || activeProfile == null || activeProfile.mountains.isEmpty()) {
            return null;
        }

        MountainLocation mountain = activeProfile.mountains.get(
                ThreadLocalRandom.current().nextInt(activeProfile.mountains.size()));
        return new Location(questWorld, mountain.x + 0.5, mountain.y, mountain.z + 0.5);
    }

    /** Retourne une position aléatoire dans la bordure du monde quest. */
    public Location getRandomLocation() {
        if (questWorld == null) {
            return null;
        }

        double half = questWorld.getWorldBorder().getSize() / 2.0;
        Location center = questWorld.getWorldBorder().getCenter();
        double x = center.getX() + ThreadLocalRandom.current().nextDouble(-half + 2, half - 2);
        double z = center.getZ() + ThreadLocalRandom.current().nextDouble(-half + 2, half - 2);
        int y = questWorld.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
        return new Location(questWorld, x, y, z);
    }

    private void applyWorldBorder() {
        applyWorldBorder(questWorld);
    }

    private void applyWorldBorder(World world) {
        if (world == null) {
            return;
        }

        ConfigurationSection border = plugin.getConfig().getConfigurationSection("quest-world.border");
        if (border == null) {
            return;
        }

        double size = border.getDouble("size", 2000.0);
        double centerX = border.getDouble("center-x", 0.0);
        double centerZ = border.getDouble("center-z", 0.0);

        if (size <= 0) {
            return;
        }

        world.getWorldBorder().setCenter(centerX, centerZ);
        world.getWorldBorder().setSize(size);
    }

    public World getQuestWorld() {
        return questWorld;
    }

    public Long getActiveSeed() {
        return activeProfile == null ? null : activeProfile.seed;
    }

    private record QuestSeedProfile(long seed, List<MountainLocation> mountains) {
    }

    private record MountainLocation(double x, double y, double z) {
    }
}
