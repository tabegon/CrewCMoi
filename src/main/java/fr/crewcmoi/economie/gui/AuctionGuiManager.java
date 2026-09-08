package fr.crewcmoi.economie.gui;

import fr.crewcmoi.other.utils.Messages;

import fr.crewcmoi.Main;
import fr.crewcmoi.economie.auction.AuctionItem;
import fr.crewcmoi.economie.managers.AuctionManager;
import fr.crewcmoi.other.gui.ConfirmationGuiManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.other.utils.GuiItems;
import java.util.ArrayList;
import java.util.List;

public class AuctionGuiManager {

    private final Main plugin;
    private final AuctionManager auctionManager;
    private final ConfirmationGuiManager confirmationGuiManager = new ConfirmationGuiManager();

    public AuctionGuiManager(Main plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    public void openBuyConfirmation(Player player, int page, int auctionId) {
        AuctionItem auction = auctionManager.getCachedAuctions().stream()
                .filter(a -> a.getId() == auctionId)
                .findFirst()
                .orElse(null);

        if (auction == null) {
            Messages.send(player, "economy.auction.not-available", java.util.Map.of(), false);
            open(player, page);
            return;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol");

        List<String> extraLore = new ArrayList<>();
        extraLore.add("");
        extraLore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴠᴇɴᴅᴇᴜʀ : &e" + auction.getSellerName()));
        extraLore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘʀɪx : &a" + MoneyFormat.format(auction.getPrice()) + currency));
        extraLore.add("");
        extraLore.add(ChatColor.translateAlternateColorCodes('&', "&eᴄᴏɴꜰɪʀᴍᴇᴢ-ᴠᴏᴜꜱ ᴄᴇᴛ ᴀᴄʜᴀᴛ ?"));

        confirmationGuiManager.open(
                player,
                "&5&lᴄᴏɴꜰɪʀᴍᴇʀ ʟ'ᴀᴄʜᴀᴛ",
                auction.getItem(),
                extraLore,
                () -> auctionManager.buy(player, auctionId, result -> {
                    switch (result) {
                        case SUCCESS -> Messages.send(player, "economy.auction.buy-success", java.util.Map.of(), false);
                        case NOT_ENOUGH_MONEY -> Messages.send(player, "economy.auction.buy-not-enough-money", java.util.Map.of(), false);
                        case INVENTORY_FULL -> Messages.send(player, "economy.auction.buy-inventory-full", java.util.Map.of(), false);
                        case OWN_ITEM -> Messages.send(player, "economy.auction.buy-own-item", java.util.Map.of(), false);
                        case NOT_FOUND -> Messages.send(player, "economy.auction.not-available", java.util.Map.of(), false);
                    }
                    open(player, page);
                }),
                () -> open(player, page)
        );
    }

    public void open(Player player, int page) {
        openInternal(player, page, false);
    }

    public void openMyListings(Player player, int page) {
        openInternal(player, page, true);
    }

    private void openInternal(Player player, int page, boolean myListings) {
        auctionManager.refreshCache(() -> {
            AuctionHolder holder = new AuctionHolder();
            String title = myListings ? "&5&lᴍᴇꜱ ᴀɴɴᴏɴᴄᴇꜱ" : "&5&lʜᴏᴛᴇʟ ᴅᴇꜱ ᴠᴇɴᴛᴇꜱ";
            Inventory gui = Bukkit.createInventory(holder, AuctionHolder.SIZE,
                    ChatColor.translateAlternateColorCodes('&', title));
            holder.setInventory(gui);
            holder.setPage(page);
            holder.setMyListings(myListings);

            render(player, holder);
            player.openInventory(gui);
        });
    }

    public void render(Player viewer, AuctionHolder holder) {
        Inventory gui = holder.getInventory();
        holder.clearMapping();

        for (int i = 0; i < AuctionHolder.SIZE; i++) {
            gui.setItem(i, null);
        }

        List<AuctionItem> auctions = auctionManager.getCachedAuctions();
        if (holder.isMyListings()) {
            List<AuctionItem> mine = new ArrayList<>();
            for (AuctionItem auction : auctions) {
                if (auction.getSellerUuid().equals(viewer.getUniqueId())) {
                    mine.add(auction);
                }
            }
            auctions = mine;
        }

        int page = Math.max(0, holder.getPage());
        int maxPage = Math.max(0, (auctions.size() - 1) / AuctionHolder.ITEMS_PER_PAGE);
        if (page > maxPage) {
            page = maxPage;
            holder.setPage(page);
        }

        int start = page * AuctionHolder.ITEMS_PER_PAGE;
        int end = Math.min(start + AuctionHolder.ITEMS_PER_PAGE, auctions.size());

        String currency = plugin.getConfig().getString("economy.currency-symbol");

        for (int i = start; i < end; i++) {
            AuctionItem auction = auctions.get(i);
            int slot = i - start;

            ItemStack display = auction.getItem().clone();
            ItemMeta meta = display.getItemMeta();
            if (meta != null) {
                List<String> lore = new ArrayList<>();
                if (meta.hasLore() && meta.getLore() != null) {
                    lore.addAll(meta.getLore());
                    lore.add("");
                }
                if (!holder.isMyListings()) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴠᴇɴᴅᴇᴜʀ : &e" + auction.getSellerName()));
                }
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘʀɪx : &a" + MoneyFormat.format(auction.getPrice()) + currency));

                if (holder.isMyListings() || auction.getSellerUuid().equals(viewer.getUniqueId())) {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&cᴄʟɪǫᴜᴇ ᴘᴏᴜʀ ʀᴇᴛɪʀᴇʀ ᴛᴏɴ ᴀɴɴᴏɴᴄᴇ"));
                } else {
                    lore.add(ChatColor.translateAlternateColorCodes('&', "&aᴄʟɪǫᴜᴇ ᴘᴏᴜʀ ᴀᴄʜᴇᴛᴇʀ"));
                }

                meta.setLore(lore);
                display.setItemMeta(meta);
            }

            gui.setItem(slot, display);
            holder.mapSlot(slot, auction.getId());
        }

        if (page > 0) {
            gui.setItem(AuctionHolder.PREV_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ᴘʀᴇᴄᴇᴅᴇɴᴛᴇ"));
        }
        if (end < auctions.size()) {
            gui.setItem(AuctionHolder.NEXT_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ꜱᴜɪᴠᴀɴᴛᴇ"));
        }
        gui.setItem(AuctionHolder.INFO_SLOT, createInfoItem(page + 1, maxPage + 1, auctions.size(), holder.isMyListings()));
        gui.setItem(AuctionHolder.MY_LISTINGS_SLOT,
                holder.isMyListings() ? createBackItem() : createMyListingsItem());

        
        
        ItemStack filler = GuiItems.nothing(" ");
        for (int slot = 45; slot < AuctionHolder.SIZE; slot++) {
            if (gui.getItem(slot) == null) {
                gui.setItem(slot, filler);
            }
        }
    }

    private ItemStack createNavItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createInfoItem(int currentPage, int totalPages, int totalAuctions, boolean myListings) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                    myListings ? "&d&lᴍᴇꜱ ᴀɴɴᴏɴᴄᴇꜱ" : "&d&lʜᴏᴛᴇʟ ᴅᴇꜱ ᴠᴇɴᴛᴇꜱ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘᴀɢᴇ &e" + currentPage + "&7/&e" + totalPages));
            if (myListings) {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴠᴏꜱ ᴀɴɴᴏɴᴄᴇꜱ ᴇɴ ᴄᴏᴜʀꜱ : &e" + totalAuctions));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴄʟɪǫᴜᴇᴢ ꜱᴜʀ ᴜɴ ᴏʙᴊᴇᴛ ᴘᴏᴜʀ ʟᴇ ʀᴇᴛɪʀᴇʀ."));
            } else {
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴀɴɴᴏɴᴄᴇꜱ ᴀᴄᴛɪᴠᴇꜱ : &e" + totalAuctions));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴜᴛɪʟɪꜱᴇᴢ &f/ᴀʜ ꜱᴇʟʟ <ᴘʀɪx>&7 ᴘᴏᴜʀ ᴠᴇɴᴅʀᴇ"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ʟ'ᴏʙᴊᴇᴛ ǫᴜᴇ ᴠᴏᴜꜱ ᴛᴇɴᴇᴢ ᴇɴ ᴍᴀɪɴ."));
            }
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMyListingsItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e&lᴍᴇꜱ ᴀɴɴᴏɴᴄᴇꜱ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴄʟɪǫᴜᴇᴢ ᴘᴏᴜʀ ᴠᴏɪʀ ʟᴇꜱ ᴏʙᴊᴇᴛꜱ"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ǫᴜᴇ ᴠᴏᴜꜱ ᴀᴠᴇᴢ ᴍɪꜱ ᴇɴ ᴠᴇɴᴛᴇ."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&l« ʀᴇᴛᴏᴜʀ »"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ʀᴇᴛᴏᴜʀ à ʟ'ʜᴏᴛᴇʟ ᴅᴇꜱ ᴠᴇɴᴛᴇꜱ."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
