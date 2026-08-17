package fr.crewcmoi.listeners;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.ClaimFlag;
import fr.crewcmoi.database.ClaimPermission;
import fr.crewcmoi.gui.AuctionGuiManager;
import fr.crewcmoi.gui.AuctionHolder;
import fr.crewcmoi.gui.BountyGuiManager;
import fr.crewcmoi.gui.BountyHolder;
import fr.crewcmoi.gui.BountyReviewGuiManager;
import fr.crewcmoi.gui.BountyReviewHolder;
import fr.crewcmoi.gui.ClaimAuctionGuiManager;
import fr.crewcmoi.gui.ClaimAuctionHolder;
import fr.crewcmoi.gui.ClaimSettingsGuiManager;
import fr.crewcmoi.gui.ClaimSettingsHolder;
import fr.crewcmoi.gui.ClaimShopGuiManager;
import fr.crewcmoi.gui.ClaimShopHolder;
import fr.crewcmoi.gui.ConfirmationHolder;
import fr.crewcmoi.gui.SellGuiManager;
import fr.crewcmoi.gui.SellHolder;
import fr.crewcmoi.managers.AuctionManager;
import fr.crewcmoi.managers.BountyManager;
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
    private final BountyReviewGuiManager bountyReviewGuiManager;
    private final BountyManager bountyManager;
    private final ClaimManager claimManager;
    private final ClaimSettingsGuiManager claimSettingsGuiManager;
    private final ClaimShopGuiManager claimShopGuiManager;
    private final ClaimAuctionGuiManager claimAuctionGuiManager;

    public GuiListener(Main plugin, SellGuiManager sellGuiManager, AuctionManager auctionManager,
                        AuctionGuiManager auctionGuiManager, BountyGuiManager bountyGuiManager,
                        ClaimManager claimManager, ClaimSettingsGuiManager claimSettingsGuiManager,
                        ClaimShopGuiManager claimShopGuiManager, BountyReviewGuiManager bountyReviewGuiManager,
                        BountyManager bountyManager, ClaimAuctionGuiManager claimAuctionGuiManager) {
        this.plugin = plugin;
        this.sellGuiManager = sellGuiManager;
        this.auctionManager = auctionManager;
        this.auctionGuiManager = auctionGuiManager;
        this.bountyGuiManager = bountyGuiManager;
        this.claimManager = claimManager;
        this.claimSettingsGuiManager = claimSettingsGuiManager;
        this.claimShopGuiManager = claimShopGuiManager;
        this.bountyReviewGuiManager = bountyReviewGuiManager;
        this.bountyManager = bountyManager;
        this.claimAuctionGuiManager = claimAuctionGuiManager;
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
        } else if (holder instanceof ClaimShopHolder claimShopHolder) {
            handleClaimShopClick(event, claimShopHolder);
        } else if (holder instanceof BountyReviewHolder bountyReviewHolder) {
            handleBountyReviewClick(event, bountyReviewHolder);
        } else if (holder instanceof ClaimAuctionHolder claimAuctionHolder) {
            handleClaimAuctionClick(event, claimAuctionHolder);
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

    private void handleClaimShopClick(InventoryClickEvent event, ClaimShopHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() != ClaimShopHolder.BUY_SLOT) {
            return;
        }

        fr.crewcmoi.managers.ClaimManager.ShopResult result = claimManager.buyExtraClaim(player);
        String prefix = plugin.getMessages().getString("prefix", "");
        switch (result) {
            case SUCCESS:
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix +
                        plugin.getMessages().getString("claim.shop-success", "&aVous avez acheté un claim supplémentaire !")));
                break;
            case NOT_ENOUGH_MONEY:
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix +
                        plugin.getMessages().getString("claim.shop-not-enough-money", "&cVous n'avez pas assez d'argent.")));
                break;
            case LIMIT_REACHED:
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix +
                        plugin.getMessages().getString("claim.shop-limit-reached", "&cVous avez atteint la limite de claims achetables.")));
                break;
        }
        claimShopGuiManager.render(player, holder);
    }

    private void handleBountyReviewClick(InventoryClickEvent event, BountyReviewHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == BountyReviewHolder.PREV_PAGE_SLOT) {
            bountyReviewGuiManager.open(player, holder.getPage() - 1);
            return;
        }
        if (slot == BountyReviewHolder.NEXT_PAGE_SLOT) {
            bountyReviewGuiManager.open(player, holder.getPage() + 1);
            return;
        }

        Integer entryId = holder.getEntryId(slot);
        if (entryId == null) {
            return;
        }

        String prefix = plugin.getMessages().getString("prefix", "");
        if (event.isLeftClick()) {
            bountyManager.approveBounty(entryId, success -> {
                if (success) {
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix
                            + "&aʀᴀɪꜱᴏɴ ᴀᴘᴘʀᴏᴜᴠᴇᴇ, ʟᴀ ᴘʀɪᴍᴇ ᴇꜱᴛ ᴍᴀɪɴᴛᴇɴᴀɴᴛ ᴠᴀʟɪᴅᴇ."));
                }
                bountyReviewGuiManager.render(holder);
            });
        } else if (event.isRightClick()) {
            bountyManager.denyBounty(entryId, success -> {
                if (success) {
                    player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix
                            + "&cʀᴀɪꜱᴏɴ ʀᴇꜰᴜꜱᴇᴇ, ʟᴇ ᴄᴏɴᴛʀɪʙᴜᴛᴇᴜʀ ᴀ ᴇᴛᴇ ʀᴇᴍʙᴏᴜʀꜱᴇ."));
                }
                bountyReviewGuiManager.render(holder);
            });
        }
    }

    private void handleClaimAuctionClick(InventoryClickEvent event, ClaimAuctionHolder holder) {
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        int slot = event.getRawSlot();
        if (slot == ClaimAuctionHolder.PREV_PAGE_SLOT) {
            claimAuctionGuiManager.open(player, holder.getPage() - 1);
            return;
        }
        if (slot == ClaimAuctionHolder.NEXT_PAGE_SLOT) {
            claimAuctionGuiManager.open(player, holder.getPage() + 1);
            return;
        }

        String chunkKey = holder.getChunkKey(slot);
        if (chunkKey == null || !event.isLeftClick()) {
            return;
        }
        String[] parts = chunkKey.split(";", 3);
        if (parts.length != 3) {
            return;
        }
        String world = parts[0];
        int chunkX;
        int chunkZ;
        try {
            chunkX = Integer.parseInt(parts[1]);
            chunkZ = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            return;
        }

        String prefix = plugin.getMessages().getString("prefix", "");
        ClaimManager.BuyResult result = claimManager.buyClaimAt(player, world, chunkX, chunkZ);
        switch (result) {
            case SUCCESS -> player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix
                    + plugin.getMessages().getString("claim.buy-success", "&aVous avez acheté ce claim.")));
            case NOT_FOR_SALE -> player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix
                    + plugin.getMessages().getString("claim.not-for-sale", "&cCe claim n'est pas en vente.")));
            case OWN_CLAIM -> player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix
                    + plugin.getMessages().getString("claim.own-claim", "&cVous êtes déjà le propriétaire de ce claim.")));
            case NOT_ENOUGH_MONEY -> player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix
                    + plugin.getMessages().getString("claim.not-enough-money", "&cVous n'avez pas assez d'argent pour acheter ce claim.")));
            case LIMIT_REACHED -> {
                String def = "&cVous avez atteint votre nombre maximum de claims (&e{max}&c).";
                String message = plugin.getMessages().getString("claim.limit-reached-buy", def)
                        .replace("{max}", String.valueOf(claimManager.getMaxClaims(player)));
                player.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', prefix + message));
            }
            case NOT_CLAIMED -> {
                // Le claim a été retiré/unclaim entre-temps (ex: annulé par le vendeur) : on
                // se contente de rafraîchir la GUI, il n'apparaîtra simplement plus.
            }
        }

        claimAuctionGuiManager.render(holder);
    }
}
