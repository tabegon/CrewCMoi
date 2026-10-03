package fr.crewcmoi.halloween.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.halloween.gui.HalloweenChallengeHolder;
import fr.crewcmoi.halloween.gui.HalloweenNpcHolder;
import fr.crewcmoi.other.utils.ConfigGuiItem;
import fr.crewcmoi.other.utils.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HalloweenGuiManager {
    private final Main plugin;
    private final HalloweenManager manager;

    public HalloweenGuiManager(Main plugin, HalloweenManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    public void openNpc(Player p) { openNpc(p, 1); }

    public void openNpc(Player p, int page) {
        HalloweenNpcHolder holder = new HalloweenNpcHolder();
        holder.setPage(page);

        int size = Math.max(9, Math.min(54, plugin.getConfig().getInt("halloween.shop.size", 54)));
        size = (size / 9) * 9;
        Inventory inv = Bukkit.createInventory(holder, size,
                color(plugin.getConfig().getString("halloween.shop.title", "&5&lHalloween &8• &dMarchand")));
        holder.setInventory(inv);

        // Design historique : NOTHING partout, avec les cases prévues réellement vides.
        ItemStack nothing = GuiItems.nothing(" ");
        for (int i = 0; i < size; i++) inv.setItem(i, nothing.clone());
        for (int emptySlot : new int[]{11, 20, 29, 38, 41, 42, 43}) {
            if (emptySlot < size) inv.setItem(emptySlot, null);
        }

        // Décoration historique aux quatre coins de navigation.
        ConfigurationSection fillerSection = plugin.getConfig().getConfigurationSection("halloween.shop.filler");
        ItemStack fillButton = fillerSection == null ? null
                : ConfigGuiItem.fromSection(plugin, fillerSection, Material.GRAY_STAINED_GLASS_PANE);
        for (int slot : plugin.getConfig().getIntegerList("halloween.shop.fill-slots")) {
            if (slot >= 0 && slot < size && fillButton != null) inv.setItem(slot, fillButton.clone());
        }

        // Nouveaux boutons : toute la zone 1-2-3 / 46-47-48 est cliquable.
        ConfigurationSection nav1 = plugin.getConfig().getConfigurationSection("halloween.shop.navigation." + "first");
        ConfigurationSection nav2 = plugin.getConfig().getConfigurationSection("halloween.shop.navigation." + "second");
        ItemStack navItem1 = ConfigGuiItem.fromSection(plugin, nav1, Material.ARROW);
        ItemStack navItem2 = ConfigGuiItem.fromSection(plugin, nav2, Material.ARROW);
        inv.setItem(2, navItem1);
        inv.setItem(47, navItem2);

        // Les produits restent aux anciens emplacements 12/21/30/39.
        ConfigurationSection trades = plugin.getConfig().getConfigurationSection(
                "halloween.shop.pages.page-" + page + ".trades");
        if (trades != null) {
            for (String key : trades.getKeys(false)) {
                ConfigurationSection trade = trades.getConfigurationSection(key);
                if (trade == null) continue;
                int productSlot = trade.getInt("product-slot", -1);
                int cost = trade.getInt("cost", 0);
                String id = trade.getString("itemsadder-id", "").trim();
                if (productSlot < 0 || productSlot >= size || id.isEmpty()) continue;

                ItemStack item = manager.createShopItem(trade);
                if (item != null) inv.setItem(productSlot, item);

                // Arrow Exchange : 10 + 9n, soit exactement deux slots avant le produit.
                int arrowSlot = productSlot - 2;
                if (arrowSlot >= 0 && arrowSlot < size) {
                    ConfigurationSection arrow = new org.bukkit.configuration.file.YamlConfiguration();
                    arrow.set("itemsadder-id", "server:arrow_exchange");
                    arrow.set("amount", cost);
                    ItemStack arrowItem = ConfigGuiItem.fromSection(plugin, arrow, Material.ARROW);
                    if (arrowItem != null) inv.setItem(arrowSlot, arrowItem);
                }
            }
        }

        p.openInventory(inv);
    }

    public void openChallenges(Player p) {
        HalloweenChallengeHolder holder = new HalloweenChallengeHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, color("&5&lTable maudite &8• &dDéfis"));
        holder.setInventory(inv);
        for (int i=0;i<27;i++) inv.setItem(i, GuiItems.nothing(" "));
        List<String> ids = manager.getChallengeIds();
        int[] slots = {11,13,15};
        for (int i=0;i<Math.min(3, ids.size());i++) {
            String id=ids.get(i);
            String path="halloween.challenges."+id;
            Material mat=Material.matchMaterial(plugin.getConfig().getString(path+".material","PAPER"));
            if(mat==null) mat=Material.PAPER;
            ItemStack item=new ItemStack(mat);
            ItemMeta meta=item.getItemMeta();
            if(meta!=null){
                meta.setDisplayName(color(plugin.getConfig().getString(path+".display-name","&k???")));
                List<String> lore=new ArrayList<>();
                lore.add(color("&7Défi : &f"+manager.getChallengeName(id)));
                lore.add(color("&7Difficulté : "+plugin.getConfig().getString(path+".difficulty","?")));
                lore.add(color("&7Objectif : &f"+plugin.getConfig().getInt(path+".amount",1)));
                if(manager.hasChosenToday(p.getUniqueId())) lore.add(color("&cVous avez déjà choisi aujourd'hui."));
                else lore.add(color("&aCliquez pour accepter ce défi"));
                meta.setLore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(slots[i],item);
        }
        p.openInventory(inv);
    }

    private String color(String s){ return ChatColor.translateAlternateColorCodes('&',s==null?"":s); }
}
