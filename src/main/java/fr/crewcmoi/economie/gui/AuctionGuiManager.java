package fr.crewcmoi.economie.gui;

import dev.lone.itemsadder.api.CustomStack;
import fr.crewcmoi.Main;
import fr.crewcmoi.economie.auction.AuctionItem;
import fr.crewcmoi.economie.managers.AuctionManager;
import fr.crewcmoi.other.gui.ConfirmationGuiManager;
import fr.crewcmoi.other.utils.GuiItems;
import fr.crewcmoi.other.utils.Messages;
import fr.crewcmoi.other.utils.MoneyFormat;
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
import java.util.Map;

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
            Messages.send(player, "economy.auction.not-available", Map.of(), false);
            open(player, page);
            return;
        }

        String currency = plugin.getConfig().getString("economy.currency-symbol", "");
        List<String> extraLore = new ArrayList<>();
        if (auction.getItem().getItemMeta() != null && auction.getItem().getItemMeta().hasLore()) {
            List<String> lore = auction.getItem().getItemMeta().getLore();
            if (lore != null) extraLore.addAll(lore);
        }
        extraLore.add(ChatColor.translateAlternateColorCodes('&', "&7ᴠᴇɴᴅᴇᴜʀ : &e" + auction.getSellerName()));

        ItemStack sellerHead = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta skullMeta = (SkullMeta) sellerHead.getItemMeta();
        if (skullMeta != null) {
            OfflinePlayer seller = Bukkit.getOfflinePlayer(auction.getSellerUuid());
            skullMeta.setOwningPlayer(seller);
            skullMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&e" + auction.getSellerName()));
            sellerHead.setItemMeta(skullMeta);
        }

        ItemStack money = GuiItems.moneyButton(
                "&eᴍᴏɴᴇʏ : &a" + MoneyFormat.format(auction.getPrice()) + currency);

        confirmationGuiManager.open(
                player,
                "&8&lBUY GUI",
                auction.getItem(),
                extraLore,
                sellerHead,
                money,
                () -> auctionManager.buy(player, auctionId, result -> {
                    switch (result) {
                        case SUCCESS -> Messages.send(player, "economy.auction.buy-success", Map.of(), false);
                        case NOT_ENOUGH_MONEY -> Messages.send(player, "economy.auction.buy-not-enough-money", Map.of(), false);
                        case INVENTORY_FULL -> Messages.send(player, "economy.auction.buy-inventory-full", Map.of(), false);
                        case OWN_ITEM -> Messages.send(player, "economy.auction.buy-own-item", Map.of(), false);
                        case NOT_FOUND -> Messages.send(player, "economy.auction.not-available", Map.of(), false);
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
            holder.setMyListings(myListings);
            int size = myListings ? AuctionHolder.MY_LISTINGS_SIZE : AuctionHolder.MAIN_SIZE;
            String title = myListings ? "&8&lᴍᴇꜱ ᴀɴɴᴏɴᴄᴇꜱ" : "&8&lʜᴏᴛᴇʟ ᴅᴇꜱ ᴠᴇɴᴛᴇꜱ";
            Inventory gui = Bukkit.createInventory(holder, size,
                    ChatColor.translateAlternateColorCodes('&', title));
            holder.setInventory(gui);
            holder.setPage(Math.max(0, page));
            render(player, holder);
            player.openInventory(gui);
        });
    }

    public void render(Player viewer, AuctionHolder holder) {
        Inventory gui = holder.getInventory();
        holder.clearMapping();
        for (int i = 0; i < gui.getSize(); i++) gui.setItem(i, null);

        List<AuctionItem> auctions = auctionManager.getCachedAuctions();
        if (holder.isMyListings()) {
            List<AuctionItem> mine = new ArrayList<>();
            for (AuctionItem auction : auctions) {
                if (auction.getSellerUuid().equals(viewer.getUniqueId())) mine.add(auction);
            }
            auctions = mine;
        }

        int perPage = holder.isMyListings() ? AuctionHolder.MY_ITEMS_PER_PAGE : AuctionHolder.MAIN_ITEMS_PER_PAGE;
        int page = Math.max(0, holder.getPage());
        int maxPage = Math.max(0, (auctions.size() - 1) / perPage);
        if (page > maxPage) {
            page = maxPage;
            holder.setPage(page);
        }

        int start = page * perPage;
        int end = Math.min(start + perPage, auctions.size());
        String currency = plugin.getConfig().getString("economy.currency-symbol", "");

        for (int i = start; i < end; i++) {
            AuctionItem auction = auctions.get(i);
            int slot = AuctionHolder.itemSlot(holder.isMyListings(), i - start);
            if (slot < 0 || slot >= gui.getSize()) continue;

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
                lore.add(ChatColor.translateAlternateColorCodes('&', holder.isMyListings()
                        ? "&cᴄʟɪǫᴜᴇᴢ ᴘᴏᴜʀ ʀᴇᴛɪʀᴇʀ ʟ'ᴀɴɴᴏɴᴄᴇ"
                        : "&aᴄʟɪǫᴜᴇᴢ ᴘᴏᴜʀ ᴀᴄʜᴇᴛᴇʀ"));
                meta.setLore(lore);
                display.setItemMeta(meta);
            }

            gui.setItem(slot, display);
            holder.mapSlot(slot, auction.getId());
        }

        if (holder.isMyListings()) {
            setNothing(gui, new int[]{0, 8, 9, 17, 18, 19, 20, 21, 23, 24, 25, 26});
            gui.setItem(AuctionHolder.MY_BACK_SLOT, GuiItems.cancelButton("&c&lɢᴏ ʙᴀᴄᴋ"));
        } else {
            setNothing(gui, new int[]{8, 9, 17, 18, 26, 27, 35, 36, 44, 45, 53});
            gui.setItem(AuctionHolder.MAIN_BACK_SLOT, GuiItems.cancelButton("&c&lɢᴏ ʙᴀᴄᴋ"));
            gui.setItem(AuctionHolder.MY_LISTINGS_SLOT, GuiItems.chestButton("&e&lʏᴏᴜʀ ɪᴛᴇᴍꜱ"));
        }

        if (!holder.isMyListings()) {
            ItemStack filler = itemsAdderItem("server:fill_button", new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            gui.setItem(46, filler.clone());
            gui.setItem(48, filler.clone());
            gui.setItem(50, filler.clone());
            gui.setItem(52, filler.clone());
            gui.setItem(AuctionHolder.MY_LISTINGS_SLOT, GuiItems.chestButton("&e&lʏᴏᴜʀ ɪᴛᴇᴍꜱ"));
            gui.setItem(AuctionHolder.PREV_PAGE_SLOT, itemsAdderItem("server:button_up", GuiItems.arrowButton("&e&lᴘᴀɢᴇ ᴘʀᴇᴄᴇᴅᴇɴᴛᴇ")));
            gui.setItem(AuctionHolder.NEXT_PAGE_SLOT, itemsAdderItem("server:button_down", GuiItems.arrowButton("&e&lᴘᴀɢᴇ ꜱᴜɪᴠᴀɴᴛᴇ")));
        }
    }

    private void setNothing(Inventory gui, int[] slots) {
        for (int slot : slots) gui.setItem(slot, GuiItems.nothing(" "));
    }

    private ItemStack itemsAdderItem(String id, ItemStack fallback) {
        try {
            CustomStack custom = CustomStack.getInstance(id);
            if (custom != null && custom.getItemStack() != null) return custom.getItemStack().clone();
            plugin.getLogger().warning("AH : item ItemsAdder introuvable '" + id + "'. Vérifie son ID exact dans ItemsAdder.");
        } catch (Throwable error) {
            plugin.getLogger().warning("AH : erreur de chargement ItemsAdder '" + id + "' : " + error.getMessage());
        }
        return fallback.clone();
    }

}
