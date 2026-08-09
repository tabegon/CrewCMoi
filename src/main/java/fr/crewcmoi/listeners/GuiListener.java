package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.gui.AuctionGuiManager;
import fr.crewcmoi.gui.AuctionHolder;
import fr.crewcmoi.gui.BaltopHolder;
import fr.crewcmoi.gui.BountyHolder;
import fr.crewcmoi.gui.ConfirmationHolder;
import fr.crewcmoi.gui.SellGuiManager;
import fr.crewcmoi.gui.SellHolder;
import fr.crewcmoi.managers.AuctionManager;
import org.bukkit.Bukkit;
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
 * et gère les interactions autorisées dans les GUI /sell, /ah et les
 * GUI de confirmation (achat /ah, vente /sell).
 */
public class GuiListener implements Listener {

    private final Main plugin;
    private final SellGuiManager sellGuiManager;
    private final AuctionManager auctionManager;
    private final AuctionGuiManager auctionGuiManager;

    public GuiListener(Main plugin, SellGuiManager sellGuiManager, AuctionManager auctionManager, AuctionGuiManager auctionGuiManager) {
        this.plugin = plugin;
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

        if (holder instanceof BountyHolder) {
            // GUI de consultation uniquement.
            event.setCancelled(true);
            return;
        }

        if (holder instanceof ConfirmationHolder confirmationHolder) {
            // GUI de confirmation générique : seuls les boutons confirmer/annuler sont actionnables.
            event.setCancelled(true);

            int rawSlot = event.getRawSlot();
            boolean clickedTop = rawSlot >= 0 && rawSlot < topInventory.getSize();
            if (!clickedTop || !(event.getWhoClicked() instanceof Player player)) {
                return;
            }

            if (rawSlot == ConfirmationHolder.CONFIRM_SLOT) {
                resolveConfirmation(player, confirmationHolder, confirmationHolder.getOnConfirm());
            } else if (rawSlot == ConfirmationHolder.CANCEL_SLOT) {
                resolveConfirmation(player, confirmationHolder, confirmationHolder.getOnCancel());
            }
            return;
        }

        if (holder instanceof SellHolder sellHolder) {
            int rawSlot = event.getRawSlot();
            boolean clickedTop = rawSlot >= 0 && rawSlot < topInventory.getSize();

            if (sellHolder.isConfirming()) {
                // Pendant l'étape de confirmation : plus aucune manipulation d'objet n'est autorisée,
                // seuls les boutons confirmer/annuler réagissent.
                event.setCancelled(true);

                if (!clickedTop || !(event.getWhoClicked() instanceof Player player)) {
                    return;
                }

                if (rawSlot == SellHolder.CONFIRM_SLOT) {
                    sellGuiManager.confirmSale(player, topInventory, sellHolder);
                } else if (rawSlot == SellHolder.CANCEL_CONFIRM_SLOT) {
                    sellGuiManager.cancelConfirmation(topInventory, sellHolder);
                }
                return;
            }

            if (clickedTop) {
                if (rawSlot == SellHolder.SELL_SLOT) {
                    event.setCancelled(true);
                    if (event.getWhoClicked() instanceof Player player) {
                        sellGuiManager.askConfirmation(player, topInventory, sellHolder);
                    }
                    return;
                }

                if (!SellHolder.isItemSlot(rawSlot)) {
                    // Clic sur un emplacement de remplissage décoratif : interdit.
                    event.setCancelled(true);
                    return;
                }
                // Sinon (emplacement de vente) : on laisse faire pour permettre de poser/retirer des objets.
            }
            // Clics dans l'inventaire du joueur (shift-click compris) : autorisés par défaut,
            // et peuvent aussi remplir la GUI de vente (shift-click depuis l'inventaire du joueur).

            // On recalcule le montant affiché sur l'émeraude au tick suivant,
            // une fois que le serveur a effectivement déplacé l'objet.
            Bukkit.getScheduler().runTask(plugin, () -> sellGuiManager.refreshConfirmButton(topInventory));
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
                // Achat : on passe systématiquement par une GUI de confirmation avant d'exécuter l'achat.
                int page = auctionHolder.getPage();
                player.closeInventory();
                Bukkit.getScheduler().runTask(plugin, () -> auctionGuiManager.openBuyConfirmation(player, page, auctionId));
            }
        }
    }

    /**
     * Exécute le Runnable choisi (confirmer ou annuler) une seule fois puis ferme la GUI.
     * Protège contre une double exécution si l'InventoryCloseEvent se déclenche ensuite.
     */
    private void resolveConfirmation(Player player, ConfirmationHolder confirmationHolder, Runnable action) {
        if (confirmationHolder.isResolved()) {
            return;
        }
        confirmationHolder.setResolved(true);
        player.closeInventory();
        if (action != null) {
            Bukkit.getScheduler().runTask(plugin, action);
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

        if (holder instanceof BountyHolder) {
            event.setCancelled(true);
            return;
        }

        if (holder instanceof ConfirmationHolder) {
            event.setCancelled(true);
            return;
        }

        if (holder instanceof AuctionHolder) {
            event.setCancelled(true);
            return;
        }

        if (holder instanceof SellHolder sellHolder) {
            if (sellHolder.isConfirming()) {
                event.setCancelled(true);
                return;
            }

            int topSize = topInventory.getSize();
            for (int rawSlot : event.getRawSlots()) {
                if (rawSlot < topSize && !SellHolder.isItemSlot(rawSlot)) {
                    event.setCancelled(true);
                    return;
                }
            }
            Bukkit.getScheduler().runTask(plugin, () -> sellGuiManager.refreshConfirmButton(topInventory));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof SellHolder sellHolder && event.getPlayer() instanceof Player player) {
            sellGuiManager.returnItems(player, event.getInventory(), sellHolder);
            return;
        }

        if (holder instanceof ConfirmationHolder confirmationHolder && event.getPlayer() instanceof Player) {
            // Le joueur a fermé la GUI sans cliquer confirmer/annuler (touche Echap, etc.) :
            // on considère cela comme une annulation.
            if (!confirmationHolder.isResolved()) {
                confirmationHolder.setResolved(true);
                Runnable onCancel = confirmationHolder.getOnCancel();
                if (onCancel != null) {
                    Bukkit.getScheduler().runTask(plugin, onCancel);
                }
            }
        }
    }
}
