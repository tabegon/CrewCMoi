package fr.crewcmoi.teleport.gui;
import org.bukkit.inventory.Inventory; import org.bukkit.inventory.InventoryHolder;
public class HomeShopHolder implements InventoryHolder { private Inventory inv; public Inventory getInventory(){return inv;} public void setInventory(Inventory i){inv=i;} }
