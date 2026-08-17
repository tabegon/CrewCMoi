package fr.crewcmoi.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.database.BountyEntry;
import fr.crewcmoi.managers.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import fr.crewcmoi.utils.MoneyFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Construit et rafraîchit la GUI /bounty review (admins uniquement) : liste des
 * raisons de primes en attente de validation. Clic gauche = approuver (la prime
 * devient réclamable normalement, sans malus pour celui qui tue la cible), clic
 * droit = refuser (la contribution est supprimée et remboursée à son auteur).
 */
public class BountyReviewGuiManager {

    private final Main plugin;
    private final BountyManager bountyManager;

    public BountyReviewGuiManager(Main plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
    }

    public void open(Player viewer, int page) {
        BountyReviewHolder holder = new BountyReviewHolder();
        Inventory gui = Bukkit.createInventory(holder, BountyReviewHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&4&lʀᴇᴠɪꜱɪᴏɴ ᴅᴇꜱ ᴘʀɪᴍᴇꜱ"));
        holder.setInventory(gui);
        holder.setPage(page);

        render(holder);
        viewer.openInventory(gui);
    }

    public void render(BountyReviewHolder holder) {
        Inventory gui = holder.getInventory();
        holder.clearMapping();

        for (int i = 0; i < BountyReviewHolder.SIZE; i++) {
            gui.setItem(i, null);
        }

        List<BountyEntry> entries = bountyManager.getPendingReasonedBounties();
        int page = Math.max(0, holder.getPage());
        int maxPage = Math.max(0, (entries.size() - 1) / BountyReviewHolder.ITEMS_PER_PAGE);
        if (page > maxPage) {
            page = maxPage;
            holder.setPage(page);
        }

        int start = page * BountyReviewHolder.ITEMS_PER_PAGE;
        int end = Math.min(start + BountyReviewHolder.ITEMS_PER_PAGE, entries.size());

        String currency = plugin.getConfig().getString("economy.currency-symbol", "§f");

        for (int i = start; i < end; i++) {
            BountyEntry entry = entries.get(i);
            int slot = i - start;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta rawMeta = head.getItemMeta();
            if (rawMeta instanceof SkullMeta skullMeta) {
                OfflinePlayer target = Bukkit.getOfflinePlayer(entry.getTargetUuid());
                skullMeta.setOwningPlayer(target);
                skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&l" + entry.getTargetName()));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴍᴏɴᴛᴀɴᴛ : &a" + MoneyFormat.format(entry.getAmount()) + currency));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘᴀʀ : &e" + entry.getContributorName()));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ʀᴀɪꜱᴏɴ : &f" + entry.getReason()));
                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&aᴄʟɪᴄ ɢᴀᴜᴄʜᴇ &7: ᴀᴘᴘʀᴏᴜᴠᴇʀ"));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&cᴄʟɪᴄ ᴅʀᴏɪᴛ &7: ʀᴇꜰᴜꜱᴇʀ (ʀᴇᴍʙᴏᴜʀꜱᴇ)"));

                skullMeta.setLore(lore);
                head.setItemMeta(skullMeta);
            }

            gui.setItem(slot, head);
            holder.mapSlot(slot, entry.getId());
        }

        if (entries.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7ᴀᴜᴄᴜɴᴇ ʀᴀɪꜱᴏɴ ᴇɴ ᴀᴛᴛᴇɴᴛᴇ."));
                empty.setItemMeta(meta);
            }
            gui.setItem(22, empty);
        }

        if (page > 0) {
            gui.setItem(BountyReviewHolder.PREV_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ᴘʀᴇᴄᴇᴅᴇɴᴛᴇ"));
        }
        if (end < entries.size()) {
            gui.setItem(BountyReviewHolder.NEXT_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ꜱᴜɪᴠᴀɴᴛᴇ"));
        }
        gui.setItem(BountyReviewHolder.INFO_SLOT, createInfoItem(page + 1, maxPage + 1, entries.size()));
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
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4&lʀᴇᴠɪꜱɪᴏɴ ᴅᴇꜱ ᴘʀɪᴍᴇꜱ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘᴀɢᴇ &e" + currentPage + "&7/&e" + totalPages));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ʀᴀɪꜱᴏɴꜱ ᴇɴ ᴀᴛᴛᴇɴᴛᴇ : &e" + totalEntries));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
