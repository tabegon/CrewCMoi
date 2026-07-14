package fr.crewcmoi.listeners;

import fr.crewcmoi.gui.AuctionGuiManager;
import fr.crewcmoi.gui.AuctionHolder;
import fr.crewcmoi.gui.BaltopHolder;
import fr.crewcmoi.gui.SellGuiManager;
import fr.crewcmoi.gui.SellHolder;
import fr.crewcmoi.managers.AuctionManager;
import org.bukkit.ChatColor;
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
 * et gère les interactions autorisées dans les GUI /sell et /ah.
 */
public class GuiListener implements Listener {

    private final SellGuiManager sellGuiManager;
    private final AuctionManager auctionManager;
    private final AuctionGuiManager auctionGuiManager;

    public GuiListener(SellGuiManager sellGuiManager, AuctionManager auctionManager, AuctionGuiManager auctionGuiManager) {
        this.sellGuiManager = sellGuiManager;
        this.auctionManager = auctionManager;
        this.auctionGuiManager = auctionGuiManager;
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
            return;
        }

        if (holder instanceof AuctionHolder auctionHolder) {
            int rawSlot = event.getRawSlot();
            boolean clickedTop = rawSlot >= 0 && rawSlot < topInventory.getSize();

            // Aucun dépôt/retrait libre autorisé : toute la GUI est en lecture/action seule.
            event.setCancelled(true);

            if (!clickedTop) {
                return;
            }

            if (!(event.getWhoClicked() instanceof Player player)) {
                return;
            }

            if (rawSlot == AuctionHolder.PREV_PAGE_SLOT) {
                auctionHolder.setPage(Math.max(0, auctionHolder.getPage() - 1));
                auctionGuiManager.render(player, auctionHolder);
                return;
            }

            if (rawSlot == AuctionHolder.NEXT_PAGE_SLOT) {
                auctionHolder.setPage(auctionHolder.getPage() + 1);
                auctionGuiManager.render(player, auctionHolder);
                return;
            }

            if (!AuctionHolder.isAuctionSlot(rawSlot)) {
                return;
            }

            Integer auctionId = auctionHolder.getAuctionId(rawSlot);
            if (auctionId == null) {
                return;
            }

            boolean isOwn = auctionManager.getCachedAuctions().stream()
                    .anyMatch(a -> a.getId() == auctionId && a.getSellerUuid().equals(player.getUniqueId()));

            if (isOwn) {
                auctionManager.cancel(player, auctionId, success -> {
                    if (Boolean.TRUE.equals(success)) {
                        player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&8[&6Economy&8] &r&aAnnonce retirée, objet(s) rendu(s)."));
                    }
                    auctionGuiManager.render(player, auctionHolder);
                });
            } else {
                auctionManager.buy(player, auctionId, result -> {
                    switch (result) {
                        case SUCCESS -> player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&8[&6Economy&8] &r&aAchat effectué avec succès !"));
                        case NOT_ENOUGH_MONEY -> player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&8[&6Economy&8] &r&cVous n'avez pas assez d'argent pour cet achat."));
                        case INVENTORY_FULL -> player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&8[&6Economy&8] &r&cVotre inventaire est plein."));
                        case OWN_ITEM -> player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&8[&6Economy&8] &r&cVous ne pouvez pas acheter votre propre annonce."));
                        case NOT_FOUND -> player.sendMessage(ChatColor.translateAlternateColorCodes('&',
                                "&8[&6Economy&8] &r&cCette annonce n'est plus disponible."));
                    }
                    auctionGuiManager.render(player, auctionHolder);
                });
            }
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

        if (holder instanceof AuctionHolder) {
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
