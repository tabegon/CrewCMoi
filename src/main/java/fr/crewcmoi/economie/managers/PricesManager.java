package fr.crewcmoi.economie.managers;

import fr.crewcmoi.Main;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Charge et donne accès aux prix de vente définis dans prices.yml.
 * Format du fichier :
 * prices:
 *   COBBLESTONE: 0.5
 *   DIAMOND: 50.0
 */
public class PricesManager {

    private final Main plugin;
    private File pricesFile;
    private FileConfiguration pricesConfig;
    private final Map<Material, Double> prices = new HashMap<>();

    public PricesManager(Main plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        pricesFile = new File(plugin.getDataFolder(), "prices.yml");
        if (!pricesFile.exists()) {
            plugin.saveResource("prices.yml", false);
        }
        pricesConfig = YamlConfiguration.loadConfiguration(pricesFile);

        prices.clear();

        org.bukkit.configuration.ConfigurationSection section = pricesConfig.getConfigurationSection("prices");
        if (section == null) {
            return;
        }

        for (String key : section.getKeys(false)) {
            Material material = Material.matchMaterial(key);
            if (material == null) {
                plugin.getLogger().warning("prices.yml : matériau inconnu \"" + key + "\" ignoré.");
                continue;
            }
            double price = section.getDouble(key, 0.0);
            prices.put(material, price);
        }
    }

    public void reload() {
        load();
    }

    /**
     * Retourne le prix unitaire d'un matériau, ou -1 si celui-ci n'est pas vendable.
     */
    public double getPrice(Material material) {
        return prices.getOrDefault(material, -1.0);
    }

    public boolean isSellable(Material material) {
        return prices.containsKey(material) && prices.get(material) > 0.0;
    }
}
