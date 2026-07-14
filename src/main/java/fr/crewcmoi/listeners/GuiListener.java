package fr.crewcmoi.listeners;

import fr.crewcmoi.gui.BaltopHolder;
import fr.crewcmoi.gui.SellGuiManager;
import fr.crewcmoi.gui.SellHolder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Empêche toute interaction avec la GUI /baltop (lecture seule)
 * et gère les interactions autorisées dans la GUI /sell.
 */
public class GuiListener implements Listener {

    private final SellGuiManager sellGuiManager;

    public GuiListener(SellGuiManager sellGuiManager) {
        this.sellGuiManager = sellGuiManager;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (holder instanceof BaltopHolder) {
            // Aucune interaction autorisée dans le classement : on empêche toute prise/dépôt/déplacement.
            event.setCancelled(true);
            return;
        }

        if (holder instanceof SellHolder) {
            int rawSlot = event.getRawSlot();
            boolean clickedTop = rawSlot >= 0 && rawSlot < topInventory.getSize();

            if (clickedTop) {
                if (rawSlot == SellHolder.CONFIRM_SLOT) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player player) {
                        sellGuiManager.sell(player, topInventory);
                    }
                    return;
                }

                if (!SellHolder.isItemSlot(rawSlot)) {
                    // Clic sur un emplacement de remplissage décoratif : interdit.
                    event.setCancelled(true);
                }
                // Sinon (emplacement de vente) : on laisse faire pour permettre de poser/retirer des objets.
            }
            // Clics dans l'inventaire du joueur (shift-click compris) : autorisés par défaut.
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory topInventory = event.getView().getTopInventory();
        InventoryHolder holder = topInventory.getHolder();

        if (holder instanceof BaltopHolder) {
            event.setCancelled(true);
            return;
        }

        if (holder instanceof SellHolder) {
            int topSize = topInventory.getSize();
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topSize && !SellHolder.isItemSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof SellHolder && event.getPlayer() instanceof Player player) {
            sellGuiManager.returnItems(player, event.getInventory());
        }
    }
}
