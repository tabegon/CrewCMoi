package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.ClaimFlag;
import fr.crewcmoi.database.ClaimPermission;
import fr.crewcmoi.gui.AuctionGuiManager;
import fr.crewcmoi.gui.AuctionHolder;
import fr.crewcmoi.gui.BountyGuiManager;
import fr.crewcmoi.gui.BountyHolder;
import fr.crewcmoi.gui.ClaimSettingsGuiManager;
import fr.crewcmoi.gui.ClaimSettingsHolder;
import fr.crewcmoi.gui.ConfirmationHolder;
import fr.crewcmoi.gui.SellGuiManager;
import fr.crewcmoi.gui.SellHolder;
import fr.crewcmoi.managers.AuctionManager;
import fr.crewcmoi.managers.ClaimManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

/**
 * Gère les clics/fermetures des GUIs custom du plugin (vente, hôtel des
 * ventes, confirmation générique, primes).
 */
public class GuiListener implements Listener {

    private final Main plugin;
    private final SellGuiManager sellGuiManager;
    private final AuctionManager auctionManager;
    private final AuctionGuiManager auctionGuiManager;
    private final BountyGuiManager bountyGuiManager;
    private final ClaimManager claimManager;
    private final ClaimSettingsGuiManager claimSettingsGuiManager;

    public GuiListener(Main plugin, SellGuiManager sellGuiManager, AuctionManager auctionManager,
                        AuctionGuiManager auctionGuiManager, BountyGuiManager bountyGuiManager,
                        ClaimManager claimManager, ClaimSettingsGuiManager claimSettingsGuiManager) {
        this.plugin = plugin;
        this.sellGuiManager = sellGuiManager;
        this.auctionManager = auctionManager;
        this.auctionGuiManager = auctionGuiManager;
        this.bountyGuiManager = bountyGuiManager;
        this.claimManager = claimManager;
        this.claimSettingsGuiManager = claimSettingsGuiManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof SellHolder sellHolder) {
            handleSellClick(event, sellHolder);
        } else if (holder instanceof AuctionHolder auctionHolder) {
            handleAuctionClick(event, auctionHolder);
        } else if (holder instanceof ConfirmationHolder confirmationHolder) {
            handleConfirmationClick(event, confirmationHolder);
        } else if (holder instanceof BountyHolder bountyHolder) {
            handleBountyClick(event, bountyHolder);
        } else if (holder instanceof ClaimSettingsHolder claimSettingsHolder) {
            handleClaimSettingsClick(event, claimSettingsHolder);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(event.getPlayer() instanceof Player player)) {
            return;
        }

        if (holder instanceof SellHolder sellHolder) {
            sellGuiManager.returnItems(player, event.getInventory(), sellHolder);
        } else if (holder instanceof ConfirmationHolder confirmationHolder && !confirmationHolder.isResolved()) {
            confirmationHolder.setResolved(true);
            if (confirmationHolder.getOnCancel() != null) {
                confirmationHolder.getOnCancel().run();
            }
        }
    }

    private void handleSellClick(InventoryClickEvent event, SellHolder holder) {
        int slot = event.getRawSlot();
        Inventory gui = event.getInventory();

        if (slot < 0 || slot >= gui.getSize()) {
            // Clic dans l'inventaire du joueur (pour y prendre/ajouter des objets) :
            // autorisé, sauf pendant l'étape de confirmation où tout est verrouillé.
            if (holder.isConfirming()) {
                event.setCancelled(true);
            }
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (holder.isConfirming()) {
            event.setCancelled(true);
            if (slot == SellHolder.CONFIRM_SLOT) {
                sellGuiManager.confirmSale(player, gui, holder);
            } else if (slot == SellHolder.CANCEL_CONFIRM_SLOT) {
                sellGuiManager.cancelConfirmation(gui, holder);
            }
            return;
        }

        if (slot == SellHolder.SELL_SLOT) {
            event.setCancelled(true);
            sellGuiManager.askConfirmation(player, gui, holder);
        } else if (!SellHolder.isItemSlot(slot)) {
            event.setCancelled(true);
        } else {
            // Dépôt/retrait d'un objet à vendre : autorisé, on recalcule le bouton
            // au tick suivant (l'objet n'a pas encore bougé au moment de cet event).
            plugin.getServer().getScheduler().runTask(plugin, () -> sellGuiManager.refreshConfirmButton(gui));
        }
    }

    private void handleAuctionClick(InventoryClickEvent event, AuctionHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (slot == AuctionHolder.PREV_PAGE_SLOT) {
            auctionGuiManager.open(player, holder.getPage() - 1);
            return;
        }
        if (slot == AuctionHolder.NEXT_PAGE_SLOT) {
            auctionGuiManager.open(player, holder.getPage() + 1);
            return;
        }

        Integer auctionId = holder.getAuctionId(slot);
        if (auctionId == null) {
            return;
        }

        boolean ownItem = auctionManager.getCachedAuctions().stream()
                .filter(a -> a.getId() == auctionId)
                .findFirst()
                .map(a -> a.getSellerUuid().equals(player.getUniqueId()))
                .orElse(false);

        if (ownItem) {
            auctionManager.cancel(player, auctionId, success -> auctionGuiManager.open(player, holder.getPage()));
        } else {
            auctionGuiManager.openBuyConfirmation(player, holder.getPage(), auctionId);
        }
    }

    private void handleConfirmationClick(InventoryClickEvent event, ConfirmationHolder holder) {
        event.setCancelled(true);
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        if (slot == ConfirmationHolder.CONFIRM_SLOT) {
            holder.setResolved(true);
            if (holder.getOnConfirm() != null) {
                holder.getOnConfirm().run();
            }
            event.getWhoClicked().closeInventory();
        } else if (slot == ConfirmationHolder.CANCEL_SLOT) {
            holder.setResolved(true);
            if (holder.getOnCancel() != null) {
                holder.getOnCancel().run();
            }
            event.getWhoClicked().closeInventory();
        }
    }

    private void handleBountyClick(InventoryClickEvent event, BountyHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == BountyHolder.PREV_PAGE_SLOT) {
            bountyGuiManager.open(player, holder.getPage() - 1);
        } else if (slot == BountyHolder.NEXT_PAGE_SLOT) {
            bountyGuiManager.open(player, holder.getPage() + 1);
        }
    }

    private void handleClaimSettingsClick(InventoryClickEvent event, ClaimSettingsHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= event.getInventory().getSize()) {
            return;
        }

        ClaimFlag flag = holder.getFlag(slot);
        if (flag == null) {
            return;
        }

        var claim = claimManager.getClaim(holder.getWorld(), holder.getChunkX(), holder.getChunkZ());
        if (claim == null) {
            player.closeInventory();
            return;
        }

        ClaimPermission next = claim.getPermission(flag).next();
        claimManager.setFlag(player, holder.getWorld(), holder.getChunkX(), holder.getChunkZ(), flag, next);
        claimSettingsGuiManager.render(holder);
    }
}
