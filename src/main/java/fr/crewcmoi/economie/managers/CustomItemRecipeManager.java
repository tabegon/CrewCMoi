package fr.crewcmoi.economie.managers;

import dev.lone.itemsadder.api.CustomStack;
import dev.lone.itemsadder.api.Events.ItemsAdderLoadDataEvent;
import fr.crewcmoi.Main;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.FurnaceRecipe;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public class CustomItemRecipeManager implements Listener {
    private final Main plugin;
    private boolean registered;

    public CustomItemRecipeManager(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemsAdderLoaded(ItemsAdderLoadDataEvent event) {
        Bukkit.getScheduler().runTask(plugin, this::registerRecipes);
    }

    public void registerRecipes() {
        if (!Bukkit.getPluginManager().isPluginEnabled("ItemsAdder")) return;
        List<String> missing = new ArrayList<>();
        register("chiseled_stone", new String[]{"...", ".S.", ".S."}, new char[]{'S'}, new Material[]{Material.STONE_SLAB}, missing);
        register("elevator_dungeon", new String[]{"SSS", "FCF", "F.F"}, new char[]{'S','F','C'}, new Material[]{Material.OAK_SLAB, Material.OAK_FENCE, Material.COBWEB}, missing);
        register("oak_chair", new String[]{".S.", ".PP", ".SS"}, new char[]{'S','P'}, new Material[]{Material.STICK, Material.OAK_PLANKS}, missing);
        register("oak_table", new String[]{"...", "SSS", ".F."}, new char[]{'S','F'}, new Material[]{Material.OAK_SLAB, Material.OAK_FENCE}, missing);
        register("pile_of_books", new String[]{".B.", ".B.", ".B."}, new char[]{'B'}, new Material[]{Material.BOOK}, missing);
        register("stone_column", new String[]{"...", ".S.", ".S."}, new char[]{'S'}, new Material[]{Material.STONE}, missing);
        register("wall_tarp", new String[]{"...", "WRW", "..."}, new char[]{'W','R'}, new Material[]{Material.WHITE_CARPET, Material.RED_CARPET}, missing);
        registerCrabClawCooking(missing);
        registered = true;
        if (!missing.isEmpty()) plugin.getLogger().warning("Recettes ItemsAdder ignorées car les items n'existent pas encore : " + String.join(", ", missing));
        plugin.getLogger().info("Recettes des items personnalisés enregistrées.");
    }

    private void registerCrabClawCooking(List<String> missing) {
        CustomStack raw = CustomStack.getInstance("server:crab_claw");
        CustomStack cooked = CustomStack.getInstance("server:cooked_crab_claw");
        if (raw == null || raw.getItemStack() == null) {
            missing.add("server:crab_claw (cuisson)");
            return;
        }
        if (cooked == null || cooked.getItemStack() == null) {
            missing.add("server:cooked_crab_claw");
            return;
        }
        List<ItemStack> variants = new ArrayList<>();
        NamespacedKey durabilityKey = new NamespacedKey(plugin, "crab_claw_durability");
        for (int damage = 0; damage < 128; damage++) {
            ItemStack variant = raw.getItemStack().clone();
            variant.setAmount(1);
            ItemMeta meta = variant.getItemMeta();
            if (meta == null) continue;
            if (damage > 0) {
                meta.getPersistentDataContainer().set(durabilityKey, PersistentDataType.INTEGER, damage);
            }
            variant.setItemMeta(meta);
            variants.add(variant);
        }
        if (variants.isEmpty()) {
            missing.add("server:crab_claw (item non-durabilité)");
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "cook_crab_claw");
        Bukkit.removeRecipe(key);
        FurnaceRecipe recipe = new FurnaceRecipe(key, cooked.getItemStack().clone(), new RecipeChoice.ExactChoice(variants.toArray(new ItemStack[0])), 0.1f, 200);
        recipe.setGroup("crewcmoi_crab_claw_cooking");
        Bukkit.addRecipe(recipe);
    }

    private void register(String id, String[] shape, char[] symbols, Material[] ingredients, List<String> missing) {
        CustomStack custom = CustomStack.getInstance("server:" + id);
        if (custom == null || custom.getItemStack() == null) {
            missing.add("server:" + id);
            return;
        }
        NamespacedKey key = new NamespacedKey(plugin, "craft_" + id);
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, custom.getItemStack().clone());
        recipe.shape(shape);
        for (int i = 0; i < symbols.length; i++) recipe.setIngredient(symbols[i], ingredients[i]);
        Bukkit.addRecipe(recipe);
    }

    public boolean isRegistered() { return registered; }
}
