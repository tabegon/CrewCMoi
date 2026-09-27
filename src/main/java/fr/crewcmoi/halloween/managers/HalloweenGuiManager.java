package fr.crewcmoi.halloween.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.halloween.gui.HalloweenChallengeHolder;
import fr.crewcmoi.halloween.gui.HalloweenNpcHolder;
import fr.crewcmoi.other.utils.ConfigGuiItem;
import fr.crewcmoi.other.utils.GuiItems;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.List;

public class HalloweenGuiManager {
    private final Main plugin;
    private final HalloweenManager manager;

    public HalloweenGuiManager(Main plugin, HalloweenManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    /**
     * Ouvre le marchand de tables d'enchantement.
     *
     * Chaque slot peut être configuré dans :
     * halloween.shop.slots.<slot>
     *
     * Le slot peut contenir un item ItemsAdder, un prix, un nom et un lore.
     */
    public void openNpc(Player p) {
        HalloweenNpcHolder holder = new HalloweenNpcHolder();

        int size = Math.max(9, Math.min(54,
                plugin.getConfig().getInt("halloween.shop.size", 27)));
        size = (size / 9) * 9;

        Inventory inv = Bukkit.createInventory(holder, size,
                color(plugin.getConfig().getString("halloween.shop.title", "&5&lHalloween &8• &dMarchand")));
        holder.setInventory(inv);

        // `nothing` remplit uniquement les cases réellement vides.
        // Le `fill_button` ItemsAdder, lui, est réservé aux quatre slots
        // décoratifs configurés dans halloween.shop.fill-slots.
        ItemStack nothing = GuiItems.nothing(" ");
        for (int i = 0; i < size; i++) {
            inv.setItem(i, nothing.clone());
        }

        // Ces trois cases doivent rester réellement vides : elles ne doivent
        // recevoir ni NOTHING ni aucun bouton de remplissage.
        for (int emptySlot : new int[]{41, 42, 43}) {
            if (emptySlot < size) {
                inv.setItem(emptySlot, null);
            }
        }

        ConfigurationSection fillerSection = plugin.getConfig().getConfigurationSection("halloween.shop.filler");
        ItemStack fillButton = fillerSection == null
                ? null
                : ConfigGuiItem.fromSection(plugin, fillerSection, Material.GRAY_STAINED_GLASS_PANE);

        for (int slot : plugin.getConfig().getIntegerList("halloween.shop.fill-slots")) {
            if (slot >= 0 && slot < size && fillButton != null) {
                inv.setItem(slot, fillButton.clone());
            }
        }

        ConfigurationSection slots = plugin.getConfig().getConfigurationSection("halloween.shop.slots");
        if (slots != null) {
            for (String key : slots.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    plugin.getLogger().warning("Halloween GUI : slot invalide '" + key + "'.");
                    continue;
                }

                if (slot < 0 || slot >= size) {
                    plugin.getLogger().warning("Halloween GUI : le slot " + slot + " dépasse la taille de la GUI.");
                    continue;
                }

                ConfigurationSection slotSection = slots.getConfigurationSection(key);
                ItemStack item = manager.createShopItem(slotSection);
                if (item != null) {
                    inv.setItem(slot, item);
                }
            }
        }

        // Compatibilité avec l'ancienne configuration :
        // si aucun slot n'est défini, le produit historique reste au slot 13.
        if (slots == null || slots.getKeys(false).isEmpty()) {
            ConfigurationSection legacy = plugin.getConfig().getConfigurationSection("halloween.table");
            ItemStack table = manager.createTableItem();
            if (table != null) {
                applyConfiguredLore(table, legacy);
                inv.setItem(Math.min(13, size - 1), table);
            }
        }

        // Permet de définir directement des décorations par slot.
        ConfigurationSection decorations = plugin.getConfig().getConfigurationSection("halloween.shop.decorations");
        if (decorations != null) {
            for (String key : decorations.getKeys(false)) {
                int slot;
                try {
                    slot = Integer.parseInt(key);
                } catch (NumberFormatException ignored) {
                    continue;
                }
                if (slot < 0 || slot >= size) continue;

                ConfigurationSection section = decorations.getConfigurationSection(key);
                ItemStack item = ConfigGuiItem.fromSection(plugin, section, Material.PAPER);
                inv.setItem(slot, item);
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

    private void applyConfiguredLore(ItemStack item, ConfigurationSection section) {
        if (item == null || section == null) return;
        if (!section.isList("lore") && !section.contains("display-name")) return;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (section.contains("display-name")) {
            meta.setDisplayName(color(section.getString("display-name", "")));
        }
        if (section.isList("lore")) {
            List<String> lore = new ArrayList<>();
            for (String line : section.getStringList("lore")) {
                lore.add(color(line));
            }
            meta.setLore(lore);
        }
        item.setItemMeta(meta);
    }

    private String color(String s){
        return ChatColor.translateAlternateColorCodes('&',s==null?"":s);
    }
}
