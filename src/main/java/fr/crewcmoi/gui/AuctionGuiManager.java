package fr.crewcmoi.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.auction.AuctionItem;
import fr.crewcmoi.managers.AuctionManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Construit et rafraîchit la GUI de l'hôtel des ventes, et gère les clics (achat, annulation, navigation).
 */
public class AuctionGuiManager {

    private final Main plugin;
    private final AuctionManager auctionManager;
    private final DecimalFormat format = new DecimalFormat("#,##0.00");

    public AuctionGuiManager(Main plugin, AuctionManager auctionManager) {
        this.plugin = plugin;
        this.auctionManager = auctionManager;
    }

    public void open(Player player, int page) {
        auctionManager.refreshCache(() -> {
            AuctionHolder holder = new AuctionHolder();
            Inventory gui = Bukkit.createInventory(holder, AuctionHolder.SIZE,
                    ChatColor.translateAlternateColorCodes('&', "&5&lʜᴏᴛᴇʟ ᴅᴇꜱ ᴠᴇɴᴛᴇꜱ"));
            holder.setInventory(gui);
            holder.setPage(page);

            render(player, holder);
            player.openInventory(gui);
        });
    }

    /**
     * Reconstruit le contenu de la GUI déjà ouverte (après achat/annulation/changement de page).
     */
    public void render(Player viewer, AuctionHolder holder) {
        Inventory gui = holder.getInventory();
        holder.clearMapping();

        for (int i = 0; i < AuctionHolder.SIZE; i++) {
            gui.setItem(i, null);
        }

        List<AuctionItem> auctions = auctionManager.getCachedAuctions();
        int page = Math.max(0, holder.getPage());
        int maxPage = Math.max(0, (auctions.size() - 1) / AuctionHolder.ITEMS_PER_PAGE);
        if (page > maxPage) {
            page = maxPage;
            holder.setPage(page);
        }

        int start = page * AuctionHolder.ITEMS_PER_PAGE;
        int end = Math.min(start + AuctionHolder.ITEMS_PER_PAGE, auctions.size());

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

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
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴠᴇɴᴅᴇᴜʀ : &e" + auction.getSellerName()));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘʀɪx : &a" + format.format(auction.getPrice()) + currency));

                if (auction.getSellerUuid().equals(viewer.getUniqueId())) {
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

        // Navigation
        if (page > 0) {
            gui.setItem(AuctionHolder.PREV_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ᴘʀᴇᴄᴇᴅᴇɴᴛᴇ"));
        }
        if (end < auctions.size()) {
            gui.setItem(AuctionHolder.NEXT_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ꜱᴜɪᴠᴀɴᴛᴇ"));
        }
        gui.setItem(AuctionHolder.INFO_SLOT, createInfoItem(page + 1, maxPage + 1, auctions.size()));
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

    private ItemStack createInfoItem(int currentPage, int totalPages, int totalAuctions) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&d&lʜᴏᴛᴇʟ ᴅᴇꜱ ᴠᴇɴᴛᴇꜱ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘᴀɢᴇ &e" + currentPage + "&7/&e" + totalPages));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴀɴɴᴏɴᴄᴇꜱ ᴀᴄᴛɪᴠᴇꜱ : &e" + totalAuctions));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴜᴛɪʟɪꜱᴇᴢ &f/ᴀʜ ꜱᴇʟʟ <ᴘʀɪx>&7 ᴘᴏᴜʀ ᴠᴇɴᴅʀᴇ"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ʟ'ᴏʙᴊᴇᴛ ǫᴜᴇ ᴠᴏᴜꜱ ᴛᴇɴᴇᴢ ᴇɴ ᴍᴀɪɴ."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
