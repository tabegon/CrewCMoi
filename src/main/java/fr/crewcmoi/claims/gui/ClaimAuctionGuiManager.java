package fr.crewcmoi.claims.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.claims.database.ClaimData;
import fr.crewcmoi.claims.managers.ClaimManager;
import fr.crewcmoi.utils.MoneyFormat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit et rafraîchit la GUI /claim ah (hôtel des ventes des claims, dans le style
 * d'un hôtel des ventes d'objets) : liste tous les claims actuellement mis en vente par
 * leurs propriétaires (toutes coordonnées confondues), avec le monde, les coordonnées
 * du chunk et le prix. Un clic achète directement le claim, sans avoir besoin de s'y
 * rendre au préalable.
 */
public class ClaimAuctionGuiManager {

    private final Main plugin;
    private final ClaimManager claimManager;

    public ClaimAuctionGuiManager(Main plugin, ClaimManager claimManager) {
        this.plugin = plugin;
        this.claimManager = claimManager;
    }

    public void open(Player viewer, int page) {
        ClaimAuctionHolder holder = new ClaimAuctionHolder();
        Inventory gui = Bukkit.createInventory(holder, ClaimAuctionHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&5&lHôtel des ventes des claims"));
        holder.setInventory(gui);
        holder.setPage(page);

        render(holder);
        viewer.openInventory(gui);
    }

    public void render(ClaimAuctionHolder holder) {
        Inventory gui = holder.getInventory();
        holder.clearMapping();

        for (int i = 0; i < ClaimAuctionHolder.SIZE; i++) {
            gui.setItem(i, null);
        }

        List<ClaimData> forSale = claimManager.getClaimsForSale();
        int page = Math.max(0, holder.getPage());
        int maxPage = Math.max(0, (forSale.size() - 1) / ClaimAuctionHolder.ITEMS_PER_PAGE);
        if (page > maxPage) {
            page = maxPage;
            holder.setPage(page);
        }

        int start = page * ClaimAuctionHolder.ITEMS_PER_PAGE;
        int end = Math.min(start + ClaimAuctionHolder.ITEMS_PER_PAGE, forSale.size());

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        for (int i = start; i < end; i++) {
            ClaimData claim = forSale.get(i);
            int slot = i - start;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta rawMeta = head.getItemMeta();
            if (rawMeta instanceof SkullMeta skullMeta) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(claim.getOwnerUuid());
                skullMeta.setOwningPlayer(owner);
                skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&',
                        "&a&l" + MoneyFormat.format(claim.getSalePrice()) + currency));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘʀᴏᴘʀɪᴇᴛᴀɪʀᴇ : &e" + claim.getOwnerName()));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴍᴏɴᴅᴇ : &f" + claim.getWorld()));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴄʜᴜɴᴋ : &f" + claim.getChunkX() + ", " + claim.getChunkZ()));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&aᴄʟɪᴄ ɢᴀᴜᴄʜᴇ &7: ᴀᴄʜᴇᴛᴇʀ"));

                skullMeta.setLore(lore);
                head.setItemMeta(skullMeta);
            }

            gui.setItem(slot, head);
            holder.mapSlot(slot, claim.getWorld(), claim.getChunkX(), claim.getChunkZ());
        }

        if (forSale.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7ᴀᴜᴄᴜɴ ᴄʟᴀɪᴍ ᴇɴ ᴠᴇɴᴛᴇ."));
                empty.setItemMeta(meta);
            }
            gui.setItem(22, empty);
        }

        if (page > 0) {
            gui.setItem(ClaimAuctionHolder.PREV_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ᴘʀᴇᴄᴇᴅᴇɴᴛᴇ"));
        }
        if (end < forSale.size()) {
            gui.setItem(ClaimAuctionHolder.NEXT_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ꜱᴜɪᴠᴀɴᴛᴇ"));
        }
        gui.setItem(ClaimAuctionHolder.INFO_SLOT, createInfoItem(page + 1, maxPage + 1, forSale.size()));
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

    private ItemStack createInfoItem(int currentPage, int totalPages, int totalEntries) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5&lʜᴏᴛᴇʟ ᴅᴇꜱ ᴠᴇɴᴛᴇꜱ ᴅᴇꜱ ᴄʟᴀɪᴍꜱ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘᴀɢᴇ &e" + currentPage + "&7/&e" + totalPages));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴄʟᴀɪᴍꜱ ᴇɴ ᴠᴇɴᴛᴇ : &e" + totalEntries));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
