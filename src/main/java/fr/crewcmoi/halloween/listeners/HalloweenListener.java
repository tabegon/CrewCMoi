package fr.crewcmoi.halloween.listeners;

import fr.crewcmoi.halloween.gui.HalloweenChallengeHolder;
import fr.crewcmoi.halloween.gui.HalloweenNpcHolder;
import fr.crewcmoi.halloween.managers.HalloweenGuiManager;
import fr.crewcmoi.halloween.managers.HalloweenManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import dev.lone.itemsadder.api.Events.FurnitureInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;
import org.bukkit.configuration.ConfigurationSection;

public class HalloweenListener implements Listener {
    private final HalloweenManager manager; private final HalloweenGuiManager gui;
    public HalloweenListener(HalloweenManager manager,HalloweenGuiManager gui){this.manager=manager;this.gui=gui;}
    @EventHandler public void clickGui(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p)) return;
        if(e.getInventory().getHolder() instanceof HalloweenNpcHolder){
            e.setCancelled(true);
            int slot = e.getRawSlot();
            if (slot < 0 || slot >= e.getInventory().getSize()) return;
            if (slot == 1 || slot == 2 || slot == 3) {
                gui.openNpc(p, 1);
                return;
            }
            if (slot == 46 || slot == 47 || slot == 48) {
                gui.openNpc(p, 2);
                return;
            }
            int page = ((HalloweenNpcHolder)e.getInventory().getHolder()).getPage();
            buy(p, page, slot);
        }
        else if(e.getInventory().getHolder() instanceof HalloweenChallengeHolder){ e.setCancelled(true); int slot=e.getRawSlot(); String id=null; var ids=manager.getChallengeIds(); if(slot==11&&ids.size()>0)id=ids.get(0); if(slot==13&&ids.size()>1)id=ids.get(1); if(slot==15&&ids.size()>2)id=ids.get(2); if(id!=null && manager.chooseChallenge(p,id)) p.closeInventory(); }
    }
    @EventHandler public void dragGui(InventoryDragEvent e){ if(e.getInventory().getHolder() instanceof HalloweenNpcHolder || e.getInventory().getHolder() instanceof HalloweenChallengeHolder) e.setCancelled(true); }
    private void buy(Player p, int page, int slot){
        ConfigurationSection section = manager.getShopSlotConfig(page, slot);

        // Compatibilité avec l'ancien slot 13.
        if (section == null && slot == 13) {
            section = manager.getPlugin().getConfig().getConfigurationSection("halloween.table");
        }

        if (section == null) return;

        // Les produits du marchand Halloween sont désormais payés exclusivement
        // en Candy Corn. Le prix est lu depuis l'Arrow Exchange situé deux slots avant.
        int candyCost = manager.getCandyCornCost(page, slot);
        if (candyCost < 0) {
            p.sendMessage(color("&cCet item n'a pas d'Arrow Exchange associé. Contactez un administrateur."));
            return;
        }
        if (candyCost == 0) {
            p.sendMessage(color("&cLe coût de cet item est mal configuré."));
            return;
        }
        if (manager.countCandyCorn(p) < candyCost) {
            p.sendMessage(color("&cVous n'avez pas assez de &6Candy Corn&c. Il vous en faut &e" + candyCost + "&c."));
            return;
        }

        // Création de l'item AVANT toute dépense : une erreur de configuration
        // ne fait donc jamais perdre les Candy Corn.
        ItemStack item = manager.createShopItem(section);
        if(item == null){
            p.sendMessage(color("&cCet item est mal configuré. Contactez un administrateur."));
            return;
        }

        // Transaction atomique : si quelque chose échoue, l'inventaire est restauré
        // exactement comme avant l'achat. Ainsi ni l'objet ni les Candy Corn ne sont perdus.
        ItemStack[] inventorySnapshot = p.getInventory().getContents().clone();
        for (int i = 0; i < inventorySnapshot.length; i++) {
            if (inventorySnapshot[i] != null) {
                inventorySnapshot[i] = inventorySnapshot[i].clone();
            }
        }

        try {
            var leftovers = p.getInventory().addItem(item);

            // Les Candy Corn ne sont retirés qu'une fois l'item créé et ajouté.
            if (!manager.removeCandyCorn(p, candyCost)) {
                p.getInventory().setContents(inventorySnapshot);
                p.sendMessage(color("&cL'achat n'a pas pu être finalisé. Vos Candy Corn n'ont pas été retirés."));
                return;
            }

            leftovers.values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));

            String name = item.hasItemMeta() && item.getItemMeta().hasDisplayName()
                    ? item.getItemMeta().getDisplayName()
                    : "l'item";
            p.sendMessage(color("&aVous avez obtenu " + name + " &apour &6" + candyCost + " Candy Corn&a."));
            p.closeInventory();
        } catch (Exception ex) {
            p.getInventory().setContents(inventorySnapshot);
            manager.getPlugin().getLogger().warning("Erreur pendant l'achat Halloween au slot " + slot + " : " + ex.getMessage());
            p.sendMessage(color("&cUne erreur est survenue pendant l'achat. Aucun Candy Corn n'a été retiré."));
        }
    }
    @EventHandler public void place(BlockPlaceEvent e){if(manager.isTableItem(e.getItemInHand())) manager.registerTable(e.getBlockPlaced().getLocation());}
    @EventHandler public void breakTable(BlockBreakEvent e){if(manager.isSpecialTable(e.getBlock().getLocation())){manager.unregisterTable(e.getBlock().getLocation()); e.setDropItems(false); e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),manager.createTableItem());} manager.handleBreak(e);}
    /** Ouvre la GUI quand la furniture ItemsAdder déjà placée est cliquée. */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void interactFurniture(FurnitureInteractEvent e) {
        String tableId = manager.getPlugin().getConfig().getString("halloween.table.itemsadder-id", "").trim();
        if (tableId.isEmpty()) return;
        String furnitureId = e.getNamespacedID();
        if (furnitureId == null || !tableId.equalsIgnoreCase(furnitureId)) return;

        e.setCancelled(true);
        gui.openChallenges(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void interact(PlayerInteractEvent e){
        if (e.getAction() != Action.RIGHT_CLICK_AIR && e.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        // La furniture ItemsAdder de la table peut être cliquée directement
        // avant d'être considérée comme un bloc spécial par Bukkit.
        if (manager.isTableItem(e.getItem())) {
            e.setCancelled(true);
            gui.openChallenges(e.getPlayer());
            return;
        }

        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block b=e.getClickedBlock();
        if(b!=null&&manager.isSpecialTable(b.getLocation())){
            e.setCancelled(true);
            gui.openChallenges(e.getPlayer());
        }
    }
    @EventHandler public void kill(EntityDeathEvent e){manager.handleKill(e);}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s);}
}
