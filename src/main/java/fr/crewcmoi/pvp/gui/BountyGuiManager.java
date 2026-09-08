package fr.crewcmoi.pvp.gui;

import fr.crewcmoi.Main;
import fr.crewcmoi.pvp.database.BountyEntry;
import fr.crewcmoi.pvp.database.BountyTarget;
import fr.crewcmoi.pvp.managers.BountyManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import fr.crewcmoi.other.utils.MoneyFormat;
import fr.crewcmoi.other.utils.GuiItems;
import java.util.ArrayList;
import java.util.List;

public class BountyGuiManager {

    private final Main plugin;
    private final BountyManager bountyManager;

    public BountyGuiManager(Main plugin, BountyManager bountyManager) {
        this.plugin = plugin;
        this.bountyManager = bountyManager;
    }

    public void open(Player viewer, int page) {
        BountyHolder holder = new BountyHolder();
        Inventory gui = Bukkit.createInventory(holder, BountyHolder.SIZE,
                ChatColor.translateAlternateColorCodes('&', "&4&lᴘʀɪᴍᴇꜱ ᴅᴇꜱ ᴊᴏᴜᴇᴜʀꜱ"));
        holder.setInventory(gui);
        holder.setPage(page);

        render(viewer, holder);
        viewer.openInventory(gui);
    }

    public void render(Player viewer, BountyHolder holder) {
        Inventory gui = holder.getInventory();
        holder.clearMapping();

        for (int i = 0; i < BountyHolder.SIZE; i++) {
            gui.setItem(i, null);
        }

        List<BountyTarget> targets = bountyManager.getBountyTargets();
        int page = Math.max(0, holder.getPage());
        int maxPage = Math.max(0, (targets.size() - 1) / BountyHolder.ITEMS_PER_PAGE);
        if (page > maxPage) {
            page = maxPage;
            holder.setPage(page);
        }

        int start = page * BountyHolder.ITEMS_PER_PAGE;
        int end = Math.min(start + BountyHolder.ITEMS_PER_PAGE, targets.size());

        String currency = plugin.getConfig().getString("economy.currency-symbol");

        for (int i = start; i < end; i++) {
            BountyTarget target = targets.get(i);
            int slot = i - start;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta rawMeta = head.getItemMeta();
            if (rawMeta instanceof SkullMeta skullMeta) {
                OfflinePlayer owner = Bukkit.getOfflinePlayer(target.getTargetUuid());
                skullMeta.setOwningPlayer(owner);
                skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&c&l" + target.getTargetName()));

                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘʀɪᴍᴇ ᴛᴏᴛᴀʟᴇ : &a" + MoneyFormat.format(target.getTotalAmount()) + currency));
                lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴄᴏɴᴛʀɪʙᴜᴛᴇᴜʀ(ꜱ) : &e" + target.getContributorCount()));
                lore.add("");

                List<BountyEntry> entries = bountyManager.getBounties(target.getTargetUuid());
                int shown = 0;
                for (BountyEntry entry : entries) {
                    if (shown >= 5) {
                        lore.add(ChatColor.translateAlternateColorCodes('&', "&8... ᴇᴛ " + (entries.size() - shown) + " de plus"));
                        break;
                    }
                    String reasonSuffix = entry.hasReason() ? " &8(&7" + entry.getReason() + "&8)" : "";
                    lore.add(ChatColor.translateAlternateColorCodes('&',
                            "&8- &7" + entry.getContributorName() + " &8: &a" + MoneyFormat.format(entry.getAmount()) + currency + reasonSuffix));
                    shown++;
                }

                lore.add("");
                lore.add(ChatColor.translateAlternateColorCodes('&', "&cᴛᴜᴇ ᴄᴇ ᴊᴏᴜᴇᴜʀ ᴘᴏᴜʀ ʀᴇᴄᴜᴘᴇʀᴇʀ ʟᴀ ᴘʀɪᴍᴇ !"));

                skullMeta.setLore(lore);
                head.setItemMeta(skullMeta);
            }

            gui.setItem(slot, head);
            holder.mapSlot(slot, target.getTargetUuid());
        }

        if (targets.isEmpty()) {
            ItemStack empty = new ItemStack(Material.BARRIER);
            ItemMeta meta = empty.getItemMeta();
            if (meta != null) {
                meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&7ᴀᴜᴄᴜɴᴇ ᴘʀɪᴍᴇ ᴀᴄᴛɪᴠᴇ ᴘᴏᴜʀ ʟᴇ ᴍᴏᴍᴇɴᴛ."));
                empty.setItemMeta(meta);
            }
            gui.setItem(22, empty);
        }

        if (page > 0) {
            gui.setItem(BountyHolder.PREV_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ᴘʀᴇᴄᴇᴅᴇɴᴛᴇ"));
        }
        if (end < targets.size()) {
            gui.setItem(BountyHolder.NEXT_PAGE_SLOT, createNavItem(Material.ARROW, "&eᴘᴀɢᴇ ꜱᴜɪᴠᴀɴᴛᴇ"));
        }
        gui.setItem(BountyHolder.INFO_SLOT, createInfoItem(page + 1, maxPage + 1, targets.size()));

        
        
        ItemStack filler = GuiItems.nothing(" ");
        for (int slot = 45; slot < BountyHolder.SIZE; slot++) {
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

    private ItemStack createInfoItem(int currentPage, int totalPages, int totalTargets) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4&lᴘʀɪᴍᴇꜱ ᴅᴇꜱ ᴊᴏᴜᴇᴜʀꜱ"));
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘᴀɢᴇ &e" + currentPage + "&7/&e" + totalPages));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴊᴏᴜᴇᴜʀꜱ ʀᴇᴄʜᴇʀᴄʜᴇꜱ : &e" + totalTargets));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴜᴛɪʟɪꜱᴇᴢ &f/ʙᴏᴜɴᴛʏ ᴀᴅᴅ <ᴊᴏᴜᴇᴜʀ> <ᴍᴏɴᴛᴀɴᴛ>"));
            lore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴘᴏᴜʀ ᴘʟᴀᴄᴇʀ ᴜɴᴇ ᴘʀɪᴍᴇ."));
            meta.setLore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}
