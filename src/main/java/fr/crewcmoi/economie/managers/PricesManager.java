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
        mergeOceanTemplePriceDefaults();

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

    private void mergeOceanTemplePriceDefaults() {
        FileConfiguration defaults;
        try (InputStreamReader reader = new InputStreamReader(plugin.getResource("prices.yml"), StandardCharsets.UTF_8)) {
            defaults = YamlConfiguration.loadConfiguration(reader);
        } catch (Exception exception) {
            plugin.getLogger().warning("Impossible de charger les nouveaux prix des objets marins : " + exception.getMessage());
            return;
        }

        Map<String, Double> previousDefaults = Map.ofEntries(
                Map.entry("CONDUIT", 1500.0), Map.entry("HEART_OF_THE_SEA", 800.0),
                Map.entry("NAUTILUS_SHELL", 60.0), Map.entry("DARK_PRISMARINE", 1.0),
                Map.entry("DARK_PRISMARINE_SLAB", 2.0), Map.entry("DARK_PRISMARINE_STAIRS", 2.0),
                Map.entry("DARK_PRISMARINE_WALL", 2.0), Map.entry("PRISMARINE", 1.0),
                Map.entry("PRISMARINE_BRICKS", 1.0), Map.entry("PRISMARINE_BRICKS_SLAB", 2.0),
                Map.entry("PRISMARINE_BRICKS_STAIRS", 2.0), Map.entry("PRISMARINE_BRICKS_WALL", 2.0),
                Map.entry("PRISMARINE_CRYSTALS", 10.0), Map.entry("PRISMARINE_SHARD", 8.0),
                Map.entry("PRISMARINE_SLAB", 2.0), Map.entry("PRISMARINE_STAIRS", 2.0),
                Map.entry("PRISMARINE_WALL", 2.0), Map.entry("SEA_LANTERN", 25.0),
                Map.entry("SPONGE", 20.0), Map.entry("WET_SPONGE", 15.0), Map.entry("TRIDENT", 900.0)
        );

        boolean changed = false;
        for (Map.Entry<String, Double> entry : previousDefaults.entrySet()) {
            String key = entry.getKey();
            String path = "prices." + key;
            if (!defaults.contains(path)) continue;
            double current = pricesConfig.getDouble(path, Double.NaN);
            double newDefault = defaults.getDouble(path);
            if (Double.isNaN(current) || Math.abs(current - entry.getValue()) < 0.000001D) {
                if (Double.isNaN(current) || Math.abs(current - newDefault) >= 0.000001D) {
                    pricesConfig.set(path, newDefault);
                    changed = true;
                }
            }
        }

        if (changed) {
            try {
                ((YamlConfiguration) pricesConfig).save(pricesFile);
                plugin.getLogger().info("Prix par défaut des objets liés aux temples marins mis à jour (les prix personnalisés sont conservés).");
            } catch (Exception exception) {
                plugin.getLogger().warning("Impossible d'enregistrer les prix marins : " + exception.getMessage());
            }
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
