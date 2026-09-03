package fr.crewcmoi.pvp.managers;

import fr.crewcmoi.Main;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/** Gestion de l'unique kit disponible dans les duels. */
public class DuelKitManager {

    public static final String BASIC_KIT_ID = "basic";

    private final Main plugin;

    public DuelKitManager(Main plugin) {
        this.plugin = plugin;
    }

    public boolean isValidKit(String kitId) {
        return kitId == null || BASIC_KIT_ID.equalsIgnoreCase(kitId);
    }

    public double getPrice(String kitId) {
        if (BASIC_KIT_ID.equalsIgnoreCase(kitId)) {
            return plugin.getConfig().getDouble("duel.kits.basic.price", 1000.0);
        }
        return 0.0;
    }

    public String getDisplayName(String kitId) {
        if (BASIC_KIT_ID.equalsIgnoreCase(kitId)) {
            return plugin.getConfig().getString("duel.kits.basic.name", "Kit Classique");
        }
        return "Aucun kit";
    }

    /**
     * Donne exactement le kit visible sur la capture : équipement netherite,
     * potions, consommables et armes. Le kit ne contient aucun objet de
     * l'inventaire personnel du joueur.
     */
    public void applyKit(Player player, String kitId) {
        if (!BASIC_KIT_ID.equalsIgnoreCase(kitId)) {
            return;
        }

        PlayerInventory inv = player.getInventory();
        inv.clear();

        // Armure : Protection IV + Unbreaking III + Mending.
        inv.setHelmet(enchanted(Material.NETHERITE_HELMET,
                Enchantment.PROTECTION, 4,
                Enchantment.UNBREAKING, 3,
                Enchantment.MENDING, 1));
        inv.setChestplate(enchanted(Material.NETHERITE_CHESTPLATE,
                Enchantment.PROTECTION, 4,
                Enchantment.UNBREAKING, 3,
                Enchantment.MENDING, 1));
        inv.setLeggings(enchanted(Material.NETHERITE_LEGGINGS,
                Enchantment.PROTECTION, 4,
                Enchantment.UNBREAKING, 3,
                Enchantment.MENDING, 1));
        inv.setBoots(enchanted(Material.NETHERITE_BOOTS,
                Enchantment.PROTECTION, 4,
                Enchantment.UNBREAKING, 3,
                Enchantment.MENDING, 1));

        // Bouclier dans la main secondaire.
        inv.setItemInOffHand(enchanted(Material.SHIELD,
                Enchantment.MENDING, 1,
                Enchantment.UNBREAKING, 3));

        // Inventaire principal : 3 lignes de 9 slots, exactement comme sur la capture.
        inv.setItem(9, splashPotion(PotionType.STRENGTH));
        inv.setItem(10, splashPotion(PotionType.STRENGTH));
        inv.setItem(11, splashPotion(PotionType.SWIFTNESS));
        inv.setItem(12, splashPotion(PotionType.SWIFTNESS));
        inv.setItem(13, splashPotion(PotionType.FIRE_RESISTANCE));
        inv.setItem(14, splashPotion(PotionType.FIRE_RESISTANCE));
        inv.setItem(15, stack(Material.EXPERIENCE_BOTTLE, 64));
        inv.setItem(16, stack(Material.EXPERIENCE_BOTTLE, 64));
        inv.setItem(17, stack(Material.EXPERIENCE_BOTTLE, 64));

        inv.setItem(18, splashPotion(PotionType.STRENGTH));
        inv.setItem(19, splashPotion(PotionType.STRENGTH));
        inv.setItem(20, splashPotion(PotionType.SWIFTNESS));
        inv.setItem(21, splashPotion(PotionType.SWIFTNESS));
        inv.setItem(22, splashPotion(PotionType.FIRE_RESISTANCE));
        inv.setItem(23, stack(Material.ARROW, 64));
        inv.setItem(24, splashPotion(PotionType.HEALING));
        inv.setItem(25, splashPotion(PotionType.HEALING));
        inv.setItem(26, stack(Material.COBWEB, 64));

        inv.setItem(27, splashPotion(PotionType.STRENGTH));
        inv.setItem(28, splashPotion(PotionType.STRENGTH));
        inv.setItem(29, splashPotion(PotionType.SWIFTNESS));
        inv.setItem(30, splashPotion(PotionType.SWIFTNESS));
        inv.setItem(31, new ItemStack(Material.WATER_BUCKET));
        inv.setItem(32, stack(Material.SNOWBALL, 64));
        inv.setItem(33, stack(Material.ENDER_PEARL, 16));
        inv.setItem(34, splashPotion(PotionType.HEALING));
        inv.setItem(35, stack(Material.GOLDEN_APPLE, 64));

        // Barre rapide : épée, mace, hache puis les utilitaires visibles sur la capture.
        inv.setItem(0, enchanted(Material.NETHERITE_SWORD,
                Enchantment.SHARPNESS, 5,
                Enchantment.MENDING, 1,
                Enchantment.UNBREAKING, 3,
                Enchantment.FIRE_ASPECT, 2));
        inv.setItem(1, enchanted(Material.MACE,
                Enchantment.DENSITY, 5,
                Enchantment.WIND_BURST, 3,
                Enchantment.MENDING, 1,
                Enchantment.UNBREAKING, 3));
        inv.setItem(2, enchanted(Material.NETHERITE_AXE,
                Enchantment.SHARPNESS, 5,
                Enchantment.UNBREAKING, 3,
                Enchantment.MENDING, 1));
        inv.setItem(3, stack(Material.COBWEB, 64));
        inv.setItem(4, new ItemStack(Material.WATER_BUCKET));
        inv.setItem(5, stack(Material.SNOWBALL, 64));
        inv.setItem(6, stack(Material.ENDER_PEARL, 16));
        inv.setItem(7, splashPotion(PotionType.HEALING));
        inv.setItem(8, stack(Material.GOLDEN_APPLE, 64));
        player.updateInventory();
    }

    private ItemStack splashPotion(PotionType type) {
        ItemStack item = new ItemStack(Material.SPLASH_POTION);
        PotionMeta meta = (PotionMeta) item.getItemMeta();
        if (meta != null) {
            meta.setBasePotionType(type);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack stack(Material material, int amount) {
        return new ItemStack(material, amount);
    }

    private ItemStack enchanted(Material material, Object... enchantments) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            for (int i = 0; i < enchantments.length; i += 2) {
                Enchantment enchantment = (Enchantment) enchantments[i];
                int level = (Integer) enchantments[i + 1];
                meta.addEnchant(enchantment, level, true);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
