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
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.block.Action;

public class HalloweenListener implements Listener {
    private final HalloweenManager manager; private final HalloweenGuiManager gui;
    public HalloweenListener(HalloweenManager manager,HalloweenGuiManager gui){this.manager=manager;this.gui=gui;}
    @EventHandler public void clickGui(InventoryClickEvent e){
        if(!(e.getWhoClicked() instanceof Player p)) return;
        if(e.getInventory().getHolder() instanceof HalloweenNpcHolder){ e.setCancelled(true); if(e.getRawSlot()==13) buy(p); }
        else if(e.getInventory().getHolder() instanceof HalloweenChallengeHolder){ e.setCancelled(true); int slot=e.getRawSlot(); String id=null; var ids=manager.getChallengeIds(); if(slot==11&&ids.size()>0)id=ids.get(0); if(slot==13&&ids.size()>1)id=ids.get(1); if(slot==15&&ids.size()>2)id=ids.get(2); if(id!=null && manager.chooseChallenge(p,id)) p.closeInventory(); }
    }
    @EventHandler public void dragGui(InventoryDragEvent e){ if(e.getInventory().getHolder() instanceof HalloweenNpcHolder || e.getInventory().getHolder() instanceof HalloweenChallengeHolder) e.setCancelled(true); }
    private void buy(Player p){ double price=manager.getPlugin().getConfig().getDouble("halloween.table.price",10000); var eco=manager.getEconomy(); if(!eco.has(p.getUniqueId(),price)){p.sendMessage(color("&cVous n'avez pas assez d'argent."));return;} if(!eco.withdraw(p.getUniqueId(),price))return; var leftovers = p.getInventory().addItem(manager.createTableItem()); leftovers.values().forEach(item -> p.getWorld().dropItemNaturally(p.getLocation(), item)); p.sendMessage(color("&aVous avez acheté la table d'enchantement d'Halloween !")); p.closeInventory(); }
    @EventHandler public void place(BlockPlaceEvent e){if(manager.isTableItem(e.getItemInHand())) manager.registerTable(e.getBlockPlaced().getLocation());}
    @EventHandler public void breakTable(BlockBreakEvent e){if(manager.isSpecialTable(e.getBlock().getLocation())){manager.unregisterTable(e.getBlock().getLocation()); e.setDropItems(false); e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),manager.createTableItem());} manager.handleBreak(e);}
    @EventHandler public void interact(PlayerInteractEvent e){if(e.getAction()!=Action.RIGHT_CLICK_BLOCK)return; Block b=e.getClickedBlock(); if(b!=null&&manager.isSpecialTable(b.getLocation())){e.setCancelled(true);gui.openChallenges(e.getPlayer());}}
    @EventHandler public void kill(EntityDeathEvent e){manager.handleKill(e);}
    private String color(String s){return ChatColor.translateAlternateColorCodes('&',s);}
}
