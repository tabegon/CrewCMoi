package fr.crewcmoi.economie.managers;

import fr.crewcmoi.Main;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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

    public double getPrice(Material material) {
        return prices.getOrDefault(material, -1.0);
    }

    public boolean isSellable(Material material) {
        return prices.containsKey(material) && prices.get(material) > 0.0;
    }

    public Map<Material, Double> getAllPrices() {
        return java.util.Collections.unmodifiableMap(new HashMap<>(prices));
    }
}
