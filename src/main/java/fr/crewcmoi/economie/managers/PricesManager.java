package fr.crewcmoi.economie.managers;

import fr.crewcmoi.Main;
import dev.lone.itemsadder.api.CustomStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class PricesManager {

    private final Main plugin;
    private File pricesFile;
    private FileConfiguration pricesConfig;
    private final Map<Material, Double> prices = new HashMap<>();
    private final Map<String, Double> customPrices = new HashMap<>();

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
        customPrices.clear();

        org.bukkit.configuration.ConfigurationSection customSection = pricesConfig.getConfigurationSection("custom-prices");
        if (customSection != null) {
            for (String id : customSection.getKeys(false)) {
                customPrices.put(id.toLowerCase(java.util.Locale.ROOT), customSection.getDouble(id, -1.0));
            }
        }
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("prices.yml"), StandardCharsets.UTF_8)) {
            FileConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
            org.bukkit.configuration.ConfigurationSection defaultCustomSection = defaults.getConfigurationSection("custom-prices");
            if (defaultCustomSection != null) {
                for (String id : defaultCustomSection.getKeys(false)) {
                    customPrices.putIfAbsent(id.toLowerCase(java.util.Locale.ROOT), defaultCustomSection.getDouble(id, -1.0));
                }
            }
        } catch (Exception exception) {
            plugin.getLogger().warning("Impossible de charger les prix par défaut des items personnalisés : " + exception.getMessage());
        }

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

    public double getPrice(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) return -1.0;
        if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) {
            try {
                CustomStack custom = CustomStack.byItemStack(stack);
                if (custom != null) {
                    Double customPrice = customPrices.get(custom.getNamespacedID().toLowerCase(java.util.Locale.ROOT));
                    if (customPrice != null) return customPrice;
                }
            } catch (RuntimeException ignored) {
            }
        }
        return getPrice(stack.getType());
    }

    public boolean isSellable(Material material) {
        return prices.containsKey(material) && prices.get(material) > 0.0;
    }

    public Map<Material, Double> getAllPrices() {
        return java.util.Collections.unmodifiableMap(new HashMap<>(prices));
    }
}
