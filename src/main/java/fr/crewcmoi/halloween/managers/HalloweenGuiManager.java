package fr.crewcmoi.halloween.managers;

import fr.crewcmoi.Main;
import fr.crewcmoi.halloween.gui.HalloweenChallengeHolder;
import fr.crewcmoi.halloween.gui.HalloweenNpcHolder;
import fr.crewcmoi.other.utils.GuiItems;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class HalloweenGuiManager {
    private final Main plugin;
    private final HalloweenManager manager;
    public HalloweenGuiManager(Main plugin, HalloweenManager manager) { this.plugin=plugin; this.manager=manager; }

    public void openNpc(Player p) {
        HalloweenNpcHolder holder = new HalloweenNpcHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, color("&5&lHalloween &8• &dMarchand"));
        holder.setInventory(inv);
        for (int i=0;i<27;i++) inv.setItem(i, GuiItems.nothing(" "));
        ItemStack table = manager.createTableItem();
        ItemMeta meta = table.getItemMeta();
        if (meta != null) {
            List<String> lore = new ArrayList<>();
            lore.add(color("&7Prix : &e" + plugin.getConfig().getDouble("halloween.table.price", 10000) + " &6" + plugin.getConfig().getString("economy.currency-symbol", "")));
            lore.add(color(""));
            lore.add(color("&eCliquez pour acheter"));
            meta.setLore(lore); table.setItemMeta(meta);
        }
        inv.setItem(13, table); p.openInventory(inv);
    }

    public void openChallenges(Player p) {
        HalloweenChallengeHolder holder = new HalloweenChallengeHolder();
        Inventory inv = Bukkit.createInventory(holder, 27, color("&5&lTable maudite &8• &dDéfis"));
        holder.setInventory(inv);
        for (int i=0;i<27;i++) inv.setItem(i, GuiItems.nothing(" "));
        List<String> ids = manager.getChallengeIds();
        int[] slots = {11,13,15};
        for (int i=0;i<Math.min(3, ids.size());i++) {
            String id=ids.get(i); String path="halloween.challenges."+id;
            Material mat=Material.matchMaterial(plugin.getConfig().getString(path+".material","PAPER")); if(mat==null) mat=Material.PAPER;
            ItemStack item=new ItemStack(mat); ItemMeta meta=item.getItemMeta();
            if(meta!=null){ meta.setDisplayName(color(plugin.getConfig().getString(path+".display-name","&k???"))); List<String> lore=new ArrayList<>(); lore.add(color("&7Défi : &f"+manager.getChallengeName(id))); lore.add(color("&7Difficulté : "+plugin.getConfig().getString(path+".difficulty","?"))); lore.add(color("&7Objectif : &f"+plugin.getConfig().getInt(path+".amount",1))); if(manager.hasChosenToday(p.getUniqueId())) lore.add(color("&cVous avez déjà choisi aujourd'hui.")); else lore.add(color("&aCliquez pour accepter ce défi")); meta.setLore(lore); item.setItemMeta(meta);}
            inv.setItem(slots[i],item);
        }
        p.openInventory(inv);
    }
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
}
